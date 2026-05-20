#!/bin/bash
# Pre-commit hook — installed automatically via Gradle's installGitHooks task.
# Only processes staged Kotlin files; runs tests only for affected modules.
# Mirrors the CI checks in .github/workflows/ci.yml.

set -e

STAGED_KT=$(git diff --cached --name-only --diff-filter=ACM | grep "\.kt$" || true)

if [ -z "$STAGED_KT" ]; then
  exit 0
fi

echo "pre-commit: running quality checks on staged Kotlin files..."

# Always run: format check + full detekt (fast on warm daemon, ~3–5 s)
./gradlew spotlessCheck \
  :shared:detekt \
  :shared:detektDomainImports \
  :shared:detektPresentationImports \
  --daemon --quiet

# Unit tests — only for modules with staged changes
if echo "$STAGED_KT" | grep -q "^shared/"; then
  echo "pre-commit: running :shared unit tests..."
  ./gradlew :shared:testAndroidHostTest --daemon --quiet
fi

if echo "$STAGED_KT" | grep -q "^composeApp/"; then
  echo "pre-commit: running :composeApp unit tests..."
  ./gradlew :composeApp:testDebugUnitTest --daemon --quiet
fi

echo "pre-commit: all checks passed."
