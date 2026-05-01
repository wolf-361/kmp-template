# ADR-006: CI/CD Pipeline — GitHub Actions + Fastlane

- **Status:** Accepted
- **Date:** 2026-04-30

---

## Context

The pipeline must:
1. Block PRs that fail linting or tests.
2. Automate signed release builds via Fastlane for Google Play and TestFlight.
3. Support semantic versioning via Git tags.
4. Work for both public and private repositories — **without requiring a paid GitHub plan**.

---

## Tool Decisions

### CI platform: GitHub Actions

Native GitHub integration; macOS runners for iOS builds; secrets management built-in; YAML version-controlled alongside code.

### Deployment: Fastlane

Industry standard for mobile release automation. Handles signing (`match`), building, and store uploads via reusable lanes runnable locally or from CI.

```
.github/workflows/    ← triggers, secrets injection
fastlane/
├── Appfile           ← app identifiers, Apple ID
├── Fastfile          ← lane definitions (Android + iOS)
└── Matchfile         ← iOS code signing via App Store Connect
```

---

## Branch Strategy

| Branch | Purpose | Direct push | CI trigger |
|:---|:---|:---|:---|
| `main` | Production; tagged releases | ❌ | On PR merge + tag push |
| `dev` | Integration | ❌ | On PR open/push |
| `feature/*` | Feature work | ✅ | On PR to `dev` |
| `hotfix/*` | Emergency fixes | ✅ | On PR to `main` |

---

## Workflows

### 1. `ci.yml` — PR gate

**Trigger:** Push to any open PR targeting `main` or `dev`.
**Runner:** `ubuntu-latest`

```yaml
steps:
  - Checkout + restore Gradle cache (~/.gradle, ~/.konan)
  - Setup Java 17
  - ./gradlew spotlessCheck
  - ./gradlew detekt
  - ./gradlew :shared:testDebugUnitTest
  - ./gradlew :composeApp:assembleDebug
```

### 2. `release-android.yml` — Android release

**Trigger:** Tag `v*` pushed to `main`.
**Runner:** `ubuntu-latest`

```yaml
steps:
  - Checkout
  - Setup Java 17
  - Setup Ruby + bundle install
  - bundle exec fastlane android deploy track:internal
```

**Fastfile (Android):**
```ruby
platform :android do
  lane :deploy do |options|
    gradle(
      task: "bundle",
      build_type: "Release",
      properties: {
        "android.injected.signing.store.file"     => ENV["KEYSTORE_PATH"],
        "android.injected.signing.store.password" => ENV["STORE_PASSWORD"],
        "android.injected.signing.key.alias"      => ENV["KEY_ALIAS"],
        "android.injected.signing.key.password"   => ENV["KEY_PASSWORD"],
      },
    )
    upload_to_play_store(
      track: options[:track] || "internal",
      aab: "composeApp/build/outputs/bundle/release/composeApp-release.aab",
      json_key_data: ENV["PLAY_SERVICE_ACCOUNT_JSON"],
    )
  end
end
```

**Required secrets:** `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`

### 3. `release-ios.yml` — iOS release

**Trigger:** Tag `v*` pushed to `main`.
**Runner:** `macos-latest` ← runs only on tags, never on PRs (10× more expensive).

```yaml
steps:
  - Checkout
  - Select Xcode version
  - Setup Ruby + bundle install
  - bundle exec fastlane ios deploy
```

**Fastfile (iOS):**
```ruby
platform :ios do
  lane :deploy do
    match(type: "appstore", readonly: is_ci)
    build_app(
      workspace: "iosApp/iosApp.xcworkspace",
      scheme: "iosApp",
      export_method: "app-store",
    )
    upload_to_testflight(api_key_path: "fastlane/api_key.json")
  end
end
```

**Required secrets:** `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`, `APP_STORE_CONNECT_API_KEY_BASE64`, `MATCH_PASSWORD`, `MATCH_GIT_BASIC_AUTHORIZATION`

---

## Versioning

```kotlin
// composeApp/build.gradle.kts
val versionTag = System.getenv("GITHUB_REF_NAME") ?: "0.0.0-local"
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1

android {
    defaultConfig {
        versionCode = buildNumber
        versionName = versionTag.removePrefix("v")
    }
}
```

---

## Branch Protection — GitHub Rulesets (Free Plan)

**GitHub Rulesets** (released 2023, available on all plans including Free) replace the old "Branch protection rules" system which required a paid Team plan for private repos. Configure at: **Settings → Rules → Rulesets → New branch ruleset**.

### Ruleset for `main`

| Rule | Setting |
|:---|:---|
| Target branches | `main` |
| Bypass actors | Repository admins only |
| Require a pull request | ✅ Required approvals: 1 |
| Dismiss stale reviews on push | ✅ |
| Require status checks to pass | `ci / build` (from `ci.yml`) |
| Require branches to be up to date | ✅ |
| Block force pushes | ✅ |
| Restrict deletions | ✅ |
| Require linear history | ✅ (optional — enforces squash/rebase) |

### Ruleset for `dev`

| Rule | Setting |
|:---|:---|
| Target branches | `dev` |
| Require a pull request | ✅ Required approvals: 1 (0 for solo projects) |
| Require status checks to pass | `ci / build` |
| Block force pushes | ✅ |

Both rulesets are free on public and private repositories.

---

## Cost Management

| Runner | Cost (GitHub-hosted) | When used |
|:---|:---|:---|
| `ubuntu-latest` | ~$0.008 / min | All non-iOS jobs |
| `macos-latest` | ~$0.080 / min | iOS release builds only (tag push) |

**Optimizations:**
1. iOS macOS runners trigger only on `v*` tags — never on PRs.
2. Aggressive Gradle caching: `~/.gradle`, `~/.konan` cached between runs.
3. **Self-hosted macOS runner** (recommended for active iOS teams): A Mac Mini registered as a self-hosted runner eliminates macOS runner costs entirely.

```yaml
# In release-ios.yml — switch to self-hosted when available
runs-on: [self-hosted, macOS, Xcode]
```

Register at: Settings → Actions → Runners → New self-hosted runner.

---

## Local Quality Gate

Installed automatically via a Gradle task on `preBuild`. Runs:
- `spotlessCheck` + `detekt` — always (< 5 s on warm daemon)
- `testDebugUnitTest` — only for modules with staged Kotlin files

See [ADR-013](ADR-013-code-quality.md).

---

## Rejected Alternatives

| Alternative | Reason rejected |
|:---|:---|
| Bitrise | Paid; config outside the repo |
| CircleCI | No advantage over GitHub Actions for this stack |
| Branch protection rules (old) | Requires GitHub Team for private repos |
| No branch protection | Unacceptable for production — direct pushes to main allowed |
| Xcode Cloud | iOS-only; no GitHub Actions integration |
