import SwiftUI

struct OrDivider: View {
    var body: some View {
        HStack(spacing: 12) {
            VStack { Divider() }
            Text("or")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            VStack { Divider() }
        }
    }
}

#Preview("Light") {
    OrDivider()
        .padding()
}

#Preview("Dark") {
    OrDivider()
        .padding()
        .preferredColorScheme(.dark)
}
