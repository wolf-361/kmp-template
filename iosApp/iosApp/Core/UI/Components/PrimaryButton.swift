import SwiftUI

struct PrimaryButton: View {
    let title: String
    var isLoading: Bool = false
    var disabled: Bool = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Group {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(.white)
                } else {
                    Text(title)
                        .fontWeight(.semibold)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
        }
        .buttonStyle(.borderedProminent)
        .disabled(disabled || isLoading)
    }
}

#Preview("Idle") {
    PrimaryButton(title: "Continue", action: {})
        .padding()
}

#Preview("Loading") {
    PrimaryButton(title: "Continue", isLoading: true, action: {})
        .padding()
}

#Preview("Disabled") {
    PrimaryButton(title: "Continue", disabled: true, action: {})
        .padding()
}

#Preview("Dark") {
    PrimaryButton(title: "Continue", action: {})
        .padding()
        .preferredColorScheme(.dark)
}
