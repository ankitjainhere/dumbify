# Module Structure

Single Gradle module `:app`. No multi-module split in v1 (keep simple).

```
app/
├─ build.gradle.kts
├─ src/
│  ├─ main/
│  │  ├─ AndroidManifest.xml
│  │  ├─ java/com/dumbify/app/
│  │  │  ├─ DumbifyApp.kt                          # Application; DI init (Hilt)
│  │  │  │
│  │  │  ├─ admin/
│  │  │  │  ├─ DumbifyDeviceAdminReceiver.kt       # DO callbacks (onEnabled, onProfileProvisioningComplete)
│  │  │  │  └─ PolicyEnforcer.kt                   # wraps DevicePolicyManager calls
│  │  │  │
│  │  │  ├─ block/
│  │  │  │  ├─ AppMonitorService.kt                # foreground service polling UsageStatsManager
│  │  │  │  ├─ BlockScreenActivity.kt              # full-screen block UI
│  │  │  │  ├─ BootReceiver.kt                     # BOOT_COMPLETED → start AppMonitorService
│  │  │  │  └─ ReSuspendWorker.kt                  # WorkManager re-suspend on grant expiry
│  │  │  │
│  │  │  ├─ launcher/
│  │  │  │  └─ DumbifyLauncherActivity.kt          # optional minimal home (Compose grid of allowed apps)
│  │  │  │
│  │  │  ├─ notif/
│  │  │  │  └─ DumbifyNotificationListener.kt      # NotificationListenerService; suppress blocked pkgs
│  │  │  │
│  │  │  ├─ policy/
│  │  │  │  ├─ RuleStore.kt                        # Room DAO wrapper, isBlocked() eval
│  │  │  │  ├─ BypassController.kt                 # delay + PIN state machine
│  │  │  │  └─ PinManager.kt                       # Argon2id hash, verify, cooldown
│  │  │  │
│  │  │  ├─ data/
│  │  │  │  ├─ DumbifyDb.kt
│  │  │  │  ├─ entities/Config.kt
│  │  │  │  ├─ entities/AppRule.kt
│  │  │  │  ├─ entities/Event.kt
│  │  │  │  ├─ dao/ConfigDao.kt
│  │  │  │  ├─ dao/AppRuleDao.kt
│  │  │  │  ├─ dao/EventDao.kt
│  │  │  │  └─ SecurePrefs.kt                      # EncryptedSharedPreferences wrapper
│  │  │  │
│  │  │  ├─ ui/
│  │  │  │  ├─ setup/                              # 8 wizard screens (one Compose file each)
│  │  │  │  │  ├─ SetupViewModel.kt
│  │  │  │  │  └─ screens/Screen1Welcome.kt ... Screen8Done.kt
│  │  │  │  ├─ home/HomeScreen.kt                  # main rules dashboard
│  │  │  │  ├─ home/HomeViewModel.kt
│  │  │  │  ├─ settings/SettingsScreen.kt          # edit PINs, change mode, remove app
│  │  │  │  ├─ settings/SettingsViewModel.kt
│  │  │  │  ├─ common/                             # shared Compose widgets (PinPad, AppRow, etc.)
│  │  │  │  └─ theme/                              # Compose Material3 theme, colors, typography
│  │  │  │
│  │  │  └─ util/
│  │  │     ├─ AppInfoLoader.kt                    # PackageManager helpers
│  │  │     └─ Time.kt                             # clock abstraction for tests
│  │  │
│  │  └─ res/
│  │     ├─ xml/device_admin.xml                   # DeviceAdminReceiver policies
│  │     ├─ values/strings.xml
│  │     ├─ drawable/                              # icon, block screen art
│  │     └─ mipmap-*/                              # launcher icon
│  │
│  ├─ debug/
│  │  ├─ java/com/dumbify/app/debug/DevNukeReceiver.kt
│  │  └─ AndroidManifest.xml                       # registers DevNukeReceiver
│  │
│  ├─ release/                                     # release-only overrides (currently empty placeholder)
│  │
│  ├─ test/
│  │  └─ java/com/dumbify/app/
│  │     ├─ policy/PinManagerTest.kt
│  │     ├─ policy/BypassControllerTest.kt
│  │     ├─ policy/RuleStoreTest.kt
│  │     └─ data/MigrationTest.kt
│  │
│  └─ androidTest/
│     └─ java/com/dumbify/app/
│        ├─ ui/SetupWizardTest.kt                  # Compose UI test
│        ├─ ui/BlockScreenTest.kt
│        └─ admin/PolicyEnforcerTest.kt            # requires emulator with DO
```

## Dependency injection

Hilt. Top-level modules in `di/` (add when needed). For v1 keep injection light — only `RuleStore`, `BypassController`, `PolicyEnforcer`, `PinManager`, `SecurePrefs`.

## Manifest highlights

- `<receiver android:name=".admin.DumbifyDeviceAdminReceiver" android:permission="android.permission.BIND_DEVICE_ADMIN">` with `device_admin` xml
- `<service android:name=".block.AppMonitorService" android:foregroundServiceType="specialUse">` (or `dataSync` depending on target SDK)
- `<service android:name=".notif.DumbifyNotificationListener" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">`
- `<activity android:name=".launcher.DumbifyLauncherActivity">` with intent-filter `MAIN` + `HOME` + `DEFAULT` (only if user enables launcher)
- `<activity android:name=".block.BlockScreenActivity" android:launchMode="singleTask" android:excludeFromRecents="true">`

## Permissions

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" tools:ignore="ProtectedPermissions"/>
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>    <!-- optional, for block overlay fallback -->
```

Usage stats permission is special — granted via `Settings.ACTION_USAGE_ACCESS_SETTINGS` on first run; as Device Owner we can also grant via `setApplicationRestrictions` workaround on some OEMs (verify during dev).
