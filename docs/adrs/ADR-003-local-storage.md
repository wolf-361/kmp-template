# ADR-003: Local Storage Strategy

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project requires persistent local storage across two categories:

1. **Structured relational data** — domain entities (users, courses, assignments, etc.). Needs queries, joins, and offline-first sync.
2. **Scalar preferences** — app theme, language, feature flags, last-used email. Needs reactive observation.

Additionally, **secret data** (OAuth tokens) requires OS-managed secure storage and must never coexist with general application data.

A write strategy is also needed: when the user creates or modifies data, does the app write to the network first or to the local DB first?

---

## Decision Drivers

1. KMP-native — works in `commonMain` without per-call `expect/actual` boilerplate.
2. Offline-first — user can work without a connection; data survives network failures.
3. Reactive Flow emission — UI updates automatically when data changes.
4. Secure secret isolation — tokens must not be readable from general storage.
5. Predictable write behaviour — the same strategy applies everywhere unless explicitly overridden.

---

## Storage Decision Tree

```
Is it relational or needs querying?
  YES → Room KMP
  NO  → Is it a secret (token, encryption key)?
          YES → Keychain (iOS) / EncryptedSharedPreferences (Android)
          NO  → DataStore (Preferences)
```

---

## Relational data: Room KMP

Room KMP with the bundled SQLite driver provides:
- Compile-time verified SQL (`@Query`)
- Flow-returning DAOs for reactive UI
- `@Transaction` for atomic writes
- `AutoMigrations` for schema evolution

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val displayName: String,
    val syncedAt: Long,
    val pendingSync: Boolean = false,   // ← marks local-only writes awaiting sync
)
```

All DAO `suspend` functions must be called from `withContext(Dispatchers.IO)` at the repository layer. Flow-returning DAOs handle their own threading internally.

**DataStore singleton rule:** Every `DataStore` instance is registered as a Koin `single { }` — never `factory { }`. Creating two instances pointing to the same file causes data corruption on both platforms.

---

## Preferences: DataStore

DataStore is coroutine-native, Flow-based, and handles concurrent writes safely via serialised transactions. It replaces SharedPreferences.

```kotlin
object PreferenceKeys {
    val THEME    = stringPreferencesKey("theme")
    val LANGUAGE = stringPreferencesKey("language")
    val LAST_EMAIL = stringPreferencesKey("last_email")
}
```

**Why not Multiplatform Settings?** Its synchronous model delegates to SharedPreferences (Android) and NSUserDefaults (iOS), both of which have concurrent-write race conditions. DataStore's transactional model fits the reactive architecture.

---

## Secrets: Platform secure storage

| Platform | Mechanism |
|:---|:---|
| iOS | Keychain Services (`expect/actual`) |
| Android | EncryptedSharedPreferences (Jetpack Security) (`expect/actual`) |

```kotlin
interface TokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun clearTokens()
}
```

---

## Write Strategy

### Default: Local-first (offline-capable writes)

All user-initiated create, update, and delete operations write to the local Room database first, then enqueue a background sync to the network. The UI responds immediately without waiting for network confirmation.

```
User action
    → Write to Room (marks entity pendingSync = true)
    → Update UI (Flow emits new state instantly)
    → SyncScheduler enqueues SyncTask
        → NetworkClient syncs in background
        → On success: clear pendingSync flag
        → On conflict: apply server-wins or last-write-wins policy (per-entity decision)
```

This pattern means the app is fully functional offline. When connectivity returns, `SyncScheduler` drains the pending queue.

### Exception: Network-first for auth and identity operations

Authentication, registration, password reset, and token refresh always go to the network **before** any local write. The reasons:

- Credentials must be validated server-side before being trusted locally.
- A session token that has never been verified by the server is useless.
- Offline auth is a security anti-pattern.

```
Login action
    → Network call (POST /auth/login)
    → On success: persist token via TokenProvider, write user entity to Room
    → On failure: return AppResult.Failure (never touch local DB)
```

### Summary

| Operation type | Write order |
|:---|:---|
| Create / Update / Delete (domain data) | DB first → background network sync |
| Login / Register / Auth token refresh | Network first → DB on success |
| Logout | Clear tokens only (see logout contract below) |

---

## Logout Contract

```
logout() MUST:
  ✅  tokenProvider.clearTokens()       — wipe access + refresh token
  ✅  retain last_email in DataStore    — auto-fill on next login
  ✅  retain user preferences           — theme, language survive logout
  ❌  NEVER call dataStore.edit { it.clear() }
  ❌  NEVER call roomDb.clearAllTables()
```

`clearAll()` on any store is only permitted from a **"Delete account"** flow, after explicit user confirmation with a destructive-action dialog.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Multiplatform Settings as primary | Synchronous; SharedPreferences race conditions |
| SQLDelight instead of Room | Valid but higher onboarding cost; can be revisited via a new ADR |
| Tokens in DataStore | Not encrypted at rest |
| Network-first for all writes | Requires connectivity for every mutation; breaks offline use cases |
| Local-first for auth | Security anti-pattern; unverified credentials must not be persisted |
