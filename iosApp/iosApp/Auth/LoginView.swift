import SwiftUI
import shared

struct LoginView: View {
    @StateObject private var holder = AuthViewModelHolder()

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    Spacer(minLength: 48)

                    AppHeader(text: "YourApp")

                    Spacer(minLength: 48)

                    Text("Sign in")
                        .font(.title)
                        .fontWeight(.bold)
                        .padding(.bottom, 32)

                    if holder.state.isLoading {
                        ProgressView()
                            .frame(height: 120)
                    } else {
                        VStack(spacing: 12) {
                            OAuthButton(
                                label: "Continue with Google",
                                provider: OAuthProvider.google,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with Apple",
                                provider: OAuthProvider.apple,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with Microsoft",
                                provider: OAuthProvider.microsoft,
                                vm: holder.viewModel
                            )
                            OAuthButton(
                                label: "Continue with GitHub",
                                provider: OAuthProvider.github,
                                vm: holder.viewModel
                            )
                        }
                    }

                    if let error = holder.state.error {
                        Text(error)
                            .foregroundStyle(.red)
                            .font(.caption)
                            .padding(.top, 16)
                    }

                    Spacer(minLength: 48)
                }
                .padding(.horizontal, 32)
                .frame(minHeight: geometry.size.height)
            }
        }
    }
}

private struct OAuthButton: View {
    let label: String
    let provider: OAuthProvider
    let vm: AuthViewModel

    var body: some View {
        Button(label) {
            vm.onAction(action: AuthActionLoginWith(provider: provider))
        }
        .buttonStyle(.bordered)
        .controlSize(.large)
        .frame(maxWidth: .infinity)
    }
}

@MainActor
private final class AuthViewModelHolder: ObservableObject {
    let viewModel: AuthViewModel = KoinHelperKt.getAuthViewModel()
    @Published var state: AuthState = AuthState(isLoading: false, error: nil)

    init() {
        Task { await observeState() }
    }

    private func observeState() async {
        for await s in viewModel.state {
            if let s { state = s }
        }
    }
}
