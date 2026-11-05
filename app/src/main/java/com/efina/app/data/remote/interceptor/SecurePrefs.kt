package com.efina.app.data.remote.interceptor

import android.content.Context
import android.content.SharedPreferences
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
    /**
     * EncryptedSharedPreferences.create() can throw on some devices (notably MIUI) when
     * the Tink-backed MasterKey can't be initialised. That failure happens during Hilt
     * dependency injection — before any ViewModel init — so it can't be caught inside
     * init. Fall back to plain SharedPreferences so the app still starts; security is
     * degraded but availability is preserved.
     */
    val secure: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "efina_secure_prefs",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("efina_secure_prefs", Context.MODE_PRIVATE)
    }
}
