# Data Model

## Room DB — `DumbifyDb`

### `config` (singleton row, id = 0)

```kotlin
enum class BlockMode { ALLOWLIST, DENYLIST }
enum class UserRole  { SELF, MANAGED }

@Entity(tableName = "config")
data class Config(
    @PrimaryKey val id: Int = 0,
    val mode: BlockMode,
    val userRole: UserRole,
    val customMessage: String,
    val launcherEnabled: Boolean,
    val onboardingComplete: Boolean
)
```

### `app_rules`

```kotlin
enum class BypassMode { DELAY, PIN, DELAY_AND_PIN }

@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val isAllowed: Boolean,
    val bypassMode: BypassMode,
    val delaySeconds: Int,            // 0 if PIN-only
    val grantedUntil: Long?           // epoch ms; non-null = currently unblocked
)
```

Evaluation: see `01-architecture.md` `isBlocked()`.

### `events` (audit log, local only in v1)

```kotlin
enum class EventType {
    BLOCK_HIT, UNBLOCK_REQUEST, UNBLOCK_GRANTED, UNBLOCK_DENIED,
    RULE_EDIT, PIN_FAIL, MODE_CHANGE, REMOVAL_ATTEMPT
}

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: EventType,
    val packageName: String?,
    val detail: String?
)
```

## EncryptedSharedPreferences — `dumbify_secure`

Two-PIN model. See `03-user-flows.md` and `04-tamper-resistance.md`.

| Key | Type | Required | Purpose |
|---|---|---|---|
| `removal_pin_hash` | String (Argon2id encoded) | yes | gates full app removal |
| `removal_pin_salt` | ByteArray | yes | salt for above |
| `bypass_pin_hash` | String? | only if any rule uses PIN bypass | gates per-app unblock |
| `bypass_pin_salt` | ByteArray? | conditional | |
| `pin_fail_count` | Int | yes (default 0) | brute-force cooldown counter |
| `pin_cooldown_until` | Long | yes (default 0) | epoch ms — refuse PIN entry before this |

PIN hashing via `Argon2id`, parameters: memory 64 MB, iterations 3, parallelism 2 (tune for low-end device boot time).

## Migrations

`schema_version = 1` for v1. Schema export enabled (`room.schemaLocation`) for future migrations.

## DAOs

Standard Room DAOs in `data/dao/`:
- `ConfigDao` — `getConfig()`, `upsert(Config)`
- `AppRuleDao` — `all()`, `byPkg(pkg)`, `upsert(rule)`, `delete(pkg)`, `setGrantedUntil(pkg, until)`
- `EventDao` — `insert(event)`, `recent(limit)`, `all()` (for future export)
