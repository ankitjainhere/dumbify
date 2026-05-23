/**
 * Thin wrapper around ya-webadb for the install wizard.
 * Loaded as an ES module from install/index.html.
 *
 * ya-webadb is loaded from CDN via importmap in install/index.html.
 * Spec: docs/superpowers/specs/2026-05-18-dumbify-design/06-installer-site.md
 */

import { credentialStore } from './credentialStore.js';

const OWNER_COMPONENT = 'com.dumbify.app/.admin.DumbifyDeviceAdminReceiver';
const GITHUB_API      = 'https://api.github.com/repos/ankitjainhere/dumbify/releases/latest';
const TMP_PATH        = '/data/local/tmp/dumbify.apk';

export class AdbSession {
  #device = null;
  #adb    = null;

  /** Open WebUSB device picker and perform ADB handshake. */
  async connect(onLog) {
    const { AdbDaemonTransport, Adb } =
      await import('@yume-chan/adb');
    const { AdbDaemonWebUsbDeviceManager, AdbDaemonWebUsbDevice } =
      await import('@yume-chan/adb-daemon-webusb');

    const manager = AdbDaemonWebUsbDeviceManager.BROWSER;
    if (!manager) throw new Error('WebUSB not available');

    onLog('Requesting USB device…', 'info');
    const device = await manager.requestDevice();
    if (!device) throw new Error('No device selected');

    onLog(`Device selected: ${device.name || device.productName || device.serial}`, 'info');
    onLog('Performing ADB handshake — tap "Allow USB debugging" on phone…', 'info');

    try {
      const transport = await AdbDaemonTransport.authenticate({
        serial: device.serial,
        connection: await device.connect(),
        credentialStore,
      });

      this.#adb = new Adb(transport);
    } catch (e) {
      if (e instanceof AdbDaemonWebUsbDevice.DeviceBusyError) {
        throw new Error(
          'Device is in use by another program. ' +
          'Close Android Studio, run: adb kill-server, then retry.',
        );
      }
      throw e;
    }

    onLog('ADB connected ✓', 'ok');
    return this;
  }

  /** Run a shell command and return trimmed stdout. */
  async shell(cmd) {
    const { exitCode, stdout, stderr } = await this.#spawnShell(cmd);
    if (exitCode !== 0) throw new Error(`Command failed (exit ${exitCode}): ${stderr || stdout}`);
    return stdout.trim();
  }

  async #spawnShell(cmd) {
    if (this.#adb.subprocess.shellProtocol?.spawnWaitText) {
      return this.#adb.subprocess.shellProtocol.spawnWaitText(cmd);
    }

    const process = await this.#adb.subprocess.shell(cmd);
    const [stdout, stderr, exitCode] = await Promise.all([
      readText(process.stdout),
      readText(process.stderr),
      process.exit,
    ]);

    return { exitCode, stdout, stderr };
  }

  /** Pre-flight: SDK version >= 29, no accounts. */
  async preflight(onLog) {
    const sdkText = await this.shell('getprop ro.build.version.sdk');
    const sdk = parseInt(sdkText, 10);
    onLog(`Android SDK: ${sdk}`, sdk >= 29 ? 'ok' : 'err');
    if (Number.isNaN(sdk)) {
      throw new Error(`Could not read Android SDK version from device. getprop returned: ${JSON.stringify(sdkText)}`);
    }
    if (sdk < 29) throw new Error('Android 10 (SDK 29) or newer required.');

    const accounts = await this.shell('dumpsys account');
    if (accounts.includes('Account {')) {
      onLog('Google/other accounts detected — must remove before installing', 'err');
      throw new Error('ACCOUNTS_PRESENT');
    }
    onLog('No accounts found ✓', 'ok');
  }

  /** Fetch latest APK from GitHub Releases, push to device, return remote path. */
  async downloadAndPush(onLog, onProgress) {
    onLog('Fetching latest release from GitHub…', 'info');
    const rel = await fetch(GITHUB_API).then(r => r.json());
    const asset = rel.assets?.find(a => a.name.endsWith('.apk'));
    if (!asset) throw new Error('No APK asset found in latest release. Has a release been published?');

    onLog(`Downloading ${asset.name} (${(asset.size / 1e6).toFixed(1)} MB)…`, 'info');
    const res = await fetch(asset.browser_download_url);
    const total = asset.size;
    const reader = res.body.getReader();
    const chunks = [];
    let received = 0;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      chunks.push(value);
      received += value.length;
      onProgress(received / total);
    }

    const apkBytes = new Uint8Array(chunks.reduce((acc, c) => acc + c.length, 0));
    let offset = 0;
    for (const chunk of chunks) { apkBytes.set(chunk, offset); offset += chunk.length; }

    onLog('Pushing APK to device…', 'info');
    const sync = await this.#adb.sync();
    try {
      await sync.write({
        filename: TMP_PATH,
        file: new ReadableStream({
          start(c) { c.enqueue(apkBytes); c.close(); },
        }),
      });
    } finally {
      await sync.dispose();
    }
    onLog('APK on device ✓', 'ok');
    return TMP_PATH;
  }

  /** pm install the APK. */
  async install(remotePath, onLog) {
    onLog(`Installing ${remotePath}…`, 'info');
    const out = await this.shell(`pm install -r ${remotePath}`);
    onLog(out, out.includes('Success') ? 'ok' : 'err');
    if (!out.includes('Success')) {
      if (out.includes('INSTALL_FAILED_VERSION_DOWNGRADE'))
        throw new Error('Version downgrade blocked. Uninstall existing Dumbify first.');
      throw new Error(`pm install failed: ${out}`);
    }
  }

  /** dpm set-device-owner. */
  async setDeviceOwner(onLog) {
    onLog('Setting Device Owner (requires no accounts, no existing DO)…', 'info');
    const out = await this.shell(`dpm set-device-owner ${OWNER_COMPONENT}`);
    onLog(out, out.toLowerCase().includes('success') ? 'ok' : 'err');
    if (!out.toLowerCase().includes('success')) {
      if (out.includes('accounts')) throw new Error('ACCOUNTS_PRESENT');
      if (out.includes('already set')) throw new Error('DO_ALREADY_SET');
      throw new Error(`set-device-owner failed: ${out}`);
    }
  }

  async cleanup(remotePath) {
    await this.shell(`rm -f ${remotePath}`).catch(() => {});
  }
}

export function isWebUsbSupported() {
  return window.isSecureContext && 'usb' in navigator;
}

async function readText(stream) {
  if (!stream) return '';

  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let result = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    result += decoder.decode(value, { stream: true });
  }

  result += decoder.decode();
  return result;
}
