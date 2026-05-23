import SwiftUI
import shared

@main
struct iOSApp: App {
    private let effectsHandler: GlobalUiEffectsHandler

    init() {
        KoinHelperKt.doInitKoin()
        effectsHandler = KoinHelperKt.getGlobalUiEffectsHandler()
    }

    var body: some Scene {
        WindowGroup {
            RootView(effectsHandler: effectsHandler)
        }
    }
}
