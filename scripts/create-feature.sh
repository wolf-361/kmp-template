#!/usr/bin/env bash
# create-feature.sh — scaffold a new feature following Feature-Driven Architecture.
#
# Usage:
#   ./scripts/create-feature.sh <feature-name>
#
# Examples:
#   ./scripts/create-feature.sh profile
#   ./scripts/create-feature.sh course_detail    → feature package 'coursedetail', class prefix 'CourseDetail'
#
# What it creates:
#   shared/commonMain  — domain model, repository interface, repository impl, ViewModel + MVI contracts
#   composeApp         — Compose screen skeleton
#   iosApp             — SwiftUI view skeleton
#   Destination.kt     — new sealed interface entry
#   AppNavHost.kt      — composable entry + navigate() dispatch case

set -euo pipefail
LC_ALL=C

# ─── Helpers ──────────────────────────────────────────────────────────────────

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; RESET='\033[0m'

die()    { echo -e "${RED}error:${RESET} $*" >&2; exit 1; }
info()   { echo -e "${GREEN}  ✓${RESET}  $*"; }
warn()   { echo -e "${YELLOW}  !${RESET}  $*"; }
step()   { echo -e "\n${BOLD}$*${RESET}"; }
created(){ echo -e "${GREEN}  +${RESET}  $1"; }

# snake_case / kebab-case → PascalCase: course_detail → CourseDetail
# Uses Python3 (always available on macOS) — BSD sed doesn't support \U case conversion.
to_pascal() {
    python3 -c "
import sys, re
s = sys.argv[1]
print(''.join(w.capitalize() for w in re.split(r'[-_ ]', s) if w))
" "$1"
}

# Write a file, creating parent dirs. Skips if file already exists.
write_file() {
    local path="$1"
    local content="$2"
    if [[ -f "$path" ]]; then
        warn "Already exists, skipping: $path"
        return
    fi
    mkdir -p "$(dirname "$path")"
    printf '%s\n' "$content" > "$path"
    created "$path"
}

# ─── Argument parsing ─────────────────────────────────────────────────────────

[[ $# -eq 1 ]] || { echo "Usage: $0 <feature-name>"; exit 1; }

RAW_NAME="$1"
# Normalise: strip special chars, lowercase for package
FEATURE_PKG=$(echo "$RAW_NAME" | tr '[:upper:]' '[:lower:]' | sed 's/[-_ ]//g')
FEATURE_PASCAL=$(to_pascal "$RAW_NAME")

[[ "$FEATURE_PKG" =~ ^[a-z][a-z0-9]+$ ]] \
    || die "Feature name must be alphanumeric (got: '$FEATURE_PKG'). Example: profile, coursedetail"

[[ "$FEATURE_PKG" != "core" && "$FEATURE_PKG" != "di" && "$FEATURE_PKG" != "auth" ]] \
    || die "'$FEATURE_PKG' is a reserved package name."

# ─── Guard: must run from project root ────────────────────────────────────────

[[ -f "shared/build.gradle.kts" ]] || die "Run this script from the project root directory."

# ─── Detect current package from applicationId ────────────────────────────────

APP_ID=$(grep 'applicationId' composeApp/build.gradle.kts \
    | grep -o '"[^"]*"' | tr -d '"' | head -1)
[[ -n "$APP_ID" ]] || die "Could not read applicationId from composeApp/build.gradle.kts"

PKG="$APP_ID"                                   # com.acme.myapp
PKG_PATH="${PKG//.//}"                          # com/acme/myapp
SHARED_SRC="shared/src/commonMain/kotlin/${PKG_PATH}"
ANDROID_SRC="composeApp/src/main/kotlin/${PKG_PATH}"
IOS_SRC="iosApp/iosApp"

DEST_FILE="${SHARED_SRC}/core/navigation/Destination.kt"
NAV_HOST_FILE="${ANDROID_SRC}/AppNavHost.kt"

[[ -f "$DEST_FILE" ]] || die "Destination.kt not found at expected path: $DEST_FILE"
[[ -f "$NAV_HOST_FILE" ]] || die "AppNavHost.kt not found at expected path: $NAV_HOST_FILE"

# ─── Summary ──────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}create-feature: ${FEATURE_PASCAL}${RESET}"
echo "  package   : ${PKG}.${FEATURE_PKG}"
echo "  class     : ${FEATURE_PASCAL}"
echo "  destination: ${FEATURE_PASCAL}Destination.${FEATURE_PASCAL}"
echo ""
read -r -p "Proceed? [Y/n] " confirm
[[ "${confirm:-Y}" =~ ^[Yy]?$ ]] || exit 0

# ─── 1. Domain layer ──────────────────────────────────────────────────────────

step "1/5  Domain layer"

write_file "${SHARED_SRC}/${FEATURE_PKG}/domain/model/${FEATURE_PASCAL}.kt" \
"package ${PKG}.${FEATURE_PKG}.domain.model

data class ${FEATURE_PASCAL}(
    val id: String,
    // TODO: add your domain fields
)"

write_file "${SHARED_SRC}/${FEATURE_PKG}/domain/errors/${FEATURE_PASCAL}Error.kt" \
"package ${PKG}.${FEATURE_PKG}.domain.errors

import ${PKG}.core.domain.AppError

sealed interface ${FEATURE_PASCAL}Error : AppError {
    data object NotFound : ${FEATURE_PASCAL}Error
    // TODO: add feature-specific error cases
}"

write_file "${SHARED_SRC}/${FEATURE_PKG}/domain/repository/${FEATURE_PASCAL}Repository.kt" \
"package ${PKG}.${FEATURE_PKG}.domain.repository

import ${PKG}.core.domain.AppResult
import ${PKG}.${FEATURE_PKG}.domain.model.${FEATURE_PASCAL}
import kotlinx.coroutines.flow.Flow

interface ${FEATURE_PASCAL}Repository {
    val stream: Flow<${FEATURE_PASCAL}?>
    suspend fun load(id: String): AppResult<${FEATURE_PASCAL}>
    // TODO: add your repository methods
}"

# ─── 2. Data layer ────────────────────────────────────────────────────────────

step "2/5  Data layer"

write_file "${SHARED_SRC}/${FEATURE_PKG}/data/remote/${FEATURE_PASCAL}Routes.kt" \
"package ${PKG}.${FEATURE_PKG}.data.remote

internal object ${FEATURE_PASCAL}Routes {
    const val BASE = \"/${FEATURE_PKG}\"
    // TODO: add route constants
}"

write_file "${SHARED_SRC}/${FEATURE_PKG}/data/remote/dto/${FEATURE_PASCAL}Response.kt" \
"package ${PKG}.${FEATURE_PKG}.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ${FEATURE_PASCAL}Response(
    val id: String,
    // TODO: add API response fields
)"

write_file "${SHARED_SRC}/${FEATURE_PKG}/data/repository/${FEATURE_PASCAL}RepositoryImpl.kt" \
"package ${PKG}.${FEATURE_PKG}.data.repository

import ${PKG}.core.data.network.NetworkClient
import ${PKG}.core.domain.AppResult
import ${PKG}.${FEATURE_PKG}.data.remote.${FEATURE_PASCAL}Routes
import ${PKG}.${FEATURE_PKG}.data.remote.dto.${FEATURE_PASCAL}Response
import ${PKG}.${FEATURE_PKG}.domain.model.${FEATURE_PASCAL}
import ${PKG}.${FEATURE_PKG}.domain.repository.${FEATURE_PASCAL}Repository
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ${FEATURE_PASCAL}RepositoryImpl(
    private val networkClient: NetworkClient,
) : ${FEATURE_PASCAL}Repository {

    private val _stream = MutableStateFlow<${FEATURE_PASCAL}?>(null)
    override val stream: Flow<${FEATURE_PASCAL}?> = _stream.asStateFlow()

    override suspend fun load(id: String): AppResult<${FEATURE_PASCAL}> =
        networkClient.request<${FEATURE_PASCAL}Response> {
            url(\"\${${FEATURE_PASCAL}Routes.BASE}/\$id\")
        }.map { it.toDomain() }
}

private fun ${FEATURE_PASCAL}Response.toDomain(): ${FEATURE_PASCAL} = ${FEATURE_PASCAL}(
    id = id,
    // TODO: map response fields to domain model
)"

# ─── 3. Presentation layer ────────────────────────────────────────────────────

step "3/5  Presentation layer"

write_file "${SHARED_SRC}/${FEATURE_PKG}/presentation/${FEATURE_PASCAL}ViewModel.kt" \
"package ${PKG}.${FEATURE_PKG}.presentation

import ${PKG}.core.domain.extensions.handle
import ${PKG}.core.navigation.${FEATURE_PASCAL}Destination
import ${PKG}.core.presentation.BaseViewModel
import ${PKG}.${FEATURE_PKG}.domain.errors.${FEATURE_PASCAL}Error
import ${PKG}.${FEATURE_PKG}.domain.model.${FEATURE_PASCAL}
import ${PKG}.${FEATURE_PKG}.domain.repository.${FEATURE_PASCAL}Repository
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory

data class ${FEATURE_PASCAL}State(
    val isLoading: Boolean = false,
    val item: ${FEATURE_PASCAL}? = null,
    val error: String? = null,
)

sealed interface ${FEATURE_PASCAL}Action {
    data class Load(val id: String) : ${FEATURE_PASCAL}Action
    data object NavigateBack : ${FEATURE_PASCAL}Action
    // TODO: add your actions
}

sealed interface ${FEATURE_PASCAL}Effect {
    // TODO: add UI-only effects (dialogs, snackbars, animations)
    //       Navigation is handled by BaseViewModel.navigateTo() — NOT here.
}

@Factory
class ${FEATURE_PASCAL}ViewModel(
    private val repository: ${FEATURE_PASCAL}Repository,
) : BaseViewModel<${FEATURE_PASCAL}State, ${FEATURE_PASCAL}Action, ${FEATURE_PASCAL}Effect>(${FEATURE_PASCAL}State()) {

    override fun onAction(action: ${FEATURE_PASCAL}Action) {
        when (action) {
            is ${FEATURE_PASCAL}Action.Load -> load(action.id)
            is ${FEATURE_PASCAL}Action.NavigateBack -> navigateBack()
        }
    }

    private fun load(id: String) = viewModelScope.launch {
        setState { copy(isLoading = true, error = null) }
        repository.load(id).handle {
            success { item -> setState { copy(isLoading = false, item = item) } }
            failure<${FEATURE_PASCAL}Error.NotFound> {
                setState { copy(isLoading = false, error = \"Not found\") }
            }
            catch { err -> setState { copy(isLoading = false, error = err.toString()) } }
        }
    }
}"

# ─── 4. DI module ─────────────────────────────────────────────────────────────

step "4/5  DI module"

DI_FILE="${SHARED_SRC}/di/${FEATURE_PASCAL}Module.kt"
write_file "$DI_FILE" \
"package ${PKG}.di

import ${PKG}.${FEATURE_PKG}.data.repository.${FEATURE_PASCAL}RepositoryImpl
import ${PKG}.${FEATURE_PKG}.domain.repository.${FEATURE_PASCAL}Repository
import ${PKG}.core.data.network.NetworkClient
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module

@Module
class ${FEATURE_PASCAL}Module {

    @Factory
    fun provide${FEATURE_PASCAL}Repository(networkClient: NetworkClient): ${FEATURE_PASCAL}Repository =
        ${FEATURE_PASCAL}RepositoryImpl(networkClient)
}"

# ─── 5a. Android Compose screen ───────────────────────────────────────────────

step "5/5  Platform screens"

write_file "${ANDROID_SRC}/${FEATURE_PKG}/${FEATURE_PASCAL}Screen.kt" \
"package ${PKG}.${FEATURE_PKG}

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ${PKG}.${FEATURE_PKG}.presentation.${FEATURE_PASCAL}Action
import ${PKG}.${FEATURE_PKG}.presentation.${FEATURE_PASCAL}ViewModel
import org.koin.java.KoinJavaComponent.getKoin

@Composable
fun ${FEATURE_PASCAL}Screen(
    id: String,
    viewModel: ${FEATURE_PASCAL}ViewModel = remember { getKoin().get() },
) {
    val state by viewModel.state.collectAsState()

    // TODO: drive load with a side-effect once (LaunchedEffect(id) { viewModel.onAction(Load(id)) })

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isLoading -> CircularProgressIndicator()
            state.error != null -> Text(state.error!!)
            state.item != null -> Text(\"TODO: render \${state.item}\")
            else -> {}
        }
    }
}"

# ─── 5b. iOS SwiftUI view ─────────────────────────────────────────────────────

write_file "${IOS_SRC}/${FEATURE_PKG}/${FEATURE_PASCAL}View.swift" \
"import SwiftUI
import shared

struct ${FEATURE_PASCAL}View: View {
    let id: String
    @StateObject private var holder = ${FEATURE_PASCAL}ViewModelHolder()

    var body: some View {
        Group {
            if holder.state.isLoading {
                ProgressView()
            } else if let error = holder.state.error {
                Text(error).foregroundStyle(.red)
            } else if let item = holder.state.item {
                Text(\"TODO: render \\(item.id)\") // TODO: build real UI
            }
        }
        .navigationTitle(\"${FEATURE_PASCAL}\")
        .task { holder.viewModel.onAction(action: ${FEATURE_PASCAL}Action.Load(id: id)) }
    }
}

@MainActor
private final class ${FEATURE_PASCAL}ViewModelHolder: ObservableObject {
    let viewModel: ${FEATURE_PASCAL}ViewModel = KoinHelperKt.get${FEATURE_PASCAL}ViewModel()
    @Published var state: ${FEATURE_PASCAL}State = .init(isLoading: false, item: nil, error: nil)

    init() { Task { await observeState() } }

    private func observeState() async {
        for await s in viewModel.state { state = s }
    }
}"

# ─── 6. Update Destination.kt ─────────────────────────────────────────────────

step "Updating Destination.kt"

if grep -q "${FEATURE_PASCAL}Destination" "$DEST_FILE"; then
    warn "${FEATURE_PASCAL}Destination already present in Destination.kt — skipping"
else
    # Append the new sealed interface before the final blank line / EOF
    printf '\nsealed interface %sDestination : Destination {\n    @Serializable data object %s : %sDestination\n}\n' \
        "$FEATURE_PASCAL" "$FEATURE_PASCAL" "$FEATURE_PASCAL" >> "$DEST_FILE"
    info "Added ${FEATURE_PASCAL}Destination to Destination.kt"
fi

# ─── 7. Update AppNavHost.kt ──────────────────────────────────────────────────

step "Updating AppNavHost.kt"

# Add import
IMPORT_MARKER="// <create-feature:import>"
IMPORT_LINE="import ${PKG}.${FEATURE_PKG}.${FEATURE_PASCAL}Screen"
if grep -q "$IMPORT_LINE" "$NAV_HOST_FILE"; then
    warn "Import already present — skipping"
else
    sed -i '' "s|${IMPORT_MARKER}|${IMPORT_LINE}\n${IMPORT_MARKER}|" "$NAV_HOST_FILE"
    info "Added import to AppNavHost.kt"
fi

# Add composable entry
# Note: sed replaces only the marker text, leaving the line's leading spaces intact.
# So COMPOSABLE_LINE must NOT carry its own leading spaces (they'd double up).
COMPOSABLE_MARKER="// <create-feature:composable>"
COMPOSABLE_LINE="composable<${FEATURE_PASCAL}Destination.${FEATURE_PASCAL}> { ${FEATURE_PASCAL}Screen(id = \"\") }"
if grep -q "${FEATURE_PASCAL}Destination" "$NAV_HOST_FILE"; then
    warn "Composable entry already present — skipping"
else
    sed -i '' "s|${COMPOSABLE_MARKER}|${COMPOSABLE_LINE}\n        ${COMPOSABLE_MARKER}|" "$NAV_HOST_FILE"
    info "Added composable<${FEATURE_PASCAL}Destination.${FEATURE_PASCAL}> to AppNavHost.kt"
fi

# Add navigate dispatch case
NAVIGATE_MARKER="// <create-feature:navigate>"
NAVIGATE_LINE="is ${FEATURE_PASCAL}Destination.${FEATURE_PASCAL} -> navigate(destination)"
if grep -q "is ${FEATURE_PASCAL}Destination" "$NAV_HOST_FILE"; then
    warn "Navigate dispatch case already present — skipping"
else
    sed -i '' "s|${NAVIGATE_MARKER}|${NAVIGATE_LINE}\n        ${NAVIGATE_MARKER}|" "$NAV_HOST_FILE"
    info "Added navigate dispatch case to AppNavHost.kt"
fi

# ─── 8. Done ──────────────────────────────────────────────────────────────────

echo ""
echo -e "${GREEN}${BOLD}Feature '${FEATURE_PASCAL}' scaffolded.${RESET}"
echo ""
echo "Manual steps remaining:"
echo "  1. Register the DI module in KmpTemplateApplication.kt (Android):"
echo "       ${FEATURE_PASCAL}Module().module"
echo "  2. Register the DI module in KoinHelper.kt (iOS) — same pattern."
echo "  3. Expose get${FEATURE_PASCAL}ViewModel() in KoinHelper.kt for iOS:"
echo "       fun get${FEATURE_PASCAL}ViewModel(): ${FEATURE_PASCAL}ViewModel = GlobalContext.get().get()"
echo "  4. Add a navigation entry in RootView.swift (iOS) for ${FEATURE_PASCAL}Destination."
echo "  5. Implement the TODO sections in the generated files."
echo ""
