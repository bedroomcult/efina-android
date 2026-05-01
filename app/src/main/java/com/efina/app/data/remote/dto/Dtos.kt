package com.efina.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ Auth DTOs ============

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class AuthResponseDto(
    val token: String,
    @SerializedName("csrfToken")
    val csrfToken: String?,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    @SerializedName("plan_id")
    val planId: String?,
    @SerializedName("force_onboarding")
    val forceOnboarding: Boolean?
)

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)

data class PinRequestDto(
    val pin: String
)

data class SessionsResponseDto(
    val sessions: List<SessionDto>
)

data class SessionDto(
    val id: String,
    @SerializedName("device_info")
    val deviceInfo: String,
    @SerializedName("ip_address")
    val ipAddress: String,
    @SerializedName("created_at")
    val createdAt: String
)

// ============ Account DTOs ============

data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String?,
    val description: String?,
    @SerializedName("display_order")
    val displayOrder: Int?,
    @SerializedName("initial_balance")
    val initialBalance: Double?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class CreateAccountRequestDto(
    val name: String,
    val type: String,
    val balance: Double?,
    val currency: String?,
    val description: String?
)

// ============ Transaction DTOs ============

data class TransactionDto(
    val id: Long,
    @SerializedName("user_id")
    val userId: Long,
    @SerializedName("account_id")
    val accountId: Long,
    @SerializedName("account_name")
    val accountName: String?,
    @SerializedName("category_id")
    val categoryId: Long?,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("category_color")
    val categoryColor: String?,
    @SerializedName("category_icon")
    val categoryIcon: String?,
    @SerializedName("to_account_id")
    val toAccountId: Long?,
    @SerializedName("to_account_name")
    val toAccountName: String?,
    val amount: Double,
    val type: String,
    val description: String?,
    val date: String,
    val mood: String?,
    @SerializedName("transfer_pair_id")
    val transferPairId: String?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class TransactionListResponseDto(
    val transactions: List<TransactionDto>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class CreateTransactionRequestDto(
    @SerializedName("account_id")
    val accountId: Long,
    @SerializedName("category_id")
    val categoryId: Long?,
    val amount: Double,
    val type: String,
    val description: String?,
    val date: String,
    val mood: String?
)

// ============ Category DTOs ============

data class CategoryDto(
    val id: Long,
    val name: String,
    val type: String,
    val color: String,
    val icon: String?,
    @SerializedName("parent_id")
    val parentId: Long?
)

data class CreateCategoryRequestDto(
    val name: String,
    val type: String,
    val color: String,
    val icon: String?,
    @SerializedName("parent_id")
    val parentId: Long?
)

// ============ Budget DTOs ============

data class BudgetDto(
    val id: Long,
    @SerializedName("category_id")
    val categoryId: Long,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("category_color")
    val categoryColor: String?,
    @SerializedName("category_icon")
    val categoryIcon: String?,
    val amount: Double,
    @SerializedName("period_type")
    val periodType: String,
    @SerializedName("period_start")
    val periodStart: String,
    @SerializedName("period_end")
    val periodEnd: String?,
    val spent: Double,
    val percentage: Int
)

data class CreateBudgetRequestDto(
    @SerializedName("category_id")
    val categoryId: Long,
    val amount: Double,
    @SerializedName("period_type")
    val periodType: String
)

// ============ Goal DTOs ============

data class GoalDto(
    val id: Long,
    val name: String,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("current_amount")
    val currentAmount: Double,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("target_date")
    val targetDate: String,
    @SerializedName("period_type")
    val periodType: String,
    @SerializedName("linked_account_id")
    val linkedAccountId: Long?,
    @SerializedName("account_name")
    val accountName: String?,
    @SerializedName("total_periods")
    val totalPeriods: Int,
    @SerializedName("remaining_periods")
    val remainingPeriods: Int,
    @SerializedName("remaining_amount")
    val remainingAmount: Double,
    @SerializedName("suggested_saving")
    val suggestedSaving: Double
)

data class CreateGoalRequestDto(
    val name: String,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("target_date")
    val targetDate: String,
    @SerializedName("period_type")
    val periodType: String,
    @SerializedName("linked_account_id")
    val linkedAccountId: Long?
)

// ============ User Stats DTOs ============

data class UserStatsDto(
    @SerializedName("active_streak")
    val activeStreak: Int?,
    @SerializedName("total_transactions")
    val totalTransactions: Int?,
    @SerializedName("last_transaction_date")
    val lastTransactionDate: String?,
    val role: String?,
    val xp: Int?,
    val level: Int?,
    val tier: String?,
    @SerializedName("xp_to_next_level")
    val xpToNextLevel: Int?,
    @SerializedName("xp_progress_percent")
    val xpProgressPercent: Int?
)

data class AchievementDto(
    val id: String,
    val name: String,
    val description: String,
    val tier: String?,
    @SerializedName("xp_reward")
    val xpReward: Int?,
    @SerializedName("achieved_at")
    val achievedAt: String?,
    val progress: Int?
)

data class UserProfileResponseDto(
    val stats: UserStatsDto,
    val achievements: List<AchievementDto>
)

// ============ Settings DTOs ============

data class UserSettingsDto(
    val theme: String?,
    val language: String?,
    val currency: String?,
    val timezone: String?,
    @SerializedName("has_seen_tutorial")
    val hasSeenTutorial: Boolean?
)

data class UpdateSettingsRequestDto(
    val theme: String?,
    val language: String?,
    val currency: String?,
    val timezone: String?
)

// ============ AI DTOs ============

data class ChatRequestDto(
    val message: String,
    val context: Map<String, Any>?
)

data class ChatResponseDto(
    val response: String,
    @SerializedName("suggested_actions")
    val suggestedActions: List<SuggestedActionDto>?
)

data class SuggestedActionDto(
    val type: String,
    val data: Map<String, Any>
)

// ============ Transfer DTOs ============

data class CreateTransferRequestDto(
    @SerializedName("from_account_id")
    val fromAccountId: Long,
    @SerializedName("to_account_id")
    val toAccountId: Long,
    val amount: Double,
    val date: String,
    val description: String?
)

// ============ Error DTO ============

data class ErrorResponseDto(
    val error: String,
    val code: String?
)
