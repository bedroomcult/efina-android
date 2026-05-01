package com.efina.app.domain.model

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val planId: String,
    val forceOnboarding: Boolean = false
)

data class AuthToken(
    val token: String,
    val csrfToken: String,
    val user: User
)

data class Session(
    val id: String,
    val deviceInfo: String,
    val ipAddress: String,
    val createdAt: String
)

data class UserSettings(
    val theme: String,
    val language: String,
    val currency: String,
    val timezone: String,
    val hasSeenTutorial: Boolean
)

data class UserStats(
    val activeStreak: Int,
    val totalTransactions: Int,
    val lastTransactionDate: String?,
    val xp: Int,
    val level: Int,
    val tier: String,
    val xpToNextLevel: Int,
    val xpProgressPercent: Int
)

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val tier: String,
    val xpReward: Int,
    val achievedAt: String?,
    val progress: Int?
)

data class UserProfile(
    val stats: UserStats,
    val achievements: List<Achievement>
)
