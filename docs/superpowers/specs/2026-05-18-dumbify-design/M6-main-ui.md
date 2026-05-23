# M6 — Main App UI

Design spec for Milestone 6: HomeScreen, EditRuleSheet, SettingsScreen, PinDialog, and Material3 theme.
Source of truth: `design/design_handoff_dumbify/` (open `Dumbify.html` in browser for interactive prototype).

---

## Confirmed design choices

| Decision | Choice |
|---|---|
| Home banner variant | **B — Dashboard card** (surfaceContainer card, shield icon, DO status headline, mode + count subtitle) |
| Rule row variant | **C — Trailing toggle** (Switch on right + amber granted chip below) |
| Density | **A — Comfortable** (12dp vertical padding, 44dp app icon) |
| Seed color | Forest green |
| Theme | Dark primary target; light also supported |

---

## File structure

```
app/src/main/java/com/dumbify/app/
├── ui/
│   ├── theme/
│   │   └── Theme.kt              # DumbifyTheme + DumbifyColors custom tokens
│   ├── home/
│   │   ├── HomeViewModel.kt
│   │   └── HomeScreen.kt         # HomeScreen + EditRuleSheet + PinDialog (reused)
│   └── settings/
│       ├── SettingsViewModel.kt
│       └── SettingsScreen.kt
└── MainActivity.kt               # nav state + DumbifyTheme wrapper
```

No Navigation Compose dep. `MainActivity` holds `var currentScreen by mutableStateOf(Screen.HOME)`.

---

## Theme (`ui/theme/Theme.kt`)

### Color scheme — forest green seed

**Dark (primary target):**

| Token | Hex |
|---|---|
| primary | `#88D993` |
| onPrimary | `#003912` |
| primaryContainer | `#1F5026` |
| onPrimaryContainer | `#A4F5AE` |
| secondary | `#B5CCB5` |
| onSecondary | `#213524` |
| secondaryContainer | `#374B39` |
| onSecondaryContainer | `#D1E8D0` |
| error | `#FFB4AB` |
| onError | `#690005` |
| errorContainer | `#93000A` |
| onErrorContainer | `#FFDAD6` |
| background / surface | `#0F140F` |
| onSurface | `#E0E4DC` |
| surfaceContainerLowest | `#0A0F0A` |
| surfaceContainerLow | `#171C17` |
| surfaceContainer | `#1B201B` |
| surfaceContainerHigh | `#262B25` |
| surfaceContainerHighest | `#303530` |
| onSurfaceVariant | `#C0C9BD` |
| outline | `#8A9388` |
| outlineVariant | `#404941` |

**Light:**

| Token | Hex |
|---|---|
| primary | `#2D6A39` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#AEF2B8` |
| onPrimaryContainer | `#002109` |
| secondaryContainer | `#D2E8D2` |
| onSecondaryContainer | `#0E1F12` |
| error | `#BA1A1A` |
| errorContainer | `#FFDAD6` |
| onErrorContainer | `#410002` |
| background / surface | `#F7FBF2` |
| onSurface | `#181D17` |
| surfaceContainer | `#EBEFE6` |
| surfaceContainerLow | `#F1F5EC` |
| surfaceContainerHigh | `#E5E9E0` |
| outlineVariant | `#C1C9BF` |

### Custom tokens (`DumbifyColors` data class — not in M3 scheme)

```kotlin
data class DumbifyColors(
    val amber: Color,
    val amberContainer: Color,
    val onAmberContainer: Color,
    val successDot: Color,
    val dangerDot: Color,
)

val LocalDumbifyColors = staticCompositionLocalOf { DumbifyColorsDark }
```

| Token | Dark | Light |
|---|---|---|
| amber | `#FFD68A` | `#7A5A00` |
| amberContainer | `#4D3E13` | `#FFE08A` |
| onAmberContainer | `#FFE3A8` | `#261A00` |
| successDot | `#73DD80` | `#2E7D32` |
| dangerDot | `#FF8A80` | `#C62828` |

Use `dynamicColorScheme` on API 31+; fall back to explicit dark/light schemes above.

### Typography

- Font family: Roboto (M3 default on Android — no override needed)
- Audit log timestamps: `fontFeatureSettings = "tnum"` (tabular-nums)
- PIN input field: Roboto Mono, 22sp, letterSpacing 0.4em

### Spacing

| Constant | Value |
|---|---|
| Horizontal margin | 16dp |
| Row vertical padding (comfortable) | 12dp |
| App icon size (comfortable) | 44dp |
| App icon corner radius | ~12dp (≈27% of 44dp) |
| Bottom sheet corner radius (top) | 28dp |
| Dialog corner radius | 28dp |
| Card / inner container corner radius | 16dp |
| Chip height (md) | 28dp |
| Chip height (sm) | 22dp |
| Row divider: 1dp `outlineVariant @ 35% alpha`, inset 16dp both sides |

---

## Screen 1 — HomeScreen

### Layout

```
TopAppBar (small, 64dp)
  title "Dumbify" (headlineSmall / 22sp / weight 500)
  trailing: IconButton(Icons.Filled.Settings) → currentScreen = SETTINGS

StatusBanner (card variant)
  surfaceContainer card, 16dp radius, 14dp/16dp padding, 14dp gap
  leading: 40dp circle (primaryContainer if protected, errorContainer if not)
             contains shieldCheck or warn icon (22dp)
  center:  "Device Owner active" / "Device Owner missing" (14sp/500)
           "{mode label} mode · {n} apps" (12sp, onSurfaceVariant)
  trailing: chevron icon (20dp, onSurfaceVariant) → tap navigates to Settings

LazyColumn (flex 1, 24dp bottom padding)
  rows separated by 1dp divider (outlineVariant @ 35% alpha, inset 16dp)
  each row: RuleRow (trailing toggle variant, comfortable density)
```

### RuleRow — trailing toggle variant

```
Row { padding 12dp vertical, 16dp horizontal, gap 14dp }
  AppIcon(44dp, cornerRadius 12dp)
    if blocked: alpha=0.55f, ColorFilter.colorMatrix(saturate 0.5)
  Column { flex 1 }
    app name (15sp/500, onSurface — or onSurface@0.60 alpha if blocked)
    package name (12sp, onSurfaceVariant, single line + ellipsis, opacity 0.65 if blocked)
  Column { items end, gap 6dp }
    Switch(checked = rule.isAllowed, onCheckedChange = { vm.onToggle(rule) })
      toggle ON (false→true) triggers PIN gate before applying
    if rule.grantedUntil != null && grantedUntil > now:
      AssistChip(22dp height, amberContainer bg, onAmberContainer text)
        Schedule icon (12dp) + "Granted until HH:mm"
```

Tap the row body → opens EditRuleSheet for that rule.
Tap the Switch thumb → same PIN gating as Save in sheet (toggle from blocked→allowed = weakening).

---

## Sheet — EditRuleSheet

`ModalBottomSheet` from `androidx.compose.material3`.
Shape: `RoundedCornerShape(topStart=28.dp, topEnd=28.dp)`.
`containerColor = MaterialTheme.colorScheme.surfaceContainerLow`.

### Layout

```
drag handle (auto from ModalBottomSheet)

title row (24dp horizontal, 14dp gap)
  AppIcon(48dp)
  Column { app name (20sp/500), package name (12sp, onSurfaceVariant) }

Card "Allow this app" (surfaceContainer, 16dp radius, 16dp margin, 14dp/16dp inner padding)
  Row { flex 1 }
    Column
      "Allow this app" (15sp/500)
        if rule.isAllowed==false && editAllowed==true: prepend Lock icon (14dp, amber)
      subtitle (12sp, onSurfaceVariant):
        true  → "App can open immediately (subject to bypass)"
        false → "App will be hidden and blocked from launch"
  Switch(checked = editAllowed)

Card "Bypass Mode" (same container styling)
  eyebrow "BYPASS MODE" (12sp, onSurfaceVariant, letterSpacing 0.4sp)
  SingleChoiceSegmentedButtonRow
    Delay / PIN / Delay + PIN
    selected: secondaryContainer bg + check icon
  description (12sp, onSurfaceVariant, lineHeight 1.4):
    DELAY     → "User waits the delay period, then app opens."
    PIN       → "User enters Bypass PIN to open."
    DELAY+PIN → "User waits the delay, then enters Bypass PIN."

Card "Delay Duration" (same, opacity 0.4 + disabled when bypass == PIN only)
  header row: eyebrow + formatted value (18sp/500, tabular-nums)
  Slider(0..300, step=5) — track 16dp tall
  labels "0s" / "5m" (11sp, onSurfaceVariant)
  formatDelay: 0→"No delay", <60→"{n}s", else "{m}m [{r}s]"

Warning (amber, only if editAllowed && !rule.isAllowed)
  amber@10% alpha bg, 12dp radius, 14dp/12dp padding
  Lock icon (18dp, amber) + "Changes that reduce protection require your Removal PIN."

Action row (right-aligned, 8dp gap)
  TextButton "Cancel" → dismiss sheet
  FilledButton "Save" → vm.saveEdit(oldRule, newRule)
```

---

## Screen 2 — SettingsScreen

### Layout

```
TopAppBar
  leading: back arrow → currentScreen = HOME
  title "Settings"

LazyColumn (scrollable, 32dp bottom padding)

  Section "BLOCK MODE"
    RadioRow: Allowlist / Denylist (each has title + subtitle)
    Info row: info icon + "Changing mode requires Removal PIN." (12sp, onSurfaceVariant)

  Section "CHANGE PINS"
    ListItem: shield icon | "Change Removal PIN" | "High-friction PIN..." | chevron
    ListItem: pin icon    | "Change Bypass PIN"  | "Used during temporary..." | chevron
    tap → triggers two-step PinDialog flow (see "Change PIN flow" section below)

  Section "AUDIT LOG"
    toggle row (history icon, "{n} recent events", chevron rotates on expand)
    when expanded: surfaceContainerLow card, 12dp radius, maxHeight 240dp, scrollable
      each AuditEventRow:
        timestamp (11sp, tabular-nums, fixed 38dp)
        event chip (sm, tone from eventToneMap below)
        app name (13sp/500) + detail (13sp, onSurfaceVariant)
      1dp outlineVariant@25% dividers between rows

  Section "DANGER ZONE" (sectionHeader in error color)
    card: error@8% alpha bg, 1px error@25% border, 16dp radius, 14dp/16dp padding
      warn icon (20dp, error) + "Remove Dumbify" title (14sp/500, error)
      body (12sp, onSurface@85%, lineHeight 1.4): "This will remove all restrictions and uninstall the app. Requires Removal PIN."
      FilledButton(colors = error-toned) "Remove Dumbify" → vm.startRemoval()
```

### Audit event tone map

| EventType | Chip tone |
|---|---|
| UNBLOCK_GRANTED | amber |
| BLOCK_HIT | danger (error) |
| UNBLOCK_DENIED | danger |
| PIN_FAIL | danger |
| UNBLOCK_REQUEST | neutral (outline) |
| RULE_EDIT | secondary |
| MODE_CHANGE | secondary |
| REMOVAL_ATTEMPT | danger |

### Change PIN flow

Tap "Change Removal PIN" or "Change Bypass PIN":
1. PinDialog("verify existing PIN") — verify old PIN via `PinManager.verify(scope, pin)`
2. On success: PinDialog("enter new PIN") — `PinManager.setPin(scope, newPin)`
3. Dismiss

---

## Component — PinDialog

Reused in HomeScreen (weakening edit gate, trailing toggle gate) and SettingsScreen (mode change, PIN change, removal).

`AlertDialog` with custom content. Width cap: 320dp.

```
Container (surfaceContainerHigh, 28dp radius)
  48dp circle (surfaceContainerHighest bg)
    Lock icon (24dp, primary)
  title (22sp/500)
  subtitle (13sp, onSurfaceVariant)
  PIN field
    background: surfaceContainerLow
    border: 1dp outline (error when error state)
    8dp radius, 16dp/14dp inner padding
    22sp Roboto Mono, center, letterSpacing 0.4em, numeric input, max 6 digits
  error text (12sp, error):
    wrong  → "Incorrect PIN. {n} attempts remaining."
    locked → "Too many attempts. Try again in {min} min."
  action row (right-aligned): TextButton Cancel + TextButton Confirm
```

**Shake animation on wrong PIN:** `AnimatedVisibility` or `Animatable(0f)` offset — 380ms shake on x-axis (±10/8/6/4/2dp keyframes).

---

## State management

### Navigation

```kotlin
// MainViewModel.kt — add:
enum class Screen { HOME, SETTINGS }
var currentScreen by mutableStateOf(Screen.HOME)
    private set
fun openSettings() { currentScreen = Screen.SETTINGS }
fun navigateBack() { currentScreen = Screen.HOME }
```

`MainActivity` switches on `vm.currentScreen`, passing `onNavigateToSettings` / `onBack` callbacks.

### HomeUiState

```kotlin
data class HomeUiState(
    val rules: List<RuleUiItem> = emptyList(),
    val config: Config? = null,
    val isDeviceOwner: Boolean = false,
    val loading: Boolean = true,
    val editingRule: RuleUiItem? = null,      // non-null → sheet open
    val pinPrompt: PinPromptState? = null,
)

data class RuleUiItem(
    val packageName: String,
    val displayName: String,
    val appIcon: Drawable?,
    val isAllowed: Boolean,
    val bypassMode: BypassMode,
    val delaySeconds: Int,
    val grantedUntil: Long?,                 // epoch ms; null if no active grant
)

data class PinPromptState(
    val title: String,
    val subtitle: String,
    val attemptsRemaining: Int = PinManager.MAX_ATTEMPTS,
    val lockedForMinutes: Long = 0,
    val onConfirm: (String) -> Unit,
)
```

### HomeViewModel responsibilities

- `init`: `combine(appRuleDao.observeAll(), configDao.observe()) { rules, config → ... }` → `HomeUiState`
- Load app labels + icons in `Dispatchers.IO` via `PackageManager` (same pattern as `SetupViewModel.loadApps`)
- `fun openEditSheet(rule: RuleUiItem)` — sets `editingRule`
- `fun dismissSheet()` — clears `editingRule`
- `fun saveEdit(old: RuleUiItem, new: RuleUiItem)`:
  - if `isWeakeningEdit(old, new)` → set `pinPrompt` with `onConfirm` = apply edit after PIN verify
  - else → apply immediately via `ruleStore.upsert(...)`
- `fun onDirectToggle(rule: RuleUiItem)`:
  - toggle is `false → true` (blocked → allowed) → same PIN gate as saveEdit
  - toggle is `true → false` (allowed → blocked) → apply immediately
- `fun submitPin(pin: String)` — calls `pinPrompt.onConfirm(pin)`, handles WRONG/COOLDOWN
- `fun dismissPinDialog()` — clears `pinPrompt`

**Weakening edit predicate:**
```kotlin
fun isWeakeningEdit(old: RuleUiItem, new: RuleUiItem): Boolean {
    if (!old.isAllowed && new.isAllowed) return true
    if (new.delaySeconds < old.delaySeconds) return true
    val strength = mapOf(BypassMode.DELAY to 0, BypassMode.PIN to 1, BypassMode.DELAY_AND_PIN to 2)
    if ((strength[new.bypassMode] ?: 0) < (strength[old.bypassMode] ?: 0)) return true
    return false
}
```

### SettingsUiState

```kotlin
data class SettingsUiState(
    val config: Config? = null,
    val auditEvents: List<Event> = emptyList(),
    val auditExpanded: Boolean = false,
    val pinPrompt: PinPromptState? = null,
)
```

### SettingsViewModel responsibilities

- `init`: load `configDao.observe()` → `config`; `eventDao.recent(50)` → `auditEvents`
- `fun changeMode(newMode: BlockMode)` — gate with Removal PIN, then `configDao.upsert(config.copy(mode=newMode))`
- `fun startChangePin(scope: PinManager.Scope)`:
  1. Gate with existing PIN verification (`PinManager.verify(scope, pin)`)
  2. On success: prompt for new PIN, call `PinManager.setPin(scope, newPin)`
- `fun startRemoval()` — gate with Removal PIN, then:
  ```kotlin
  policyEnforcer.clearAllRestrictionsForRemoval()
  policyEnforcer.clearDeviceOwner()
  context.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}")))
  ```
- `fun toggleAuditLog()` — flips `auditExpanded`
- `fun submitPin(pin)` / `fun dismissPinDialog()`

---

## Animations

| Element | Spec |
|---|---|
| Bottom sheet entry | translateY(40dp→0) + alpha(0→1), 240ms `FastOutSlowInEasing` |
| Dialog entry | `scale(0.94→1) + alpha(0→1)`, 220ms |
| PIN error shake | `Animatable(0f)`, 380ms, keyframes ±10/8/6/4/2dp on x translateX |
| Protected dot pulse | `rememberInfiniteTransition`, 2s, `box-shadow` ring in Compose = `drawBehind` pulsing outer ring |
| Row press ripple | M3 default `indication = ripple()` |

---

## Wiring — MainActivity changes

```kotlin
// switch MaterialTheme → DumbifyTheme
// add: val screen by viewModel.currentScreen
when (screen) {
    Screen.HOME    -> HomeScreen(onNavigateToSettings = { vm.openSettings() })
    Screen.SETTINGS -> SettingsScreen(onBack = { vm.navigateBack() })
}
```

`MainViewModel` gains `currentScreen` + navigation functions. `onboardingComplete` check wraps this block.

---

## Icons used

| Usage | Material icon |
|---|---|
| Settings gear | `Icons.Filled.Settings` |
| Back | `Icons.AutoMirrored.Filled.ArrowBack` |
| Lock | `Icons.Filled.Lock` |
| Granted clock | `Icons.Filled.Schedule` |
| Shield (mode pill) | `Icons.Filled.Shield` |
| Protected shield | `Icons.Filled.VerifiedUser` |
| Warning | `Icons.Filled.Warning` |
| History (audit) | `Icons.Filled.History` |
| Chevron right | `Icons.AutoMirrored.Filled.KeyboardArrowRight` |
| Info | `Icons.Outlined.Info` |
| PIN / keypad | `Icons.Filled.Pin` |
