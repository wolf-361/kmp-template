# ADR-012: Swift Interoperability — SKIE

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

KMP compiles shared code to an Objective-C framework for iOS consumption. The default Kotlin-to-Objective-C bridge has significant limitations:

- `StateFlow<T>` and `SharedFlow<T>` are not natively observable in Swift.
- Sealed interfaces and sealed classes are exposed as flat Objective-C classes, losing exhaustive `switch` matching.
- `suspend` functions require manual callback bridging.
- Generic types are erased.

These limitations make it impractical to consume shared ViewModels from SwiftUI without a bridging layer.

---

## Decision Drivers

1. Shared ViewModels must be directly observable in SwiftUI (`@StateObject`, `.onReceive`).
2. `StateFlow<S>` must map to a SwiftUI `@Published` or Combine `Publisher`.
3. `SharedFlow<E>` for effects must be subscribable without Objective-C wrappers.
4. Sealed interfaces must produce exhaustive `switch` cases in Swift.
5. Minimal boilerplate in iOS Swift code.
6. Actively maintained; compatible with Kotlin 2.3.

---

## Considered Options

### Option A: Manual Objective-C bridging (no library)

Write `expect/actual` wrappers that convert `StateFlow` to iOS `Publisher` manually.

**Pros:** No extra dependency.

**Cons:** Massive boilerplate; fragile; must be updated for every new ViewModel; type erasure remains.

---

### Option B: KMP-NativeCoroutines (rickclephas)

Generates Swift-friendly wrappers for coroutine types.

**Pros:** Solid coroutine support; `AsyncStream` and Combine support.

**Cons:** Requires annotating every `Flow` property; does not fix sealed interface exhaustiveness; adds annotation processor to the build.

---

### Option C: SKIE (Touchlab) — **CHOSEN**

SKIE (Swift Kotlin Interface Enhancer) is a Gradle plugin that post-processes the Kotlin framework to produce idiomatic Swift interfaces automatically, with zero annotations required.

**Pros:**
- `StateFlow<S>` automatically becomes a `SkieSwiftStateFlow<S>` with `.collect {}` in Swift.
- Sealed interfaces become exhaustive Swift `enum` equivalents.
- `suspend` functions become Swift `async` functions (structured concurrency).
- No annotations on Kotlin side — all transformations are automatic.
- Maintained by Touchlab (same team as Kermit, already in the stack).
- Compatible with Kotlin 2.3 and Xcode 15+.

**Cons:**
- Adds compile time to the iOS framework build.
- Touchlab controls the release cadence; must track Kotlin version compatibility.

---

## Decision Outcome

**Use SKIE 0.10.x.**

### SwiftUI ViewModel consumption

```swift
// No manual wrapper needed
struct LoginView: View {
    @StateObject private var viewModel: AuthViewModel = KoinDI.shared.get()

    var body: some View {
        let state = viewModel.state.value

        VStack {
            if state.isLoading { ProgressView() }
            TextField("Email", text: Binding(
                get: { state.email },
                set: { viewModel.onAction(AuthAction.OnEmailChanged(value: $0)) }
            ))
        }
        .task {
            for await effect in viewModel.effects {
                switch effect {
                case is AuthEffect.NavigateToDashboard:
                    path.append(TopLevelGraph.Dashboard())
                default: break
                }
            }
        }
    }
}
```

### Sealed interface exhaustiveness (Swift)

Without SKIE, sealed interfaces compile to unchecked Objective-C class hierarchies. SKIE transforms them:

```swift
// With SKIE — exhaustive switch
switch (effect) {
case is AuthEffect.NavigateToDashboard: …
case let show as AuthEffect.ShowResetDialog: … show.email …
// Compiler error if a case is missing
}
```

### Koin on iOS (via SKIE)

```swift
// iosApp — KoinInit.swift
func startKoin() {
    KoinInitKt.initKoin { koin in
        koin.module {
            // iOS-specific bindings (e.g., BiometricProvider)
        }
    }
}
```

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Manual Objective-C bridging | Massive boilerplate; fragile; no sealed exhaustiveness |
| KMP-NativeCoroutines | Annotation-based; no sealed interface fix |
| No bridging (raw Objective-C API) | `StateFlow` not observable; sealed hierarchy unusable in Swift |
