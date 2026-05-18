# Testing

## Layers

| Layer | Targets | Tools |
|---|---|---|
| Unit | `PinManager` (hash, verify, cooldown), `BypassController` state machine, `RuleStore.isBlocked()`, `Time` clock | JUnit5, MockK, kotlinx-coroutines-test |
| DB | Room DAO queries, schema migrations (when v2 arrives) | Room testing, in-memory db |
| UI (Compose) | Setup wizard navigation, BlockScreenActivity render, HomeScreen list | `androidx.compose.ui.test` |
| Instrumentation | `PolicyEnforcer` applying restrictions, `AppMonitorService` foreground detection, `DumbifyNotificationListener` suppression | Emulator with Device Owner pre-set |
| Manual | Boot survival, USB-debug-blocked, factory-reset-blocked (release flavor only!), launcher swap on real device | secondary physical device |

## Emulator Device Owner setup (for instrumentation)

```bash
# Start emulator with no accounts:
emulator -avd Pixel_API_34 -no-snapshot-load

# After boot:
adb install app-debug.apk
adb shell dpm set-device-owner com.dumbify.app/.admin.DumbifyDeviceAdminReceiver
```

Wrap in Gradle task `setupEmulatorDO` for repeatable test runs.

## CI

GitHub Actions workflow `.github/workflows/ci.yml`:

```yaml
- on: push, pull_request to main
- job build-and-test:
    - setup JDK 17
    - cache Gradle
    - ./gradlew :app:assembleDebug :app:testDebugUnitTest
    - ./gradlew :app:lintDebug
    - upload debug APK as artifact
- job ui-test (optional v1, slower):
    - reactivecircus/android-emulator-runner
    - api-level: 29, 34
    - ./gradlew :app:connectedDebugAndroidTest
```

Release workflow `.github/workflows/release.yml`:

```yaml
- on: push tag v*
- job release:
    - setup JDK 17
    - decode keystore from secrets
    - ./gradlew :app:assembleRelease
    - gh release create with APK
    - installer site picks up new release via GitHub API at next visit
```

## Test data

- Sample `app_rules` fixtures in `test/resources/fixtures/`
- `Time` injected (not `System.currentTimeMillis()` directly) so countdown / grant-expiry tests are deterministic

## Non-goals for v1 testing

- Real-device farm (Firebase Test Lab, Browserstack) — out of scope
- Performance benchmarks — out of scope; revisit if `AppMonitorService` polling shows battery issues
