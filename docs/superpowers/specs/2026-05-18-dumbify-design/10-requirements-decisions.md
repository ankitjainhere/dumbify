# Requirements & Decisions — Brainstorming Log

Captured during brainstorming session on 2026-05-18 with user.

## Q&A summary

| # | Question | Answer | Rationale / notes |
|---|---|---|---|
| Q1 | Primary user model | **Both** (SELF + MANAGED) | User wants flexibility — single app serves both self-control and parent-child use cases |
| Q2 | Bypass mechanism | **Per-rule selectable** (DELAY / PIN / DELAY_AND_PIN) | Different apps deserve different friction levels (IG = strong, YouTube = mild) |
| Q3 | Blocking scope | App blocking + dual mode (allowlist + denylist), **no custom browser**, web filtering deferred | User's intent: "dumbify the smartphone" — keep essentials only. Allowlist matches that better than blocklist. Skipping custom browser saves massive scope. |
| Q4 | Custom launcher | **Optional** (Screen 7 toggle at setup) | Some users want full minimal home grid; others want stock launcher with block-on-launch only |
| Q5 | Time controls | **Hard block only v1** | Schedules + quotas deferred to F6 to keep MVP tight |
| Q6 | DO setup method | **ADB/USB v1**; **QR provisioning deferred (F1)** | Matches limitphone, free users tolerate one-time PC setup |
| Q7 | Distribution | **GitHub Pages + WebUSB ADB installer + GitHub Releases**; F-Droid in v2 (F4); skip Play Store | Mirrors limitphone install model; no Play policy risk; truly free |
| Q8 | Backend | **Fully local v1**; cloud in F2 + F3 | Free + simple. Cloud later for sync + remote partner unblock. |
| Q9 | Block screen | **Full-screen lock with user-customizable motivational message** | Strongest psychological friction for self-control use case |
| Q10 | Notifications from blocked apps | **Suppress silently** | Prevents temptation leak. Notif-count stats deferred (F9). |
| Q11 | Tech stack | **Kotlin + Jetpack Compose, native Android** | Device Owner APIs are Android-native; cross-platform frameworks bring no benefit (iOS not in scope) |
| Q12 | Min Android | **API 29 (Android 10)** | Full DO API set, ~90% device coverage |
| Q13 | Project name | **Dumbify** | User suggested. Package: `com.dumbify.app`. Repo: `dumbify`. |
| Q14 | License | **GPLv3** | Copyleft — forks must stay open; F-Droid compatible |

## Sub-decisions

| Decision | Choice | Reason |
|---|---|---|
| Single PIN vs two PINs | **Two PINs** (Removal mandatory + Bypass optional) | User wants removal gated by a person other than themselves; per-app bypass is a separate, optional convenience PIN |
| Removal flow | **Pure PIN, no delay** | User feedback: "Removing the Dumbify should not be this much restricted" — third party holds PIN, that is the gate |
| Default bypass selection | **Bulk-apply control on Screen 6** with optional per-app override | User feedback: "there should be option to select DELAY or PIN at one click" |
| Always-allowed apps | Phone, Messages, Camera, Settings, Dumbify itself | Prevents user locking themselves out of essentials and out of Dumbify settings |
| Build variants | **Debug variant skips most DO restrictions, ships `DevNukeReceiver`** | User feedback: "during the development, there should be some workaround to remove the dumbify app, if there is some bug in the app... my phone will become a brick, keep that in mind" |

## Explicitly captured for future

- **F3 — Remote unblock approval** (partner approves from their phone): user-requested 2026-05-18.
- **F1 — QR provisioning** as alternate install: noted at Q6.
- **F2 — Cloud backend + partner dashboard** at Q8c.
- **F4 — F-Droid** at Q7.
- **F5 — Custom browser** if URL-level filtering desired later.
- **F6 — Time controls** (schedules + quotas) at Q5.
- **F11 — iOS port** acknowledged as not feasible for free OSS.
