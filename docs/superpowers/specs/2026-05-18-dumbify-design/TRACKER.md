# Dumbify — Dev Tracker

Live status of v1 milestones. Update as work progresses.

Legend:
- ⬜ TODO
- 🟡 IN PROGRESS
- ✅ DONE
- ❌ BLOCKED (add note)
- ⏸️ DEFERRED to future work (see `07-future-work.md`)

---

## M0 — Project bootstrap

| # | Task | Status | Notes |
|---|---|---|---|
| 0.1 | Create Android Studio project, package `com.dumbify.app`, Kotlin + Compose | ✅ | |
| 0.2 | Configure Gradle: min SDK 29, target SDK latest, JDK 17 | ✅ | |
| 0.3 | Add buildTypes `debug` + `release` with separate `applicationIdSuffix .debug` | ✅ | |
| 0.4 | Add deps: Compose, Hilt, Room, WorkManager, EncryptedSharedPreferences, Argon2-jvm, MockK, JUnit5 | ✅ | |
| 0.5 | Set up GitHub repo `dumbify`, GPLv3 LICENSE, README skeleton | ✅ | |
| 0.6 | GitHub Actions CI: assembleDebug + unit tests on push | ✅ | |

## M1 — Device Owner foundation

| # | Task | Status | Notes |
|---|---|---|---|
| 1.1 | `DumbifyDeviceAdminReceiver` + `res/xml/device_admin.xml` | ✅ | |
| 1.2 | `PolicyEnforcer` wrapping DevicePolicyManager | ✅ | |
| 1.3 | `BuildConfig.DEBUG`-gated restriction set (see `09-development-safety.md`) | ✅ | |
| 1.4 | `DevNukeReceiver` (debug variant only) + smoke test | ✅ | **Smoke-test BEFORE any other DO work** |
| 1.5 | Verify DO setup via `adb dpm set-device-owner` on emulator | ✅ | Verified offline; emulator smoke test deferred to Task 16 |

## M2 — Data layer

| # | Task | Status | Notes |
|---|---|---|---|
| 2.1 | Room db + entities (`Config`, `AppRule`, `Event`) | ✅ | |
| 2.2 | DAOs (`ConfigDao`, `AppRuleDao`, `EventDao`) | ✅ | |
| 2.3 | `SecurePrefs` wrapping EncryptedSharedPreferences | ✅ | |
| 2.4 | `PinManager` (Argon2id hash/verify, cooldown) + unit tests | ✅ | |
| 2.5 | `RuleStore` + `isBlocked()` eval + unit tests | ✅ | |

## M3 — App blocking

| # | Task | Status | Notes |
|---|---|---|---|
| 3.1 | `AppMonitorService` foreground service polling UsageStatsManager | ⬜ | Verify battery impact |
| 3.2 | Grant flow for `PACKAGE_USAGE_STATS` (Settings intent on first run) | ⬜ | |
| 3.3 | `setPackagesSuspended` integration in `PolicyEnforcer` | ⬜ | |
| 3.4 | `BlockScreenActivity` (Compose UI, custom message, action buttons) | ⬜ | |
| 3.5 | `BootReceiver` restarts service on boot | ⬜ | |
| 3.6 | `ReSuspendWorker` re-suspends apps when grant expires | ⬜ | |
| 3.7 | `BypassController` state machine (DELAY / PIN / DELAY_AND_PIN) + unit tests | ⬜ | |

## M4 — Notification suppression

| # | Task | Status | Notes |
|---|---|---|---|
| 4.1 | `DumbifyNotificationListener` service registered in manifest | ⬜ | |
| 4.2 | Grant flow for notif listener access | ⬜ | |
| 4.3 | Suppress notifs from blocked packages | ⬜ | |

## M5 — UI — Setup wizard

| # | Task | Status | Notes |
|---|---|---|---|
| 5.1 | Screen 1: Welcome + Role pick (SELF / MANAGED) | ⬜ | |
| 5.2 | Screen 2: Mode pick (ALLOWLIST / DENYLIST) | ⬜ | |
| 5.3 | Screen 3: PINs (Removal mandatory, Bypass conditional) | ⬜ | |
| 5.4 | Screen 4: Custom motivational message | ⬜ | |
| 5.5 | Screen 5: App picker (with PackageManager loader, always-allowed pre-selected) | ⬜ | |
| 5.6 | Screen 6: Bulk-apply bypass defaults + per-app overrides | ⬜ | |
| 5.7 | Screen 7: Optional launcher toggle | ⬜ | |
| 5.8 | Screen 8: Apply restrictions, mark `onboardingComplete` | ⬜ | |
| 5.9 | `SetupViewModel` state machine + draft persistence (resume on abort) | ⬜ | |

## M6 — UI — Main app

| # | Task | Status | Notes |
|---|---|---|---|
| 6.1 | `HomeScreen` — list of rules, current grants, status banner | ⬜ | |
| 6.2 | Edit rule sheet (toggle allowed, change bypass, change delay) | ⬜ | |
| 6.3 | Gate weakening edits with PIN/delay | ⬜ | |
| 6.4 | `SettingsScreen` — change PINs, change mode, view audit log, **Remove Dumbify** | ⬜ | |
| 6.5 | Compose Material3 theme | ⬜ | |

## M7 — Optional launcher

| # | Task | Status | Notes |
|---|---|---|---|
| 7.1 | `DumbifyLauncherActivity` with HOME intent-filter (gated by `Config.launcherEnabled`) | ⬜ | |
| 7.2 | Grid of allowed apps (PackageManager icons + labels) | ⬜ | |
| 7.3 | Set Dumbify as default home on toggle (`addPersistentPreferredActivity` via DO) | ⬜ | |

## M8 — Tamper resistance

| # | Task | Status | Notes |
|---|---|---|---|
| 8.1 | Apply release-build user restrictions | ⬜ | |
| 8.2 | `setUninstallBlocked` on release | ⬜ | |
| 8.3 | Removal flow (PIN gate → `clearDeviceOwnerApp` → uninstall intent) | ⬜ | |
| 8.4 | PIN brute-force cooldown wiring | ⬜ | |

## M9 — Installer site

| # | Task | Status | Notes |
|---|---|---|---|
| 9.1 | `/site` skeleton (HTML/CSS/JS, no framework) | ⬜ | |
| 9.2 | Integrate ya-webadb, USB device discovery | ⬜ | |
| 9.3 | Pre-flight checks (Android version, accounts) | ⬜ | |
| 9.4 | APK fetch from GitHub Releases API | ⬜ | |
| 9.5 | Install + `set-device-owner` via WebADB | ⬜ | |
| 9.6 | Error handling + remediation copy | ⬜ | |
| 9.7 | Deploy to GitHub Pages | ⬜ | |
| 9.8 | (Optional) custom domain `dumbify.app` | ⬜ | |

## M10 — Testing & release

| # | Task | Status | Notes |
|---|---|---|---|
| 10.1 | Unit tests pass in CI | ⬜ | |
| 10.2 | Compose UI tests for setup wizard + block screen | ⬜ | |
| 10.3 | Instrumentation tests for `PolicyEnforcer` on emulator with DO | ⬜ | |
| 10.4 | Manual smoke test on secondary device (boot survival, removal flow) | ⬜ | |
| 10.5 | Release signing keystore in GitHub Secrets | ⬜ | |
| 10.6 | Release workflow: tag → signed APK → GitHub Release | ⬜ | |
| 10.7 | v1.0.0 release | ⬜ | |

---

## Future work (not in v1 — see `07-future-work.md`)

| ID | Feature | Status |
|---|---|---|
| F1 | QR provisioning | ⏸️ |
| F2 | Cloud backend + accounts | ⏸️ |
| F3 | Remote unblock approval | ⏸️ |
| F4 | F-Droid distribution | ⏸️ |
| F5 | Custom browser | ⏸️ |
| F6 | Time controls (schedules + quotas) | ⏸️ |
| F7 | SafeSearch enforcement | ⏸️ |
| F8 | Adult DNS blocklist | ⏸️ |
| F9 | Notification stats | ⏸️ |
| F10 | Auto-allowlist by category | ⏸️ |
| F11 | iOS port | ⏸️ |
| F12 | Multi-language | ⏸️ |
| F13 | Theming | ⏸️ |
| F14 | Audit log export | ⏸️ |

---

## Change log

| Date | Note |
|---|---|
| 2026-05-18 | Initial tracker created from design spec |
| 2026-05-18 | Plan 1 complete: M0, M1, M2 done. Bootstrap + DO + data layer landed. |
