package com.efina.app.util

object Constants {
    // Secure Storage Keys
    const val KEY_AUTH_TOKEN = "auth_token"
    const val KEY_CSRF_TOKEN = "csrf_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_PIN = "user_pin"
    
    // DataStore Preferences Keys
    const val PREFS_THEME = "theme"
    const val PREFS_CURRENCY = "currency"
    const val PREFS_LANGUAGE = "language"
    const val PREFS_ONBOARDING_COMPLETE = "onboarding_complete"
    const val PREFS_BIOMETRIC_ENABLED = "biometric_enabled"
    const val PREFS_UPDATE_NOTIFICATIONS = "update_notifications"
    const val PREFS_LAST_NOTIFIED_VERSION = "last_notified_version"
    
    // Default Values
    const val DEFAULT_CURRENCY = "IDR"
    const val DEFAULT_LANGUAGE = "en"
    const val DEFAULT_TIMEZONE = "Asia/Jakarta"
    const val DEFAULT_THEME = "dark"
    
    // API Defaults
    const val DEFAULT_PAGE_SIZE = 20
    const val NETWORK_TIMEOUT_SECONDS = 30L
    
    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val PIN_LENGTH = 6
    const val MAX_DESCRIPTION_LENGTH = 500
}
