# ADR-004: Gradle Module Structure

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

KMP projects can be organised at two granularities:

1. **Flat (2-module):** `shared` + `composeApp`. Feature boundaries are packages.
2. **Multi-module:** Each feature is a Gradle module (`:feature:auth`, `:feature:course`, etc.). Enforces hard compile-time boundaries.

---

## Decision Drivers

1. Build speed — incremental compilation is more effective across Gradle modules.
2. Scalability — enforced API boundaries prevent accidental cross-feature coupling.
3. Onboarding cost — extra `build.gradle.kts` files per feature increase setup complexity.
4. Readability — a small project suffers from multi-module overhead.
5. Template usability — the template must be easy to fork and extend.

---

## Considered Options

### Option A: Flat 2-module — **CHOSEN for initial template**

```
root/
├── shared/       ← ALL shared KMP code
└── composeApp/   ← Android Compose UI
```

Feature boundaries enforced by package structure. Gradle build is simple; immediate productivity for new developers.

**Pros:**
- Single `build.gradle.kts` per platform layer.
- Easy to navigate and fork.

**Cons:**
- No compile-time enforcement of cross-feature isolation.
- As the project grows, the entire `shared` module recompiles when any file changes.

**Upgrade trigger:** project exceeds ~15 features OR full `shared` build time exceeds 3 minutes on CI.

---

### Option B: Multi-module (feature Gradle modules)

```
root/
├── core/
│   ├── network/
│   ├── database/
│   └── di/
├── feature/
│   ├── auth/
│   └── dashboard/
├── composeApp/
└── iosApp/
```

**Pros:** Hard API boundaries; better incremental compilation; scales to 50+ features.

**Cons:** Every feature adds a `build.gradle.kts`, KSP config, and DI wiring upfront.

---

## Decision Outcome

**Start with Option A.** The architecture is designed so migration to Option B is mechanical:
- Every feature is already isolated to `features/{name}/` with zero cross-feature imports.
- DI is per-feature via `FeatureModule.kt`.
- When ready to split, each `features/{name}/` becomes a `:feature:{name}` Gradle module with minimal refactoring.

### Module graph (current)

```
:composeApp ──depends on──▶ :shared
:iosApp (Xcode) ──consumes──▶ :shared (framework via SKIE)
```

### Upgrade path

1. Create `:core:network`, `:core:database`, `:core:di`, `:core:presentation` from `shared/core/`.
2. Create `:feature:auth`, `:feature:course`, … from `shared/features/`.
3. Move `commonMain` sources; adjust `build.gradle.kts` per subproject.
4. Aggregate Koin modules from each Gradle subproject in `KoinInit`.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Multi-module from day one | Premature for a template; high onboarding cost |
| Single monolithic module | Loses platform boundary clarity |
