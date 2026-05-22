import SwiftUI

struct AppHeader: View {
    let text: String

    var body: some View {
        Text(text.uppercased())
            .font(.system(.title2, design: .default, weight: .black))
            .tracking(4)
            .foregroundStyle(Color.accentColor)
            .frame(maxWidth: .infinity, alignment: .center)
    }
}

#Preview("Light") {
    AppHeader(text: "YourApp")
        .padding()
}

#Preview("Dark") {
    AppHeader(text: "YourApp")
        .padding()
        .preferredColorScheme(.dark)
}
