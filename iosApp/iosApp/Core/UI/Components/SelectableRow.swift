import SwiftUI

struct SelectableRow: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(label)
                    .foregroundStyle(.primary)
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(Color.accentColor)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.vertical, 4)
    }
}

#Preview("All States") {
    List {
        SelectableRow(label: "System", isSelected: true, action: {})
        SelectableRow(label: "Light", isSelected: false, action: {})
        SelectableRow(label: "Dark", isSelected: false, action: {})
    }
}

#Preview("Dark") {
    List {
        SelectableRow(label: "System", isSelected: true, action: {})
        SelectableRow(label: "Light", isSelected: false, action: {})
        SelectableRow(label: "Dark", isSelected: false, action: {})
    }
    .preferredColorScheme(.dark)
}
