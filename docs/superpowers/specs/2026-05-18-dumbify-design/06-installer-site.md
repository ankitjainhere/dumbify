# Installer Site

Static site hosted on GitHub Pages from `/site` directory of the `dumbify` repo. Custom domain `dumbify.app` optional. No backend; no analytics in v1.

## Stack

- Plain HTML + vanilla JS + minimal CSS (no React/Vue needed)
- `ya-webadb` (https://github.com/yume-chan/ya-webadb) — WebUSB ADB in browser
- APK fetched from latest GitHub Release via `https://api.github.com/repos/<owner>/dumbify/releases/latest`

## Pages

| Path | Purpose |
|---|---|
| `/` | landing — value prop, "Install now" CTA, screenshots, link to GitHub |
| `/install` | WebUSB install wizard (the main feature) |
| `/docs` | enable developer mode, USB debugging, account removal, supported phones |
| `/faq` | brick recovery (bootloader factory reset), what data we collect (none), license |
| `/about` | open source, GPLv3, contributors, link to repo |

## Install wizard steps

```
Step 1 — Browser check
  if (!('usb' in navigator)) → block, show "Use Chrome/Edge desktop"

Step 2 — Enable USB debugging
  Per-OEM guidance (Settings → About → tap Build Number 7×, then Settings → Developer Options → USB Debugging)

Step 3 — Connect & authorize
  await navigator.usb.requestDevice({ filters: ADB_FILTERS })
  user taps "Allow USB debugging" on phone

Step 4 — Pre-flight checks
  shell: dumpsys account → if accounts present, instruct removal, retry
  shell: getprop ro.build.version.sdk → require >= 29

Step 5 — Download APK
  fetch GitHub Releases API → latest .apk asset URL → fetch bytes (progress bar)

Step 6 — Install
  adb: sync APK to /data/local/tmp/dumbify.apk
  adb: pm install -r /data/local/tmp/dumbify.apk

Step 7 — Set Device Owner
  adb: dpm set-device-owner com.dumbify.app/.admin.DumbifyDeviceAdminReceiver
  if fails (accounts exist, already DO, etc.) → error guidance

Step 8 — Done
  "Disconnect USB. Open Dumbify app on your phone."
```

## Error handling

Common failures + remediation text:
- `device offline` → re-authorize USB debug
- `set-device-owner returned error` containing "accounts" → step back to Step 4
- `set-device-owner returned error` containing "device owner already set" → another DO app installed; cannot proceed without factory reset
- `INSTALL_FAILED_VERSION_DOWNGRADE` → uninstall existing Dumbify first

## Browser support

WebUSB available:
- Chrome desktop (Windows / macOS / Linux / ChromeOS)
- Edge desktop
- Opera desktop

WebUSB NOT available:
- Any iOS browser (Apple disables WebUSB)
- Firefox (any platform — declined to ship WebUSB)
- Any mobile browser

Site must detect and message clearly.

## CI

GitHub Actions:
- On push to `main` under `/site/**` → deploy to GitHub Pages
- On release tag → site auto-fetches new APK via Releases API (no rebuild needed)
