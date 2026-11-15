package com.efina.app.di

import android.content.Context
import com.efina.app.BuildConfig
import com.efina.app.data.local.EfinaDatabase
import com.efina.app.data.remote.interceptor.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton

/**
 * The local mirror.
 *
 * The SQLCipher passphrase is generated once and kept in the same encrypted
 * preference store as the session token. It is derived from nothing the user
 * knows, so there is no password prompt; the protection is that the database
 * file is unreadable without the app's Keystore-backed key.
 *
 * A background worker must be able to open the database without the Activity
 * being alive, which is why the passphrase lives in EncryptedSharedPreferences
 * rather than in memory.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val KEY_DB_PASSPHRASE = "efina_db_passphrase"

    @Provides
    @Singleton
    fun providePassphrase(tokenManager: TokenManager): ByteArray {
        val existing = tokenManager.getDbPassphrase()
        if (existing != null) return existing

        val fresh = ByteArray(32).also { SecureRandom().nextBytes(it) }
        tokenManager.saveDbPassphrase(fresh)
        return fresh
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        passphrase: ByteArray,
    ): EfinaDatabase = EfinaDatabase.build(context, passphrase)

    @Provides
    @Singleton
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context,
    ): com.efina.app.data.sync.ConnectivityObserver =
        com.efina.app.data.sync.ConnectivityObserver(context)
}
