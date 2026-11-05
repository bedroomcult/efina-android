package com.efina.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "efina_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.dataStore
    
    val theme: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_THEME] ?: Constants.DEFAULT_THEME
    }
    
    val currency: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY] ?: Constants.DEFAULT_CURRENCY
    }
    
    val language: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: Constants.DEFAULT_LANGUAGE
    }
    
    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }
    
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }
    
    suspend fun setTheme(theme: String) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = theme }
    }
    
    suspend fun setCurrency(currency: String) {
        dataStore.edit { prefs -> prefs[KEY_CURRENCY] = currency }
    }
    
    suspend fun setLanguage(language: String) {
        dataStore.edit { prefs -> prefs[KEY_LANGUAGE] = language }
    }
    
    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ONBOARDING_COMPLETE] = complete }
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_BIOMETRIC_ENABLED] = enabled }
    }
    
    suspend fun setUpdateNotifications(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_UPDATE_NOTIFICATIONS] = enabled }
    }
    
    companion object {
        private val KEY_THEME = stringPreferencesKey(Constants.PREFS_THEME)
        private val KEY_CURRENCY = stringPreferencesKey(Constants.PREFS_CURRENCY)
        private val KEY_LANGUAGE = stringPreferencesKey(Constants.PREFS_LANGUAGE)
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey(Constants.PREFS_ONBOARDING_COMPLETE)
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey(Constants.PREFS_BIOMETRIC_ENABLED)
        private val KEY_UPDATE_NOTIFICATIONS = booleanPreferencesKey(Constants.PREFS_UPDATE_NOTIFICATIONS)
    }
}
