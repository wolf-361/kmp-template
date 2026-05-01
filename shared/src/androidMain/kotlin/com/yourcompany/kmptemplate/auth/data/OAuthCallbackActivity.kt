package com.yourcompany.kmptemplate.auth.data

import android.app.Activity
import android.os.Bundle

/**
 * Transparent trampoline that receives the OAuth redirect URI and hands the
 * authorization code back to [AndroidOAuthFlowLauncher] via its companion object.
 *
 * Redirect URI format: kmptemplate://oauth/{provider}/callback?code=...
 */
class OAuthCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val code = intent?.data?.getQueryParameter("code")
        if (code != null) {
            AndroidOAuthFlowLauncher.deliverCode(code)
        } else {
            AndroidOAuthFlowLauncher.deliverCancellation()
        }
        finish()
    }
}
