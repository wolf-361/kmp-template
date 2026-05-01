# ADR-011: Testing Stack

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs a consistent testing stack that:
- Runs in `commonTest` (KMP) — tests share the same code as the production targets.
- Provides expressive assertions readable as natural language.
- Supports mocking without annotation processors (KSP/KAPT are unreliable in `commonTest`).
- Tests Kotlin `Flow` emissions, cancellation, and timing without real coroutine delays.
- Verifies the Koin DI graph at test time.

---

## Decision Drivers

1. KMP-compatible — all libraries run in `commonTest` on JVM and iOS simulator.
2. Annotation-free mocking — no KSP/KAPT in `commonTest`.
3. Flow testing primitives — `Flow<T>` is used throughout the architecture.
4. Readable assertions — test failures must be self-describing.
5. DI graph validation — must catch missing bindings before deployment.

---

## Library Decisions

### Mocking: Mokkery — **CHOSEN over Mockito / MockK**

Mokkery is a Kotlin Multiplatform mocking library that generates mocks via a Gradle plugin (no KAPT/KSP at test time).

```kotlin
val useCase = mock<LoginUseCase>()
every { useCase(any(), any()) } returns AppResult.Success(fakeUser)
```

**Why not Mockito?** JVM-only; incompatible with `commonTest`.

**Why not MockK?** MockK relies on JVM reflection and has limited KMP support; mocks in `commonTest` require workarounds.

---

### Assertions: Kotest Assertions — **CHOSEN over kotlin.test**

Kotest Assertions provides a rich, infix-style assertion API:

```kotlin
result shouldBe AppResult.Success(fakeUser)
list shouldHaveSize 3
state.isLoading shouldBe false
exception.message shouldContain "401"
```

`kotlin.test` assertions are valid but produce less descriptive failure messages.

---

### Flow testing: Turbine — **CHOSEN**

Turbine by Cash App provides `Flow.test {}` with explicit item awaiting:

```kotlin
viewModel.state.test {
    val initial = awaitItem()
    initial.isLoading shouldBe false

    viewModel.onAction(AuthAction.OnSubmit)

    val loading = awaitItem()
    loading.isLoading shouldBe true

    val done = awaitItem()
    done.isLoading shouldBe false
    cancelAndIgnoreRemainingEvents()
}
```

Without Turbine, Flow collection in tests requires manual coroutine coordination and `Channel` plumbing.

---

### Coroutines: kotlinx-coroutines-test — **CHOSEN**

`runTest` and `UnconfinedTestDispatcher` allow time control and fast-forward without real delays.

```kotlin
@Test
fun `should complete within timeout`() = runTest {
    advanceTimeBy(5_000)
    // assertions
}
```

---

### DI validation: Koin Test — **CHOSEN**

`checkModules {}` validates the full Koin graph at test time, catching missing bindings before runtime.

```kotlin
class KoinModulesTest : KoinTest {
    @Test
    fun `koin graph is valid`() = checkModules {
        modules(appModules)
    }
}
```

---

### Network mocks: Ktor MockEngine — **CHOSEN**

`MockEngine` replaces the real Ktor engine in tests, allowing deterministic HTTP responses without a server.

```kotlin
val engine = MockEngine { request ->
    respond(
        content = """{"token":"abc123"}""",
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
```

---

### Database: Room In-Memory — **CHOSEN**

`inMemoryDatabaseBuilder` creates a transient Room database for each test, isolating state between test cases.

```kotlin
val db = inMemoryDatabaseBuilder<AppDatabase>(context, AppDatabase::class).build()
```

---

## Test Naming Convention

```
`should [expected behavior] when [condition]`
```

Examples:
- `` `should emit loading state when submit is triggered` ``
- `` `should return failure when network is unavailable` ``
- `` `should retain last email when logout is called` ``

---

## AAA Pattern — one Act per test

```kotlin
@Test
fun `should navigate to dashboard when login succeeds`() = runTest {
    // Arrange
    every { loginUseCase(any(), any()) } returns AppResult.Success(fakeUser)
    val viewModel = createViewModel()

    // Act (exactly ONE action)
    viewModel.onAction(AuthAction.OnSubmit)

    // Assert
    viewModel.effects.test {
        awaitItem() shouldBe AuthEffect.NavigateToDashboard
    }
}
```

If two actions are needed: write two tests.

---

## Source Set Mapping

| Test type | Source set | Runs on |
|:---|:---|:---|
| Business logic, ViewModels, UseCases | `commonTest` | JVM + iOS simulator |
| Android-specific platform code | `androidUnitTest` | JVM (Robolectric if needed) |
| iOS-specific platform code | Xcode Unit Tests | iOS simulator |

---

## Rejected Alternatives

| Library | Reason rejected |
|:---|:---|
| Mockito | JVM-only |
| MockK | Reflection-based; limited KMP `commonTest` support |
| kotlin.test assertions | Less descriptive failure messages |
| Manual Flow collection | Error-prone; requires Channel/Job plumbing |
| SQLite in-memory (raw) | Room abstractions not available; misses DAO contract testing |
