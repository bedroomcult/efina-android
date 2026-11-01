package com.efina.app.data.remote.interceptor

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The encrypted preference file, shared by TokenManager and any other component
 * that needs at-rest protection. Kept separate so the MasterKey is built exactly
 * once; building it per call is both slow and a source of key-mismatch bugs.
 */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    val secure = EncryptedSharedPreferences.create(
        context,
        "efina_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}
