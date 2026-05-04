#!/usr/bin/env bash
# init-project.sh — rename this template to your own app.
# Run once after cloning. Idempotent: safe to re-run if you catch a typo.
#
# Usage:
#   ./scripts/init-project.sh --company acme --app tracker --display "Acme Tracker"
#
# What it does:
#   1. Replaces package names, class prefixes, and URL scheme throughout the source tree
#   2. Renames the Kotlin source directories to match the new package
#   3. Updates the display name in string resources and iOS app name

set -euo pipefail
LC_ALL=C  # keep sed from choking on non-ASCII in binary files

# ─── Helpers ──────────────────────────────────────────────────────────────────

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'

die()  { echo -e "${RED}error:${RESET} $*" >&2; exit 1; }
info() { echo -e "${GREEN}  ✓${RESET}  $*"; }
warn() { echo -e "${YELLOW}  !${RESET}  $*"; }
step() { echo -e "\n${BOLD}$*${RESET}"; }

usage() {
    cat <<EOF
Usage: $0 --company COMPANY --app APP [--display "Display Name"]

  --company   Reverse-domain segment, lowercase, no spaces.   e.g. acme
  --app       App identifier,         lowercase, no spaces.   e.g. tracker
  --display   Human-readable name shown in the UI.            e.g. "Acme Tracker"
              Defaults to title-cased --app value.

Example:
  $0 --company acme --app tracker --display "Acme Tracker"

EOF
    exit 1
}

to_pascal() {
    # myapp → Myapp    my-app → MyApp    my_app → MyApp
    # Uses Python3 — BSD sed doesn't support \U case conversion on macOS.
    python3 -c "
import sys, re
s = sys.argv[1]
print(''.join(w.capitalize() for w in re.split(r'[-_ ]', s) if w))
" "$1"
}

# ─── Argument parsing ─────────────────────────────────────────────────────────

COMPANY=""
APP_NAME=""
DISPLAY_NAME=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --company)      COMPANY="${2:-}";      shift 2 ;;
        --app)          APP_NAME="${2:-}";     shift 2 ;;
        --display)      DISPLAY_NAME="${2:-}"; shift 2 ;;
        -h|--help)      usage ;;
        *)              die "Unknown argument: $1. Run with --help for usage." ;;
    esac
done

[[ -z "$COMPANY" ]]  && die "--company is required"
[[ -z "$APP_NAME" ]] && die "--app is required"

[[ "$COMPANY" =~ ^[a-z][a-z0-9]*$ ]] || die "--company must be lowercase alphanumeric, e.g. acme"
[[ "$APP_NAME" =~ ^[a-z][a-z0-9]*$ ]] || die "--app must be lowercase alphanumeric, e.g. tracker"

APP_PASCAL=$(to_pascal "$APP_NAME")
COMPANY_PASCAL=$(to_pascal "$COMPANY")
[[ -z "$DISPLAY_NAME" ]] && DISPLAY_NAME="$APP_PASCAL"

# ─── Guard: must run from project root ────────────────────────────────────────

[[ -f "shared/build.gradle.kts" ]] || die "Run this script from the project root directory."

# ─── Guard: refuse to overwrite if already initialised ────────────────────────

if ! grep -qr "yourcompany" shared/src/commonMain/kotlin 2>/dev/null; then
    warn "No 'yourcompany' placeholder found — looks like init was already run."
    read -r -p "Continue anyway? [y/N] " confirm
    [[ "$confirm" =~ ^[Yy]$ ]] || exit 0
fi

# ─── Derived values ───────────────────────────────────────────────────────────

OLD_PKG="com.yourcompany.kmptemplate"
NEW_PKG="com.${COMPANY}.${APP_NAME}"
OLD_PASCAL="KmpTemplate"
NEW_PASCAL="${COMPANY_PASCAL}${APP_PASCAL}"   # e.g. AcmeTracker
OLD_SCHEME="kmptemplate"
NEW_SCHEME="${APP_NAME}"
OLD_DISPLAY="KMP Template"
NEW_DISPLAY="${DISPLAY_NAME}"

echo ""
echo -e "${BOLD}KMP Template — project initialisation${RESET}"
echo "  package     : ${OLD_PKG} → ${NEW_PKG}"
echo "  class prefix: ${OLD_PASCAL} → ${NEW_PASCAL}"
echo "  URL scheme  : ${OLD_SCHEME}:// → ${NEW_SCHEME}://"
echo "  display name: ${OLD_DISPLAY} → ${NEW_DISPLAY}"
echo ""
read -r -p "Proceed? [Y/n] " confirm
[[ "${confirm:-Y}" =~ ^[Yy]?$ ]] || exit 0

# ─── File types to process ────────────────────────────────────────────────────

FILE_PATTERN=( -name "*.kt" -o -name "*.kts" -o -name "*.xml" -o -name "*.swift"
               -o -name "*.toml" -o -name "*.properties" -o -name "*.md"
               -o -name "*.yml" -o -name "*.yaml" )

EXCLUDES=( ! -path "./.git/*" ! -path "*/build/*" ! -path "./.gradle/*"
           ! -path "*/generated/*" ! -path "*/DerivedData/*" )

find_files() {
    find . -type f \( "${FILE_PATTERN[@]}" \) "${EXCLUDES[@]}"
}

# ─── In-place text replacements ───────────────────────────────────────────────
# Order matters: most-specific patterns first.

step "1/3  Replacing text in source files…"

# 1a. Full package name
find_files | xargs sed -i '' "s|${OLD_PKG}|${NEW_PKG}|g"
info "Package ${OLD_PKG} → ${NEW_PKG}"

# 1b. PascalCase class prefix (KmpTemplate → AcmeTracker)
find_files | xargs sed -i '' "s|${OLD_PASCAL}|${NEW_PASCAL}|g"
info "Class prefix ${OLD_PASCAL} → ${NEW_PASCAL}"

# 1c. URL scheme + Keychain service name (kmptemplate → appname)
#     This covers:  kmptemplate://  android:scheme="kmptemplate"
#                   callbackURLScheme = "kmptemplate"  "kmptemplate.auth"
find_files | xargs sed -i '' "s|${OLD_SCHEME}|${NEW_SCHEME}|g"
info "Identifier '${OLD_SCHEME}' → '${NEW_SCHEME}'"

# 1d. Display name in string resources / comments
find_files | xargs sed -i '' "s|${OLD_DISPLAY}|${NEW_DISPLAY}|g"
info "Display name '${OLD_DISPLAY}' → '${NEW_DISPLAY}'"

# ─── Rename Kotlin source directories ─────────────────────────────────────────

step "2/3  Renaming source directories…"

OLD_DIR_SUFFIX="com/yourcompany/kmptemplate"
NEW_DIR_SUFFIX="com/${COMPANY}/${APP_NAME}"

# Collect all matching dirs into an array before renaming (avoid mid-loop mutation)
mapfile -t KOTLIN_ROOTS < <(find . -type d -name "kotlin" "${EXCLUDES[@]}")

for kt_root in "${KOTLIN_ROOTS[@]}"; do
    old_src="${kt_root}/${OLD_DIR_SUFFIX}"
    new_src="${kt_root}/${NEW_DIR_SUFFIX}"

    if [[ ! -d "$old_src" ]]; then
        continue
    fi

    mkdir -p "$(dirname "$new_src")"
    mv "$old_src" "$new_src"

    # Clean up the now-empty 'yourcompany' parent if nothing else is in it
    rmdir "${kt_root}/com/yourcompany" 2>/dev/null || true

    info "Moved: ${old_src} → ${new_src}"
done

# ─── Sanity checks ────────────────────────────────────────────────────────────

step "3/3  Verification…"

remaining=$(find_files | xargs grep -l "yourcompany" 2>/dev/null | head -5 || true)
if [[ -n "$remaining" ]]; then
    warn "Some 'yourcompany' references may remain:"
    echo "$remaining" | while read -r f; do warn "  $f"; done
else
    info "No 'yourcompany' placeholders remaining"
fi

remaining=$(find_files | xargs grep -l "${OLD_SCHEME}" 2>/dev/null | grep -v "init-project.sh" | head -5 || true)
if [[ -n "$remaining" ]]; then
    warn "Some '${OLD_SCHEME}' references may remain in:"
    echo "$remaining" | while read -r f; do warn "  $f"; done
fi

echo ""
echo -e "${GREEN}${BOLD}Done.${RESET} Next steps:"
echo "  1. Sync your Gradle files in Android Studio"
echo "  2. Update the iOS bundle identifier in Xcode (currently still uses placeholder)"
echo "  3. Replace TODO_CLIENT_ID in AuthViewModel.kt with real OAuth client IDs"
echo "  4. Set BASE_URL in NetworkConfig.kt"
echo "  5. Commit: git add -A && git commit -m 'chore: initialise project from kmp-template'"
echo ""
