# ADR-002: Logging Framework — Kermit

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs a KMP-compatible logging library that:
- Works identically in `commonMain` with no `expect/actual` boilerplate per call site.
- Routes to the platform's native log sink (Logcat on Android, OSLog on iOS).
- Integrates with crash-reporting (Firebase Crashlytics) in a future step.
- Produces console output in tests — visible in the test report, never silenced.

---

## Decision Drivers

1. True KMP — single call site in `commonMain`.
2. Multiple severity levels (Verbose, Debug, Info, Warn, Error).
3. Pluggable log writers — Logcat, OSLog, Crashlytics, and a plain console writer for tests.
4. Tests must always show log output — silent test runs hide diagnostic information.
5. Actively maintained; compatible with Kotlin 2.3.

---

## Considered Options

### Option A: Napier (Aakira)

**Pros:** Simple, minimal API.
**Cons:** Lower maintenance activity; no built-in Crashlytics integration; limited writer ecosystem.

---

### Option B: Kermit (Touchlab) — **CHOSEN**

Pluggable KMP logging library from Touchlab (same team as SKIE).

**Pros:** Pluggable `LogWriter` system; lazy message lambdas; Crashlytics integration (`kermit-crashlytics`); actively maintained alongside SKIE.
**Cons:** Slightly heavier than Napier.

---

## Decision Outcome

**Use Kermit 2.x.**

### AppLogger interface

```kotlin
// core/logging/AppLogger.kt
interface AppLogger {
    fun v(tag: String, message: () -> String)
    fun d(tag: String, message: () -> String)
    fun i(tag: String, message: () -> String)
    fun w(tag: String, message: () -> String)
    fun e(tag: String, throwable: Throwable? = null, message: () -> String)
}
```

### Production implementation

```kotlin
// core/logging/AppLoggerImpl.kt
class AppLoggerImpl(private val kermit: Logger) : AppLogger {
    override fun v(tag: String, message: () -> String) = kermit.v(message, tag)
    override fun d(tag: String, message: () -> String) = kermit.d(message, tag)
    override fun i(tag: String, message: () -> String) = kermit.i(message, tag)
    override fun w(tag: String, message: () -> String) = kermit.w(message, tag)
    override fun e(tag: String, throwable: Throwable?, message: () -> String) =
        kermit.e(throwable, message, tag)
}
```

### Platform writers (production)

```kotlin
// androidMain
fun buildLogger(): Logger = Logger(
    config = StaticConfig(logWriterList = listOf(LogcatWriter())),
    tag = "App",
)

// iosMain
fun buildLogger(): Logger = Logger(
    config = StaticConfig(logWriterList = listOf(OSLogWriter())),
    tag = "App",
)
```

### Test logger — console output, never silent

Tests use a `PrintLogger` that writes to stdout. This ensures log output is always visible in the test report, making failures easier to diagnose.

```kotlin
// core/testing/PrintLogger.kt  (in commonTest source set)
class PrintLogger : AppLogger {
    override fun v(tag: String, message: () -> String) = println("V/$tag: ${message()}")
    override fun d(tag: String, message: () -> String) = println("D/$tag: ${message()}")
    override fun i(tag: String, message: () -> String) = println("I/$tag: ${message()}")
    override fun w(tag: String, message: () -> String) = println("W/$tag: ${message()}")
    override fun e(tag: String, throwable: Throwable?, message: () -> String) {
        println("E/$tag: ${message()}")
        throwable?.printStackTrace()
    }
}
```

`PrintLogger` is injected via the Koin test module:

```kotlin
// core/testing/TestKoinModule.kt
val testModule = module {
    single<AppLogger> { PrintLogger() }
    single<StringProvider> { FakeStringProvider() }
}
```

**Rule:** Never use `NoOpLogger` or swallow logs in tests. Visible logs are a feature, not noise.

### Tag extension

```kotlin
// core/logging/TagExtensions.kt
inline val Any.TAG: String get() = this::class.simpleName ?: "Unknown"
```

Usage: `logger.d(TAG) { "User logged in: $userId" }`

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Napier | Lower maintenance; no Crashlytics; fewer writers |
| Timber | Android-only |
| NoOpLogger in tests | Silences diagnostic output; hides root causes in failing tests |
