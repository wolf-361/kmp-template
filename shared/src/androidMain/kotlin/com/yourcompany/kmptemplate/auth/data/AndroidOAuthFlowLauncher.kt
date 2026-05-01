package com.yourcompany.kmptemplate.auth.data

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.yourcompany.kmptemplate.auth.domain.port.OAuthFlowLauncher
import com.yourcompany.kmptemplate.core.data.local.appContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException

class AndroidOAuthFlowLauncher : OAuthFlowLauncher {

    override suspend fun launch(authUrl: String): String {
        val deferred = CompletableDeferred<String>()
        pendingAuth = deferred

        val tabIntent = CustomTabsIntent.Builder()
            .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            .build()
        tabIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        tabIntent.launchUrl(appContext, Uri.parse(authUrl))

        return try {
            deferred.await()
        } finally {
            pendingAuth = null
        }
    }

    companion object {
        @Volatile private var pendingAuth: CompletableDeferred<String>? = null

        // Called by OAuthCallbackActivity when the provider redirects back with a code.
        internal fun deliverCode(code: String) {
            pendingAuth?.complete(code)
            pendingAuth = null
        }

        // Called by MainActivity.onResume() when the user closes the Custom Tab without completing
        // the flow (back press, swipe away). CompletableDeferred.completeExceptionally() is a
        // no-op if already completed, so the success and cancel paths can't race.
        fun deliverCancellation() {
            pendingAuth?.let {
                it.completeExceptionally(CancellationException("OAuth flow cancelled by user"))
                pendingAuth = null
            }
        }
    }
}
