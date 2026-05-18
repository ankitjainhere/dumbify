# Development Safety

Critical. Device Owner mistakes can lock a dev out of their phone. This doc keeps the dev phone recoverable.

## Build variants

`app/build.gradle.kts` defines two product flavors (or buildTypes):

| Property | `debug` | `release` |
|---|---|---|
| `BuildConfig.DEBUG` | true | false |
| `applicationId` | `com.dumbify.app.debug` (separate install) | `com.dumbify.app` |
| `DISALLOW_FACTORY_RESET` | NOT applied | applied |
| `DISALLOW_DEBUGGING_FEATURES` | NOT applied | applied |
| `DISALLOW_UNINSTALL_APPS` | NOT applied | applied |
| `DISALLOW_INSTALL_UNKNOWN_SOURCES` | NOT applied | applied |
| `setUninstallBlocked` | NOT applied | applied |
| `setPermittedAccessibilityServices` | NOT applied | applied |
| `DevNukeReceiver` | present | absent |
| Hardcoded dev removal PIN `000000` | accepted as removal PIN | rejected |

Implementation in `PolicyEnforcer`:

```kotlin
fun applyRestrictions() {
    val all = listOf(
        UserManager.DISALLOW_FACTORY_RESET,
        UserManager.DISALLOW_DEBUGGING_FEATURES,
        UserManager.DISALLOW_UNINSTALL_APPS,
        UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
        UserManager.DISALLOW_SAFE_BOOT,
        UserManager.DISALLOW_APPS_CONTROL
    )
    val restrictions = if (BuildConfig.DEBUG) {
        // debug keeps factory-reset, debugging, uninstall available as escape hatches
        listOf(UserManager.DISALLOW_APPS_CONTROL, UserManager.DISALLOW_SAFE_BOOT)
    } else all

    restrictions.forEach { dpm.addUserRestriction(adminComponent, it) }
    if (!BuildConfig.DEBUG) {
        dpm.setUninstallBlocked(adminComponent, packageName, true)
    }
}
```

## DevNukeReceiver (debug variant only)

Lives in `src/debug/java/com/dumbify/app/debug/DevNukeReceiver.kt`. Manifest in `src/debug/AndroidManifest.xml` registers it. Trigger from PC:

```bash
adb shell am broadcast \
  -a com.dumbify.app.debug.DEV_NUKE \
  -n com.dumbify.app.debug/com.dumbify.app.debug.DevNukeReceiver
```

Receiver implementation:

```kotlin
class DevNukeReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val dpm = c.getSystemService(DevicePolicyManager::class.java) ?: return
        val admin = ComponentName(c, DumbifyDeviceAdminReceiver::class.java)
        // Drop every restriction we might have set
        listOf(
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_UNINSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_APPS_CONTROL
        ).forEach { dpm.clearUserRestriction(admin, it) }
        runCatching { dpm.setUninstallBlocked(admin, c.packageName, false) }
        runCatching { dpm.clearDeviceOwnerApp(c.packageName) }
        // app is now uninstallable normally; user can `adb uninstall com.dumbify.app.debug`
    }
}
```

## Dev workflow rules

1. **Always run `debug` variant locally**. Never install release APK on dev phone.
2. **Use a secondary device or emulator** for first DO smoke test of any new restriction.
3. **Keep `adb` connection alive** during DO testing — debug build keeps `DISALLOW_DEBUGGING_FEATURES` off, so USB debug toggle remains available.
4. **Smoke-test order on new device:**
   - Install debug APK
   - Set DO
   - Confirm `DevNukeReceiver` works (run it, verify uninstall succeeds)
   - Re-install debug APK + set DO
   - Now safe to develop further
5. **Escape ladder**, in order of preference, if dev phone misbehaves:
   - a. `adb shell am broadcast ... DEV_NUKE` then `adb uninstall com.dumbify.app.debug`
   - b. Settings → Factory reset (allowed in debug build)
   - c. Bootloader → recovery → factory reset (always works on any phone)

## Lint guard

Add a unit test that asserts release build does NOT include `DevNukeReceiver`:

```kotlin
@Test fun `release build excludes DevNukeReceiver`() {
    if (!BuildConfig.DEBUG) {
        assertThrows<ClassNotFoundException> {
            Class.forName("com.dumbify.app.debug.DevNukeReceiver")
        }
    }
}
```

## FAQ users to direct to

Mirror this guidance (minus the nuke receiver) to the public FAQ on installer site (`06-installer-site.md`), so end users with shipping release APK still know that **bootloader → recovery → factory reset** always works.
