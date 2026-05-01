package com.yourcompany.kmptemplate.auth.data.remote

internal object AuthRoutes {
    private const val BASE = "/auth"
    const val OAUTH = "$BASE/oauth"
    const val LOGOUT = "$BASE/logout"
}
