package com.yourcompany.kmptemplate.auth.data

import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IOSOAuthFlowLauncher : OAuthFlowLauncher {

    // Reuse the provider across launches — ASWebAuthenticationSession holds it weakly,
    // so keeping it as a field prevents premature collection.
    private val presentationProvider = AppWindowProvider()

    override suspend fun launch(authUrl: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val url = NSURL(string = authUrl)

            val session = ASWebAuthenticationSession(
                uRL = url,
                callbackURLScheme = "kmptemplate",
            ) { callbackURL, error ->
                when {
                    error != null -> {
                        // ASWebAuthenticationSessionErrorCode.canceledLogin == 1
                        if (error.code == 1L) {
                            cont.resumeWithException(CancellationException("OAuth cancelled by user"))
                        } else {
                            cont.resumeWithException(Exception(error.localizedDescription))
                        }
                    }
                    callbackURL != null -> {
                        val components = NSURLComponents(uRL = callbackURL, resolvingAgainstBaseURL = false)
                        val code = components?.queryItems
                            ?.mapNotNull { it as? platform.Foundation.NSURLQueryItem }
                            ?.firstOrNull { it.name == "code" }
                            ?.value
                        if (code != null) {
                            cont.resume(code)
                        } else {
                            cont.resumeWithException(Exception("No authorization code in callback URI"))
                        }
                    }
                    else -> cont.resumeWithException(Exception("OAuth session completed with no result"))
                }
            }

            session.presentationContextProvider = presentationProvider
            session.prefersEphemeralWebBrowserSession = true
            session.start()

            cont.invokeOnCancellation { session.cancel() }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class AppWindowProvider :
    NSObject(),
    ASWebAuthenticationPresentationContextProvidingProtocol {
    @Suppress("DEPRECATION")
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow =
        UIApplication.sharedApplication.keyWindow ?: UIWindow()
}
