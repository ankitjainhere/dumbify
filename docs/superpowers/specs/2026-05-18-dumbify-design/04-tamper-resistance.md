# Tamper Resistance

Applied via `DevicePolicyManager` on first run after Setup Screen 8. Release build only. Debug build skips most (see `09-development-safety.md`).

## User restrictions

```kotlin
private val RESTRICTIONS = listOf(
    UserManager.DISALLOW_FACTORY_RESET,
    UserManager.DISALLOW_SAFE_BOOT,
    UserManager.DISALLOW_APPS_CONTROL,           // blocks clear data / disable
    UserManager.DISALLOW_DEBUGGING_FEATURES,     // disables USB debug toggle
    UserManager.DISALLOW_UNINSTALL_APPS,         // belt-and-braces with setUninstallBlocked
    UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES // optional, prevents rival blockers (release only)
)
```

Applied:
```kotlin
RESTRICTIONS.forEach { dpm.addUserRestriction(admin, it) }
dpm.setUninstallBlocked(admin, BuildConfig.APPLICATION_ID, true)
dpm.setPermittedAccessibilityServices(admin, listOf(BuildConfig.APPLICATION_ID))
```

## Per-vector defenses

| Vector | Defense |
|---|---|
| Uninstall via Settings → Apps | `setUninstallBlocked(true)` + `DISALLOW_UNINSTALL_APPS` |
| Disable via Settings → Apps | DO apps cannot be disabled |
| Clear app data | `DISALLOW_APPS_CONTROL` |
| Factory reset from Settings | `DISALLOW_FACTORY_RESET` |
| Safe-mode boot to bypass services | `DISALLOW_SAFE_BOOT` |
| Stop foreground service via task killer | Service restarts itself; `BOOT_COMPLETED` receiver also restarts. Foreground notification keeps it pinned. |
| ADB `dpm remove-active-admin` | DO cannot be removed without factory reset. Plus `DISALLOW_DEBUGGING_FEATURES` blocks toggling USB debug. |
| Re-enable USB debugging | Restricted (`DISALLOW_DEBUGGING_FEATURES`) — user cannot turn on developer options |
| Install rival blocker / blocker-killer apps | `DISALLOW_INSTALL_UNKNOWN_SOURCES`; Play Store blocked apps in allowlist mode by default |
| Disable Accessibility / NotificationListener | `setPermittedAccessibilityServices(self only)` keeps ours allowed |

## Removal escape — only sanctioned path

See `03-user-flows.md` flow F. Requires correct Removal PIN. No delay.

## Last-resort physical escape (unavoidable, by Android design)

- Bootloader → recovery mode → factory reset — wipes everything including DO. Always possible on any Android phone. Document this on FAQ; reassure users they cannot truly brick the phone.

## Boot survival

```xml
<receiver android:name=".block.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

`BootReceiver.onReceive` → start `AppMonitorService` as foreground service.

`AppMonitorService` posts persistent notification (low priority, "Dumbify is protecting your focus") to qualify as foreground — required for long-running on Android 8+.

## Re-suspension worker

When unblock grant expires (`grantedUntil` reached), a `WorkManager` job re-calls `setPackagesSuspended(true)` and clears `grantedUntil`. Job is scheduled at `grant()` time and survives reboots via WorkManager persistence.
