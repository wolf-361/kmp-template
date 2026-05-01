# ADR-014: Build Toolchain — Kotlin, AGP, Compose Multiplatform, and Core Libraries

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The build toolchain defines the foundation that every other decision sits on top of. Versions must be mutually compatible, current enough to receive security patches and language features, and stable enough to not block the production release cycle.

This ADR documents the rationale for each version pin and the constraints they introduce.

---

## Decisions

### Kotlin 2.3.20

**Why 2.3?**
- K2 compiler is stable and production-ready; delivers faster compilation and better IDE analysis.
- `data object` (used by navigation Destinations) requires Kotlin 1.9+.
- Context parameters (optional) available from 2.x.
- `@Serializable` improvements and sealed interface support are stable.

**KSP versioning note:** Starting with KSP 2.3.x, KSP switched from the `{kotlin_version}-{ksp_patch}` format (e.g. `2.0.21-1.0.25`) to standalone semver (e.g. `2.3.7`). The two version numbers are no longer coupled — check the [KSP release notes](https://github.com/google/ksp/releases) when upgrading Kotlin.

**SKIE constraint:** Before bumping Kotlin, verify SKIE support at [touchlab/SKIE releases](https://github.com/touchlab/SKIE/releases). New Kotlin versions are typically supported within a few days of release. Kotlin 2.3.20 is the current pin because 2.3.21 is not yet supported by SKIE 0.10.11.

---

### AGP 9.1.x

**Why 9.1?**
- Latest AGP version supported by the current Android Studio release (AS caps at AGP 9.1.1; 9.2.x causes IDE sync degradation).
- Full Kotlin 2.x support.
- Mandatory `namespace` declaration enforces module identity.
- Removes deprecated `packagingOptions` and `flavorDimensions` array DSL — forces clean build scripts.
- Better support for Compose Multiplatform 1.9 resource pipeline.

**Upgrade note:** When Android Studio ships support for AGP 9.2.x, bump here, in `libs.versions.toml`, and update the compatibility matrix below. AGP 9.2.x requires Gradle 9.4.1+; 9.1.x works with Gradle 9.0+.

**Hard requirements introduced:**
- Gradle **9.4.1** or higher (`gradle/wrapper/gradle-wrapper.properties`) — already in use; can be relaxed to 9.0+ if the wrapper is downgraded.
- `compileSdk` **37** minimum.
- Java **17** source and target compatibility in `compileOptions`.
- `namespace` required in every `build.gradle.kts` that applies `androidApplication` or `androidLibrary`.
- KMP library modules must use `com.android.kotlin.multiplatform.library` instead of `com.android.library` — the two plugins are mutually exclusive with `kotlin.multiplatform` since AGP 9.0 (see [developer.android.com/kotlin/multiplatform/plugin](https://developer.android.com/kotlin/multiplatform/plugin)).
- `org.jetbrains.kotlin.android` plugin is no longer required — AGP 9.0+ includes Kotlin support built-in.
- The top-level `android { }` block is gone in KMP library modules. `namespace`, `compileSdk`, and `minSdk` are set directly on `androidTarget { }` inside `kotlin { }`. Java `compileOptions` is replaced by `compilerOptions { jvmTarget }` on the same block.

---

### Compose Multiplatform 1.10.x

**Why CMP 1.10?**
- Ships Compose Resources (used in ADR-001) as stable.
- `lifecycle-viewmodel-compose` is stable for KMP.
- iOS interop via SKIE 0.10.x is tested against CMP 1.10.

**Constraint:** The `composeMultiplatform` plugin version governs the Compose compiler; do not mix the CMP plugin version with the standalone `composeCompiler` plugin version.

**material3 versioning:** `org.jetbrains.compose.material3:material3` has its own independent release cadence and does **not** share the CMP plugin version. Pin it separately in the version catalog (currently `1.9.0`). All other CMP library artifacts (`runtime`, `foundation`, `ui`, `components-resources`) do track the CMP plugin version.

**Deprecated compose accessors:** The `compose.runtime`, `compose.material3`, etc. shorthand accessors are removed in CMP 1.10. Use explicit version catalog entries with the coordinates above.

---

### kotlinx-datetime 0.7.x

**Why kotlinx-datetime?**
`java.time.*` is not available in `commonMain`. `kotlinx-datetime` is the KMP standard replacement for `LocalDate`, `LocalDateTime`, `Instant`, `TimeZone`, and `DateTimePeriod`.

**Rule:** `java.time.*`, `java.util.Date`, and `java.util.Calendar` are forbidden in `shared/commonMain`. A Detekt rule enforces this.

---

### kotlinx-coroutines 1.10.x

Coroutines and `Flow` are the sole async mechanism. No RxJava, no callbacks in shared code. All network and database operations are `suspend` or return `Flow<T>`.

---

### kotlinx-serialization 1.10.x

Used exclusively for JSON (de)serialisation of Ktor DTOs and `@Serializable` navigation Destinations. `Gson` and `Moshi` are not KMP-compatible and are forbidden in shared code.

---

## Version Compatibility Matrix

| Library | Version | Compatible with |
|:---|:---|:---|
| Kotlin | 2.3.20 | KSP 2.3.7, SKIE 0.10.11, Compose Compiler 2.3.20 |
| AGP | 9.1.x | Gradle 9.0+ (wrapper currently at 9.4.1) |
| Compose Multiplatform | 1.10.x | Kotlin 2.3, SKIE 0.10.x |
| Room KMP | 2.8.x | KSP 2.3.7 |
| Koin | 4.2.x | Kotlin 2.x |
| Koin Annotations | 4.2.x | Koin 4.2.x (versions are now aligned) |
| Ktor | 3.4.x | Kotlin 2.x, coroutines 1.10.x |
| SKIE | 0.10.x | Kotlin 2.3, CMP 1.10 |
| Mokkery | 3.x | Kotlin 2.3 |

---

## Upgrade Policy

1. **Kotlin minor bumps** (2.3 → 2.4): upgrade KSP (check [releases](https://github.com/google/ksp/releases) — now independent semver), Mokkery, and SKIE in the same PR.
2. **AGP bumps**: check Gradle wrapper version requirement before upgrading; test on both debug and release build variants.
3. **CMP bumps**: test SKIE compatibility first; check Compose Resources API changes.
4. **Dependency bumps not in this ADR**: Renovate or Dependabot PRs are acceptable; must pass the full CI pipeline.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Kotlin 1.x | K2 compiler improvements; `data object` support required |
| AGP 8.x | Deprecated DSL still present; miss 9.x build improvements |
| CMP 1.8.x | Compose Resources not yet stable |
| RxJava | Not KMP-compatible; superseded by coroutines |
| Gson / Moshi | JVM-only; not usable in `commonMain` |
| java.time | Not available in `commonMain` |
