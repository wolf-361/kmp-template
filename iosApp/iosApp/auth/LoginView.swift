import SwiftUI
import shared

struct LoginView: View {
    @StateObject private var holder = AuthViewModelHolder()

    var body: some View {
        VStack(spacing: 12) {
            Text("Sign in")
                .font(.largeTitle)
                .padding(.bottom, 28)

            if holder.state.isLoading {
                ProgressView()
                    .frame(height: 120)
            } else {
                OAuthButton(label: "Continue with Google",
                            provider: OAuthProvider.google,
                            vm: holder.viewModel)
                OAuthButton(label: "Continue with Apple",
                            provider: OAuthProvider.apple,
                            vm: holder.viewModel)
                OAuthButton(label: "Continue with Microsoft",
                            provider: OAuthProvider.microsoft,
                            vm: holder.viewModel)
                OAuthButton(label: "Continue with GitHub",
                            provider: OAuthProvider.github,
                            vm: holder.viewModel)
            }

            if let error = holder.state.error {
                Text(error)
                    .foregroundStyle(.red)
                    .font(.caption)
                    .padding(.top, 8)
            }
        }
        .padding(.horizontal, 32)
    }
}

private struct OAuthButton: View {
    let label: String
    let provider: OAuthProvider
    let vm: AuthViewModel

    var body: some View {
        Button(label) {
            vm.onAction(action: AuthAction.LoginWith(provider: provider))
        }
        .buttonStyle(.bordered)
        .frame(maxWidth: .infinity)
    }
}

// Bridges Kotlin StateFlow<AuthState> to SwiftUI @Published via SKIE async sequence.
@MainActor
private final class AuthViewModelHolder: ObservableObject {
    let viewModel: AuthViewModel = KoinHelperKt.getAuthViewModel()
    @Published var state: AuthState = AuthState(isLoading: false, error: nil)

    init() {
        Task { await observeState() }
    }

    private func observeState() async {
        for await s in viewModel.state {
            state = s
        }
    }
}
