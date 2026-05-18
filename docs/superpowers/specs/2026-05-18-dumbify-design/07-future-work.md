# Future Work

Tracked, not in v1. Roughly ordered by likely priority.

| # | Feature | Notes / dependencies |
|---|---|---|
| F1 | QR code provisioning | Alternate install path on factory-reset phone, no PC needed. Use Android's NFC/QR provisioning flow (`PROVISIONING_DEVICE_OWNER` extras). Useful when user lacks a PC. |
| F2 | Cloud backend + accounts | Multi-device sync of rules + audit log + partner web dashboard. Likely Supabase or Firebase. Optional, opt-in, paid hosting tier later. |
| F3 | **Remote unblock approval** | Partner approves usage from their phone, no PIN handoff. Pairs with F2. Push notification → partner approves N minutes → device receives signed grant. |
| F4 | F-Droid distribution | Submit to F-Droid for FOSS-store discovery + auto-update. No policy conflict. Requires reproducible builds. |
| F5 | Custom blocking browser | Firefox/Chromium fork like Limit Browser to enforce URL-level allowlist while still permitting some web. v1 has no web at all in dumb-phone mode. |
| F6 | Time controls | Schedule windows (e.g., 10pm–7am) and daily quotas (e.g., 30 min IG/day). Additive to current hard-block model. |
| F7 | SafeSearch enforcement | Force Google/Bing/YouTube safe mode via Private DNS / hosts override. |
| F8 | Adult content DNS blocklist | Curated category blocklists via Private DNS-over-TLS (NextDNS / ControlD / AdGuard free profile). |
| F9 | Notification stats | "You avoided N pings this week" — extends notif listener to count + display. |
| F10 | Auto-allowlist by category | Read Android `ApplicationInfo.category` to suggest essentials (productivity, navigation, finance) during setup. |
| F11 | iOS port | Fundamentally different MDM (Apple Configurator + supervised mode). Not feasible as free OSS. |
| F12 | Multi-language | i18n. v1 English only. |
| F13 | Theming / icon packs | Custom launcher cosmetics. |
| F14 | Accountability log export | CSV / PDF export of `events` table. |

## Notes carried from brainstorming

- **F3 (remote unblock)** explicitly requested by user 2026-05-18: "other person (friends/family/parents) can approve usage of the some app(s) (eg: instagram) for some time from their phone without the user need to give their phone to them for PIN entering and unblocking apps."
- **F1 (QR provisioning)** explicitly noted: ADB/USB is v1, QR is v2.
