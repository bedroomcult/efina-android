package com.efina.app.domain.model

/** Current user, as returned by `POST auth/login` and `GET user`. */
data class User(
    val id: Long,
    val email: String,
    val name: String,
    val role: String = "user",
    val planId: String = "free",
    val currency: String = "IDR",
    val language: String = "id",
    val timezone: String = "Asia/Jakarta",
    val hasSeenTutorial: Boolean = false,
    val forceOnboarding: Boolean = false,
    val accountCount: Int = 0
) {
    val isPro: Boolean get() = planId == "pro"
    val isAdmin: Boolean get() = role == "admin"
}

data class Achievement(
    val id: Long,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val target: Int,
    val progress: Int,
    val achieved: Boolean,
    val achievedAt: String?
)

data class FamilyGroup(
    val id: Long,
    val name: String,
    val memberRole: String,
    val memberCount: Int,
    val members: List<FamilyMember> = emptyList()
)

data class FamilyMember(
    val userId: Long,
    val role: String,
    val name: String,
    val email: String?,
    val joinedAt: String?
)

data class AccountMember(
    val userId: Long,
    val role: AccountShareRole,
    val name: String,
    val email: String?,
    val joinedAt: String?
)

data class AppNotification(
    val id: Long,
    val title: String,
    val body: String?,
    val type: String,
    val link: String?,
    val isRead: Boolean,
    val createdAt: String?
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: Long,
    val name: String,
    val xp: Int,
    val level: Int,
    val tier: String?
)

/** Everything the dashboard needs, composed from the real endpoints. */
data class DashboardData(
    val totalBalance: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0
) {
    val netFlow: Double get() = monthlyIncome - monthlyExpense
}
