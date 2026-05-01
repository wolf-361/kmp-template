package com.yourcompany.kmptemplate.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

// Set this in Application.onCreate() before Koin starts
internal var appContext: Context? = null

actual fun createDataStore(): DataStore<Preferences> {
    val ctx = checkNotNull(appContext) {
        "Assign `appContext` in Application.onCreate() before DataStore is first injected"
    }
    return PreferenceDataStoreFactory.createWithPath {
        "${ctx.filesDir.absolutePath}/datastore/app_preferences.preferences_pb".toPath()
    }
}
