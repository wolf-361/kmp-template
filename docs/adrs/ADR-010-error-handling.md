# ADR-010: Error Handling Strategy — AppResult + handle DSL

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs a consistent, composable error-handling strategy that:
- Makes all failure paths explicit at the type level — no unchecked exceptions escaping the data layer.
- Allows feature-specific errors without forcing callers to handle every possible subtype.
- Maps cleanly onto the MVI pattern (errors land in `UiState`, not thrown into the coroutine scope).
- Is testable — success and failure paths are equally easy to assert.

---

## Decision Drivers

1. Explicit failure types — `AppResult<T>` makes the compiler enforce handling.
2. Feature-specific errors — each feature defines its own sealed error interface.
3. Generic fallback — unknown/network errors are handled without exhaustive `when`.
4. No exception leakage — `try/catch` boundaries are at the repository, not in ViewModels.
5. Readable DSL — error handling reads like prose, not nested `if`/`when` blocks.

---

## Considered Options

### Option A: Kotlin `Result<T>` (stdlib)

**Pros:** No extra code; standard library.

**Cons:** Only `Success` and `Failure(Throwable)` — no typed error hierarchy. Feature-specific error types require casting. No DSL support.

---

### Option B: Arrow's `Either<E, A>`

**Pros:** Fully typed; rich functional operators.

**Cons:** Arrow is a large dependency; functional style increases onboarding barrier for teams not familiar with Haskell-style error handling.

---

### Option C: Custom `AppResult<T>` + `handle` DSL — **CHOSEN**

A minimal sealed class with a typed failure and a Kotlin DSL for ergonomic handling.

---

## Decision Outcome

**Use a custom `AppResult<T>` + `handle` DSL.**

### AppResult

```kotlin
// core/domain/models/AppResult.kt
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}
```

### Error hierarchy

```kotlin
// core/domain/models/AppError.kt
sealed interface AppError

// core/domain/models/CoreError.kt
sealed interface CoreError : AppError {
    sealed interface Network : CoreError {
        data object Timeout : Network
        data object NoConnection : Network
        data class BackendPayload(val code: Int, val message: String) : Network
    }
    data object DataCorruption : CoreError
    data object Database : CoreError
    data object Unauthorized : CoreError
}

// features/auth/domain/models/errors/AuthError.kt
sealed interface AuthError : AppError {
    data object InvalidCredentials : AuthError
    data object EmailAlreadyExists : AuthError
    data object WeakPassword : AuthError
}
```

### handle DSL

```kotlin
// core/domain/extensions/AppResultExtensions.kt
class AppResultScope<T>(private val result: AppResult<T>) {
    private var successBlock: ((T) -> Unit)? = null
    private val failureBlocks = mutableListOf<Pair<KClass<*>, (AppError) -> Unit>>()
    private var catchBlock: ((AppError) -> Unit)? = null

    fun success(block: (T) -> Unit) { successBlock = block }

    inline fun <reified E : AppError> failure(noinline block: (E) -> Unit) {
        failureBlocks += E::class to { error -> if (error is E) block(error) }
    }

    fun catch(block: (AppError) -> Unit) { catchBlock = block }

    fun execute() {
        when (result) {
            is AppResult.Success -> successBlock?.invoke(result.data)
            is AppResult.Failure -> {
                val handled = failureBlocks.any { (_, handler) ->
                    runCatching { handler(result.error) }.isSuccess
                }
                if (!handled) catchBlock?.invoke(result.error)
            }
        }
    }
}

fun <T> AppResult<T>.handle(block: AppResultScope<T>.() -> Unit) {
    AppResultScope(this).apply(block).execute()
}
```

### Usage in ViewModel

```kotlin
loginUseCase(email, password).handle {
    success { user ->
        navigateTo(TopLevelGraph.Dashboard)   // direct call — no XEffect for navigation
    }
    failure<AuthError.InvalidCredentials> {
        updateState { it.copy(emailError = S.err_invalid_credentials) }
    }
    failure<CoreError.Network.NoConnection> {
        updateState { it.copy(globalError = S.err_no_connection) }
    }
    catch { error ->
        updateState { it.copy(globalError = error.asString()) }
    }
}
```

### Repository boundary contract

Every `RepositoryImpl` function must:
1. Wrap all network and DB calls in `try/catch`.
2. Map exceptions to typed `CoreError` variants.
3. Return `AppResult<T>` — never throw.

```kotlin
override suspend fun login(email: String, password: String): AppResult<User> =
    try {
        val response = networkClient.post<AuthResponse>("/auth/login", LoginRequest(email, password))
        AppResult.Success(response.toDomain())
    } catch (e: ConnectTimeoutException) {
        AppResult.Failure(CoreError.Network.Timeout)
    } catch (e: UnauthorizedException) {
        AppResult.Failure(AuthError.InvalidCredentials)
    } catch (e: Exception) {
        AppResult.Failure(CoreError.Network.NoConnection)
    }
```

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Kotlin `Result<T>` | No typed error hierarchy; no DSL |
| Arrow `Either<E, A>` | Large dependency; functional barrier for most teams |
| Raw exceptions in ViewModels | Unhandled exceptions crash the app; no typed hierarchy |
| `sealed class` with `onSuccess`/`onFailure` only | No generic fallback (`catch`); callers must handle every type |
