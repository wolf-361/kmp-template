import shared
import SwiftUI

@MainActor
final class SettingsViewModelHolder: ObservableObject {
    let viewModel: SettingsViewModel = KoinHelperKt.getSettingsViewModel()
    @Published var state: SettingsState

    init() {
        // StateFlow always has a current value; unwrap is safe.
        state = viewModel.state.value!
        Task { await observeState() }
    }

    private func observeState() async {
        for await s in viewModel.state {
            if let s { state = s }
        }
    }
}
