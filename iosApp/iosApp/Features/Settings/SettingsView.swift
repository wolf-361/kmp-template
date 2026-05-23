import shared
import SwiftUI

struct SettingsView: View {
    @StateObject private var holder = SettingsViewModelHolder()
    @State private var showThemeSheet = false

    var body: some View {
        Form {
            Section("Appearance") {
                SettingsRow(
                    icon: "moon.fill",
                    title: "Theme",
                    subtitle: holder.state.themeMode.displayName,
                    showChevron: true,
                    action: { showThemeSheet = true }
                )
            }
        }
        .navigationTitle("Settings")
        .sheet(isPresented: $showThemeSheet) {
            NavigationStack {
                List {
                    SelectableRow(
                        label: ThemeMode.system.displayName,
                        isSelected: holder.state.themeMode == .system,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .system)
                            )
                            showThemeSheet = false
                        }
                    )
                    SelectableRow(
                        label: ThemeMode.light.displayName,
                        isSelected: holder.state.themeMode == .light,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .light)
                            )
                            showThemeSheet = false
                        }
                    )
                    SelectableRow(
                        label: ThemeMode.dark.displayName,
                        isSelected: holder.state.themeMode == .dark,
                        action: {
                            holder.viewModel.onAction(
                                action: SettingsActionSetThemeMode(mode: .dark)
                            )
                            showThemeSheet = false
                        }
                    )
                }
                .navigationTitle("Theme")
                .navigationBarTitleDisplayMode(.inline)
            }
            .presentationDetents([.medium])
            .preferredColorScheme(holder.state.themeMode.preferredColorScheme)
        }
    }
}
