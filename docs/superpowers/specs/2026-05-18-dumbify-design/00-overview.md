# Dumbify — Overview

Free, open-source Android app that turns a smartphone into a focused / "dumb" phone using Android's Device Owner (MDM) APIs. Alternative to paid limitphone.com.

## Goals

- Restrict unnecessary apps so user keeps only essentials (Maps, Notes, Payments, Phone, Messages, Camera, Settings, Dumbify).
- Strong tamper resistance — user cannot impulsively disable, uninstall, or factory-reset their way out (except final bootloader recovery wipe, which is always possible by design).
- Free forever. No backend in v1. No accounts. All data local.
- Open source under GPLv3.

## Non-goals (v1)

- iOS support (MDM model fundamentally different on iOS; would require Apple Configurator + supervised mode — not feasible for free OSS).
- Cloud sync, partner web dashboard, remote unblock approval (future work F2, F3).
- Custom browser, URL-level allowlist, SafeSearch enforcement (future work F5, F7).
- Time-based scheduling and daily quotas (future work F6).
- Play Store distribution (Device Owner policy gray area).

## Tech summary

| Piece | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Min SDK | 29 (Android 10) |
| Target SDK | latest stable |
| DB | Room |
| Secrets | EncryptedSharedPreferences |
| Background | Foreground service + WorkManager |
| PIN hashing | Argon2id |
| Distribution v1 | GitHub Releases + GitHub Pages installer site using WebUSB ADB (ya-webadb) |
| License | GPLv3 |
| CI | GitHub Actions |

## File index

| File | Purpose |
|---|---|
| `00-overview.md` | this file |
| `01-architecture.md` | high-level components, key Android APIs |
| `02-data-model.md` | Room entities, EncryptedSharedPrefs keys |
| `03-user-flows.md` | install, setup wizard, block, unblock, edit, remove |
| `04-tamper-resistance.md` | Device Owner restrictions, escape hatches |
| `05-module-structure.md` | package layout, source tree |
| `06-installer-site.md` | GitHub Pages WebUSB ADB installer |
| `07-future-work.md` | deferred features F1–F14 |
| `08-testing.md` | unit, integration, instrumentation, CI |
| `09-development-safety.md` | debug-build escape hatches so dev phone never bricks |
| `10-requirements-decisions.md` | Q&A log from brainstorming, decision rationale |
| `TRACKER.md` | live milestone tracker — update as dev progresses |
