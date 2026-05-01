package com.efina.app.data.remote.interceptor

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.efina.app.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    @ApplicationContext private val context: Context
) : Interceptor {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "efina_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val path = originalRequest.url.encodedPath
        if (path.contains("auth/login") || path.contains("auth/register")) {
            return chain.proceed(originalRequest)
        }
        
        val token = securePrefs.getString(Constants.KEY_AUTH_TOKEN, null)
        
        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        
        return chain.proceed(request)
    }
}

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "efina_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: String) {
        securePrefs.edit().putString(Constants.KEY_AUTH_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return securePrefs.getString(Constants.KEY_AUTH_TOKEN, null)
    }
    
    fun saveUserId(userId: Long) {
        securePrefs.edit().putLong(Constants.KEY_USER_ID, userId).apply()
    }
    
    fun getUserId(): Long {
        return securePrefs.getLong(Constants.KEY_USER_ID, -1)
    }
    
    fun saveUserEmail(email: String) {
        securePrefs.edit().putString(Constants.KEY_USER_EMAIL, email).apply()
    }
    
    fun getUserEmail(): String? {
        return securePrefs.getString(Constants.KEY_USER_EMAIL, null)
    }
    
    fun hasPin(): Boolean {
        return securePrefs.getString(Constants.KEY_USER_PIN, null) != null
    }
    
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }
    
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
