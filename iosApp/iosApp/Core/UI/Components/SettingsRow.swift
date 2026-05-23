import SwiftUI

struct SettingsRow<Trailing: View>: View {
    var icon: String?
    var iconTint: Color = .accentColor
    var title: String
    var subtitle: String?
    var showChevron: Bool = false
    var action: (() -> Void)?
    @ViewBuilder var trailing: Trailing

    var body: some View {
        let content = HStack(spacing: 14) {
            if let icon {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundStyle(iconTint)
                    .frame(width: 24, alignment: .center)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body)
                    .foregroundStyle(.primary)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
            trailing

            if showChevron {
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .contentShape(Rectangle())

        if let action {
            Button(action: action) { content }
                .buttonStyle(.plain)
        } else {
            content
        }
    }
}

extension SettingsRow where Trailing == EmptyView {
    init(
        icon: String? = nil,
        iconTint: Color = .accentColor,
        title: String,
        subtitle: String? = nil,
        showChevron: Bool = false,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.iconTint = iconTint
        self.title = title
        self.subtitle = subtitle
        self.showChevron = showChevron
        self.action = action
        self.trailing = EmptyView()
    }
}

#Preview("Icon + Chevron") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", showChevron: true, action: {})
    }
}

#Preview("With Subtitle") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", subtitle: "System", showChevron: true, action: {})
    }
}

#Preview("With Toggle") {
    List {
        SettingsRow(icon: "bell.fill", title: "Notifications") {
            Toggle("", isOn: .constant(true)).labelsHidden()
        }
    }
}

#Preview("No Icon") {
    List {
        SettingsRow(title: "Sign out", action: {})
    }
}

#Preview("Dark") {
    List {
        SettingsRow(icon: "moon.fill", title: "Theme", subtitle: "Dark", showChevron: true, action: {})
    }
    .preferredColorScheme(.dark)
}
