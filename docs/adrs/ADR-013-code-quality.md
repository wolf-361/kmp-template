# ADR-013: Code Quality Toolchain — Detekt + Spotless + Git Hooks

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The project needs automated code quality enforcement that is fast, reliable, and cannot be silently skipped. Requirements:

- Formatting enforced automatically — no style debates in reviews.
- Static analysis catches Kotlin anti-patterns before merge.
- Pre-commit hooks are installed automatically and reliably.
- Pre-commit checks run only on staged Kotlin files — fast enough that developers don't bypass them.
- The full test suite runs only on CI, not in the pre-commit hook.

---

## Tool Decisions

### Formatting: Spotless with KtLint

```kotlin
// root build.gradle.kts
spotless {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        target("**/*.kt")
        targetExclude("**/build/**", "**/.gradle/**")
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
        target("**/*.kts")
    }
}
```

Developers: `./gradlew spotlessApply` to auto-fix.
CI: `./gradlew spotlessCheck` to block non-compliant PRs.

---

### Static analysis: Detekt

```kotlin
// shared/build.gradle.kts
detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}
```

Key rule sets enabled: `complexity`, `coroutines`, `naming`, `performance`, `style`.

Custom rule added to `detekt.yml`:
```yaml
ForbiddenImport:
  active: true
  imports:
    - value: 'java.time.*'
      reason: 'Use kotlinx.datetime instead (not available in commonMain)'
    - value: 'java.util.Date'
      reason: 'Use kotlinx.datetime instead'
```

---

## Pre-commit Hook

### Installation: Gradle task (no external plugin needed)

```kotlin
// root build.gradle.kts
tasks.register("installGitHooks") {
    group = "git-hooks"
    description = "Installs the pre-commit hook into .git/hooks"
    doLast {
        val hooksDir = rootProject.file(".git/hooks")
        val source   = rootProject.file("scripts/pre-commit.sh")
        val target   = File(hooksDir, "pre-commit")
        source.copyTo(target, overwrite = true)
        target.setExecutable(true)
        println("Git pre-commit hook installed.")
    }
}

// Auto-install on every build — no manual step required after cloning
tasks.named("preBuild") { dependsOn("installGitHooks") }
// Also hook into Gradle's build lifecycle for non-Android modules
gradle.projectsEvaluated {
    rootProject.tasks.findByName("build")?.dependsOn("installGitHooks")
}
```

The hook runs automatically after `./gradlew build` or any Android Studio sync that triggers `preBuild`. No separate setup step required after cloning.

---

### Pre-commit script: staged files only

The script inspects the Git staging area. It runs tests **only when Kotlin files are staged**, and only for the **modules that contain staged changes**.

```bash
#!/bin/bash
# scripts/pre-commit.sh
set -e

echo "▶ Running pre-commit checks..."

# Collect staged Kotlin files (Added, Copied, Modified only — not Deleted)
STAGED_KOTLIN=$(git diff --cached --name-only --diff-filter=ACM | grep "\.kt$" || true)

# Always run formatting and linting (fast — < 5 seconds on warm daemon)
./gradlew spotlessCheck detekt --daemon --quiet

if [ -n "$STAGED_KOTLIN" ]; then
    echo "  Kotlin files staged — running unit tests for affected modules..."

    SHARED_CHANGED=$(echo "$STAGED_KOTLIN" | grep "^shared/" || true)
    COMPOSE_CHANGED=$(echo "$STAGED_KOTLIN" | grep "^composeApp/" || true)

    if [ -n "$SHARED_CHANGED" ]; then
        ./gradlew :shared:testDebugUnitTest --daemon --quiet
    fi
    if [ -n "$COMPOSE_CHANGED" ]; then
        ./gradlew :composeApp:testDebugUnitTest --daemon --quiet
    fi
else
    echo "  No Kotlin files staged — skipping unit tests."
fi

echo "✔ Pre-commit checks passed."
```

**Why staged files only?**
- A commit that touches only markdown or config files should not trigger a 60-second test run.
- Module-scoped test execution means a change in `features/auth/` does not rebuild `features/course/`.
- The full test suite always runs on CI (no filtering) — the pre-commit is a fast local gate, not a replacement for CI.

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| External git-hooks plugin | Adds a dependency for something a 20-line Gradle task handles reliably |
| Pre-commit hook runs full test suite always | Developers use `--no-verify` within days; the hook defeats itself |
| Android Studio formatting only | Per-developer; not enforced at commit time |
| SonarQube | External server; overkill for a template |
| Diktat | Overlaps with Detekt; lower adoption |
