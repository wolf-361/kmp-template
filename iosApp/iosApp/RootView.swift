import SwiftUI
import shared

// Top-level screen routing driven by GlobalUiEffect from the shared ViewModel layer.
// Each top-level graph root (auth, dashboard, …) owns its own NavigationStack so
// per-feature deep navigation stays encapsulated.
struct RootView: View {
    @State private var current: AppScreen = .auth
    @State private var showSettings = false
    @StateObject private var settingsHolder = SettingsViewModelHolder()

    private let effectsHandler: GlobalUiEffectsHandler

    init(effectsHandler: GlobalUiEffectsHandler) {
        self.effectsHandler = effectsHandler
    }

    var body: some View {
        Group {
            switch current {
            case .auth: AuthNavStack()
            case .dashboard: DashboardNavStack(showSettings: $showSettings)
            }
        }
        .sheet(isPresented: $showSettings) {
            NavigationStack {
                SettingsView()
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") { showSettings = false }
                        }
                    }
            }
        }
        .preferredColorScheme(settingsHolder.state.themeMode.preferredColorScheme)
        .task { await observeGlobalEffects() }
    }

    private func observeGlobalEffects() async {
        for await effect in effectsHandler.effects {
            switch onEnum(of: effect) {
            case .navigateTo(let nav):
                applyNavigation(nav.destination)
            case .navigateBack, .navigateBackTo:
                // per-feature stacks handle their own back navigation;
                // global back is a no-op at the root level
                break
            case .unauthorized:
                current = .auth
            case .showToast, .showSnackbar:
                break // wired up by a future toast layer
            }
        }
    }

    private func applyNavigation(_ destination: any Destination) {
        switch onEnum(of: destination) {
        case .authDestination:
            current = .auth
        case .topLevelGraph:
            current = .dashboard
        }
    }
}

private enum AppScreen {
    case auth
    case dashboard
}

// MARK: - Feature nav stacks

private struct AuthNavStack: View {
    var body: some View {
        NavigationStack {
            LoginView()
        }
    }
}

private struct DashboardNavStack: View {
    @Binding var showSettings: Bool

    var body: some View {
        NavigationStack {
            Text("Dashboard") // TODO: DashboardView
                .navigationTitle("Dashboard")
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            showSettings = true
                        } label: {
                            Image(systemName: "gear")
                        }
                    }
                }
        }
    }
}
