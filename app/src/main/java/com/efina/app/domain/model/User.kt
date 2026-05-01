package com.efina.app.domain.model

data class User(
    val id: Long,
    val email: String,
    val name: String
)

data class AuthToken(
    val token: String,
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
    val timezone: String
)

data class UserStats(
    val activeStreak: Int,
    val totalTransactions: Int,
    val xp: Int,
    val level: Int,
    val tier: String
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
