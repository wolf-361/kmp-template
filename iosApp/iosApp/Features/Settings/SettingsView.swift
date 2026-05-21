import shared
import SwiftUI

struct SettingsView: View {
    @StateObject private var holder = SettingsViewModelHolder()

    var body: some View {
        Form {
            Section("Appearance") {
                Picker(
                    "Theme",
                    selection: Binding(
                        get: { holder.state.themeMode },
                        set: { holder.viewModel.onAction(action: SettingsActionSetThemeMode(mode: $0)) }
                    )
                ) {
                    Text("System").tag(ThemeMode.system)
                    Text("Light").tag(ThemeMode.light)
                    Text("Dark").tag(ThemeMode.dark)
                }
                .pickerStyle(.segmented)
            }

            // TODO: add more settings sections here
        }
        .navigationTitle("Settings")
    }
}
