# Architecture

Single native Android app. Runs as Device Owner. No backend.

## Components

```
┌─────────────────────────────────────────────────┐
│  Dumbify APK (com.dumbify.app)                  │
├─────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                     │
│    - Setup wizard                               │
│    - Rules editor / home dashboard              │
│    - Settings (PIN edit, remove app)            │
│    - BlockScreenActivity                        │
│    - DumbifyLauncherActivity (optional home)    │
├─────────────────────────────────────────────────┤
│  Core Services                                  │
│    - DumbifyDeviceAdminReceiver (DO callbacks)  │
│    - AppMonitorService (foreground service)     │
│    - DumbifyNotificationListener (suppress)     │
│    - BootReceiver (restart on boot)             │
├─────────────────────────────────────────────────┤
│  Policy Engine                                  │
│    - RuleStore                                  │
│    - BypassController (delay + PIN state)       │
│    - PinManager (Argon2id)                      │
│    - PolicyEnforcer (wraps DevicePolicyManager) │
├─────────────────────────────────────────────────┤
│  Persistence: Room DB + EncryptedSharedPrefs    │
└─────────────────────────────────────────────────┘

External:
  - GitHub Pages installer site (WebUSB ADB) — static, no backend
  - GitHub Releases — signed APK distribution
```

## Key Android APIs

| Concern | API |
|---|---|
| App launch detection | `UsageStatsManager.queryEvents` polled every ~500ms by `AppMonitorService` (alternative: `AccessibilityService` for instant detection — fallback option if polling latency is poor) |
| Hard app block | `DevicePolicyManager.setPackagesSuspended(admin, pkgs, true)` — kills + prevents launch when DO |
| Block screen | `Activity` launched with `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TOP` when blocked launch detected |
| Notification suppression | `NotificationListenerService.cancelNotification(key)` for keys whose pkg matches blocked rule |
| Custom launcher | Activity with intent-filter `action.MAIN` + `category.HOME` + `category.DEFAULT` |
| Boot survival | `BroadcastReceiver` for `BOOT_COMPLETED` → starts `AppMonitorService` |
| DO setup | `dpm set-device-owner com.dumbify.app/.admin.DumbifyDeviceAdminReceiver` via ADB |
| User restrictions | `DevicePolicyManager.addUserRestriction(...)` — see `04-tamper-resistance.md` |
| Uninstall block | `DevicePolicyManager.setUninstallBlocked(admin, pkg, true)` |
| Self-cleanup | `DevicePolicyManager.clearDeviceOwnerApp(pkgName)` then `requestPackageDelete` |

## Dual mode

User picks mode at setup; switchable later (gated by Removal PIN to discourage casual change):

- **Allowlist** — every package is blocked unless its `AppRule.isAllowed == true`. Default safe stance, matches "dumbify" intent.
- **Denylist** — every package allowed unless its `AppRule.isAllowed == false`. Less strict.

Mode lives in `Config.mode`. Evaluation is a single function:

```kotlin
fun isBlocked(pkg: String, config: Config, rule: AppRule?): Boolean = when (config.mode) {
    ALLOWLIST -> rule?.isAllowed != true
    DENYLIST  -> rule?.isAllowed == false
}
```

Dumbify itself, Phone, Messages, Settings, Camera are always-allowed (hardcoded set) regardless of mode — prevents user locking themselves out.
