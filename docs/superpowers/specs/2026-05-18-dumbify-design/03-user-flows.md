# User Flows

## A. First install (one-time, via WebUSB site)

```
1. User visits installer site in Chrome/Edge desktop (WebUSB required).
2. Site checks WebUSB support; refuses gracefully on unsupported browsers.
3. Site guides USB debugging enable (per-Android-version screenshots).
4. WebADB handshake → user taps Allow on phone.
5. Pre-flight: Android >= 10, no personal accounts present.
   - If accounts present, site instructs Settings → Accounts → Remove all,
     then retry.
6. Site downloads latest APK from GitHub Releases.
7. ADB: `pm install` → `dpm set-device-owner com.dumbify.app/.admin.DumbifyDeviceAdminReceiver`
8. Site confirms success; user disconnects USB; opens Dumbify app.
```

See `06-installer-site.md` for site implementation.

## B. In-app setup wizard

| Screen | Content |
|---|---|
| 1. Welcome + Role | Pick `SELF` or `MANAGED` |
| 2. Mode | Pick `ALLOWLIST` or `DENYLIST` |
| 3. PINs | Set **Removal PIN** (mandatory; should be set by/given to a trusted third party — do not memorize). Set **Bypass PIN** if any rule will use PIN bypass (decided post Screen 6). |
| 4. Motivation | Free-text custom message shown on block screen |
| 5. App picker | List of installed apps; user marks essentials (allowlist) or blocked (denylist). Pre-selected always-allowed: Phone, Messages, Camera, Settings, Dumbify. |
| 6. Bypass defaults | **Bulk-apply** at top: pick default `DELAY` / `PIN` / `DELAY_AND_PIN` + default delay, tap "Apply to all". Optional per-app override list below. |
| 7. Launcher | Toggle: "Replace home screen with minimal Dumbify launcher?" |
| 8. Done | Apply Device Owner restrictions, write `Config.onboardingComplete = true`, dismiss wizard. |

Cancelling mid-wizard: rules persisted as drafts; wizard resumes at last screen on next open. Restrictions NOT applied until Screen 8.

## C. Blocked app launch (runtime)

```
AppMonitorService loop (every ~500ms):
  fgPkg = UsageStatsManager.queryEvents(...).lastForegroundPackage()
  rule  = ruleStore.byPkg(fgPkg)
  if isBlocked(fgPkg, config, rule) and (rule?.grantedUntil ?: 0) < now:
    DevicePolicyManager.setPackagesSuspended(admin, [fgPkg], true)
    start BlockScreenActivity(fgPkg)
    log Event(BLOCK_HIT, fgPkg)
```

BlockScreenActivity UI:
- App icon + name
- `Config.customMessage`
- Button: **Request N min** (user picks 5 / 15 / 30 / custom)
- Button: **Go back** → finish(), launch home

## D. Unblock request

```kotlin
BypassController.requestUnblock(pkg, durationMinutes) {
    val rule = ruleStore.byPkg(pkg) ?: return refuse
    when (rule.bypassMode) {
        DELAY          -> runCountdown(rule.delaySeconds) { grant(pkg, durationMinutes) }
        PIN            -> promptPin { ok -> if (ok) grant(pkg, durationMinutes) }
        DELAY_AND_PIN  -> runCountdown(rule.delaySeconds) { promptPin { ok -> if (ok) grant(pkg, durationMinutes) } }
    }
}

fun grant(pkg, durationMinutes) {
    val until = now + durationMinutes * 60_000
    ruleStore.setGrantedUntil(pkg, until)
    policyEnforcer.setSuspended(pkg, false)
    workManager.enqueueOneTimeWorkRequestAt(until) { reSuspend(pkg) }
    log Event(UNBLOCK_GRANTED, pkg, "${durationMinutes}m")
}
```

PIN failures: 3 wrong entries → cooldown 5 min. Cooldown stored in EncryptedSharedPrefs (`pin_cooldown_until`) — survives reboot.

Countdown UI: full-screen, no skip, no back-press dismiss (consume back), seconds remaining shown large.

## E. Edit rules (mid-use)

Any edit that *weakens* protection (add to allowlist, remove from denylist, lower delay, switch mode) is gated by the same bypass flow as Screen 6 defaults — by default `Removal PIN` is required, configurable in Settings.

Edits that *strengthen* (remove from allowlist, add to denylist, raise delay) — no gate, immediate.

## F. Remove Dumbify

```
1. Settings → "Remove Dumbify".
2. Prompt: enter Removal PIN.
3. Wrong PIN → counter +1; 3 fails → cooldown 5 min.
4. Correct PIN:
     policyEnforcer.clearAllRestrictions()
     devicePolicyManager.clearDeviceOwnerApp(packageName)
     startActivity(Intent.ACTION_DELETE, data = "package:com.dumbify.app")
5. User confirms standard Android uninstall dialog. Done.
```

No delay on removal. Pure PIN gate. PIN holder is assumed to be a trusted person (or the user who deliberately forgot the PIN).
