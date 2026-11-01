package com.efina.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the Efina backend.
 *
 * Field names mirror the JSON the Pages Functions actually return, which in turn
 * mirrors the D1 column names (snake_case). Nothing here is renamed to "look
 * Kotlin"; a mapping layer in Mappers.kt handles the domain translation, so a
 * backend column rename surfaces as one compile error instead of silent nulls.
 *
 * The JSON reader is configured with `ignoreUnknownKeys = true` (AppModule), so
 * adding a backend column never breaks the client.
 */

// ─────────────────────────── Auth ───────────────────────────

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val name: String? = null
)

@Serializable
data class GoogleLoginRequestDto(
    val credential: String
)

@Serializable
data class ForgotPasswordRequestDto(
    val email: String
)

/** `POST auth/login` and `POST auth/register` both return this. */
@Serializable
data class AuthResponseDto(
    val token: String,
    val csrfToken: String? = null,
    val user: UserDto
)

// ─────────────────────────── User ───────────────────────────

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    val name: String? = null,
    val role: String = "user",
    val plan_id: String = "free",
    @SerialName("theme") val theme: String? = null,
    val language: String? = null,
    val currency: String? = null,
    val timezone: String? = null,
    val has_seen_tutorial: Boolean = false,
    val force_onboarding: Boolean = false,
    val account_count: Int = 0
)

@Serializable
data class UpdateUserRequestDto(
    val name: String? = null,
    val force_onboarding: Int? = null
)

@Serializable
data class ChangePasswordRequestDto(
    val current_password: String,
    val new_password: String
)

// ─────────────────────────── Accounts ───────────────────────────

@Serializable
data class AccountDto(
    val id: Long,
    val user_id: Long,
    val name: String,
    val type: String = "Bank",
    val balance: Double = 0.0,
    val currency: String = "IDR",
    val description: String? = null,
    val initial_balance: Double = 0.0,
    val order_index: Int = 0,
    val disabled: Int = 0,
    val color: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Sharing (migration 0079)
    val family_id: Long? = null,
    val is_shared: Int = 0,
    /** 'admin' | 'member' | 'viewer' — computed per caller by the API. */
    val account_role: String? = null,
    val member_count: Int = 0
)

@Serializable
data class CreateAccountRequestDto(
    val name: String,
    val type: String,
    val balance: Double = 0.0,
    val currency: String = "IDR",
    val description: String? = null,
    val color: String? = null,
    val family_id: Long? = null
)

@Serializable
data class UpdateAccountRequestDto(
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String,
    val description: String? = null,
    val color: String? = null
)

@Serializable
data class UpdateAccountSharingRequestDto(
    val family_id: Long? = null
)

@Serializable
data class RecalculateResultDto(
    val id: Long,
    val name: String,
    val currentBalance: Double = 0.0,
    val calculatedBalance: Double = 0.0,
    val discrepancy: Boolean = false,
    val currency: String = "IDR"
)

// ─────────────────────────── Transactions ───────────────────────────

/** `GET transactions` returns an envelope, not a bare array. */
@Serializable
data class TransactionsPageDto(
    val transactions: List<TransactionDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)

@Serializable
data class TransactionDto(
    val id: Long,
    val user_id: Long = 0,
    val account_id: Long,
    val category_id: Long? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val date: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val to_account_id: Long? = null,
    val transfer_pair_id: String? = null,
    val debt_id: Long? = null,
    val mood: String? = null,
    val source: String? = null,
    val family_id: Long? = null,
    // Joined by the list query
    val account_name: String? = null,
    val account_color: String? = null,
    val account_role: String? = null,
    val to_account_name: String? = null,
    val category_name: String? = null,
    val category_color: String? = null,
    val category_icon: String? = null
)

@Serializable
data class CreateTransactionRequestDto(
    val account_id: Long,
    val category_id: Long? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val date: String,
    val mood: String? = null,
    val source: String = "android"
)

@Serializable
data class UpdateTransactionRequestDto(
    val account_id: Long,
    val category_id: Long? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val date: String,
    val mood: String? = null
)

// ─────────────────────────── Transfers ───────────────────────────

@Serializable
data class CreateTransferRequestDto(
    val from_account_id: Long,
    val to_account_id: Long,
    val amount: Double,
    val date: String,
    val description: String? = null
)

/** `POST transfers` returns the pair plus the shared pair id. */
@Serializable
data class TransferResultDto(
    val transfer_pair_id: String? = null,
    val from_transaction: TransactionDto? = null,
    val to_transaction: TransactionDto? = null
)

// ─────────────────────────── Categories ───────────────────────────

@Serializable
data class CategoryDto(
    val id: Long,
    val user_id: Long = 0,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val parent_id: Long? = null,
    val is_system: Int = 0,
    val family_id: Long? = null,
    val created_at: String? = null,
    /** Present on some list responses. */
    val type: String? = null
)

@Serializable
data class CreateCategoryRequestDto(
    val name: String,
    val type: String? = null,
    val parent_id: Long? = null,
    val color: String? = null,
    val icon: String? = null
)

@Serializable
data class UpdateCategoryRequestDto(
    val name: String? = null,
    val parent_id: Long? = null,
    val color: String? = null,
    val icon: String? = null
)

// ─────────────────────────── Budgets ───────────────────────────

@Serializable
data class BudgetDto(
    val id: Long,
    val user_id: Long = 0,
    val category_id: Long,
    val amount: Double,
    val period_type: String,
    val period_start: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    // Joined by the list query
    val category_name: String? = null,
    val category_color: String? = null,
    val category_icon: String? = null,
    val spent: Double = 0.0
)

@Serializable
data class CreateBudgetRequestDto(
    val category_id: Long,
    val amount: Double,
    val period_type: String
)

@Serializable
data class UpdateBudgetRequestDto(
    val category_id: Long? = null,
    val amount: Double? = null,
    val period_type: String? = null
)

@Serializable
data class BudgetVsActualDto(
    val period_type: String? = null,
    val total_budget: Double = 0.0,
    val total_spent: Double = 0.0,
    val categories: List<BudgetVsActualItemDto> = emptyList()
)

@Serializable
data class BudgetVsActualItemDto(
    val category_id: Long,
    val category_name: String? = null,
    val budget: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentage: Double = 0.0
)

// ─────────────────────────── Goals ───────────────────────────

@Serializable
data class GoalDto(
    val id: Long,
    val user_id: Long = 0,
    val name: String,
    val target_amount: Double,
    val current_amount: Double = 0.0,
    val target_date: String? = null,
    val icon: String? = null,
    val linked_account_id: Long? = null,
    val goal_id: Long? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val start_date: String? = null,
    val period_type: String? = null
)

@Serializable
data class CreateGoalRequestDto(
    val name: String,
    val target_amount: Double,
    val current_amount: Double = 0.0,
    val start_date: String? = null,
    val target_date: String? = null,
    val period_type: String? = null,
    val linked_account_id: Long? = null,
    val icon: String? = null
)

@Serializable
data class UpdateGoalRequestDto(
    val name: String? = null,
    val target_amount: Double? = null,
    val current_amount: Double? = null,
    val target_date: String? = null,
    val linked_account_id: Long? = null,
    val icon: String? = null
)

@Serializable
data class AddGoalFundsRequestDto(
    val amount: Double,
    val account_id: Long
)

// ─────────────────────────── Debts ───────────────────────────

@Serializable
data class DebtDto(
    val id: Long,
    val user_id: Long = 0,
    val name: String,
    val amount: Double,
    val remaining_amount: Double = 0.0,
    val due_date: String? = null,
    val type: String = "borrow",
    val status: String = "active",
    val notes: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class CreateDebtRequestDto(
    val name: String,
    val amount: Double,
    val due_date: String? = null,
    val type: String = "borrow",
    val notes: String? = null
)

@Serializable
data class UpdateDebtRequestDto(
    val name: String? = null,
    val amount: Double? = null,
    val due_date: String? = null,
    val type: String? = null,
    val notes: String? = null
)

@Serializable
data class PayDebtRequestDto(
    val amount: Double,
    val account_id: Long? = null,
    val create_transaction: Boolean = false,
    val date: String? = null
)

// ─────────────────────────── Scheduled ───────────────────────────

@Serializable
data class ScheduledDto(
    val id: Long,
    val user_id: Long = 0,
    val account_id: Long,
    val category_id: Long? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val frequency: String = "monthly",
    val next_date: String? = null,
    val is_active: Int = 1,
    val created_at: String? = null
)

@Serializable
data class CreateScheduledRequestDto(
    val account_id: Long,
    val category_id: Long? = null,
    val amount: Double,
    val type: String,
    val description: String? = null,
    val frequency: String = "monthly",
    val next_date: String? = null
)

@Serializable
data class UpdateScheduledRequestDto(
    val account_id: Long? = null,
    val category_id: Long? = null,
    val amount: Double? = null,
    val type: String? = null,
    val description: String? = null,
    val frequency: String? = null,
    val next_date: String? = null,
    val is_active: Int? = null
)

// ─────────────────────────── Notifications ───────────────────────────

@Serializable
data class NotificationDto(
    val id: Long,
    val user_id: Long = 0,
    val title: String,
    val body: String? = null,
    val type: String = "info",
    val link: String? = null,
    val is_read: Int = 0,
    val created_at: String? = null
)

// ─────────────────────────── Settings ───────────────────────────

@Serializable
data class UserSettingsDto(
    val user_id: Long = 0,
    val currency: String = "IDR",
    val date_format: String? = null,
    val language: String = "id",
    val timezone: String = "Asia/Jakarta",
    val theme: String = "light",
    val has_seen_tutorial: Int = 0,
    val animation_mode: String? = null,
    val animation_level: Int = 1
)

@Serializable
data class UpdateSettingsRequestDto(
    val currency: String? = null,
    val date_format: String? = null,
    val language: String? = null,
    val timezone: String? = null,
    val theme: String? = null,
    val animation_mode: String? = null,
    val animation_level: Int? = null
)

// ─────────────────────────── AI ───────────────────────────

@Serializable
data class ChatRequestDto(
    val message: String,
    val history: List<ChatMessageDto> = emptyList()
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponseDto(
    val response: String = "",
    val reply: String? = null,
    val actions: List<SuggestedActionDto> = emptyList()
)

@Serializable
data class SuggestedActionDto(
    val type: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class AiContextDto(
    val accounts: List<AccountDto> = emptyList(),
    val summary: Map<String, String> = emptyMap()
)

// ─────────────────────────── Gamification ───────────────────────────

@Serializable
data class AchievementDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val type: String? = null,
    val target: Int = 0,
    val progress: Int = 0,
    val achieved: Boolean = false,
    val achieved_at: String? = null
)

@Serializable
data class XpHistoryEntryDto(
    val id: Long,
    val amount: Int = 0,
    val source: String? = null,
    val source_id: String? = null,
    val created_at: String? = null
)

@Serializable
data class LeaderboardPageDto(
    val leaderboard: List<LeaderboardEntryDto> = emptyList(),
    val total: Int = 0,
    val currentUserRank: Int? = null
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int = 0,
    val user_id: Long,
    val name: String? = null,
    val xp: Int = 0,
    val level: Int = 1,
    val tier: String? = null
)

// ─────────────────────────── Reporting ───────────────────────────

@Serializable
data class ForecastDto(
    val forecast: List<ForecastPointDto> = emptyList(),
    val average_monthly_expense: Double = 0.0,
    val averageMonthlyExpense: Double? = null
)

@Serializable
data class ForecastPointDto(
    val month: String,
    val predicted_expense: Double = 0.0,
    val predictedExpense: Double? = null
)

@Serializable
data class HealthScoreDto(
    val score: Int = 0,
    val factors: Map<String, HealthFactorDto> = emptyMap()
)

@Serializable
data class HealthFactorDto(
    val score: Int = 0,
    val value: String? = null,
    val label: String? = null,
    val recommendation: String? = null
)

@Serializable
data class HeatmapDayDto(
    val date: String,
    val count: Int = 0,
    val total: Double = 0.0
)

@Serializable
data class MonthlyStatDto(
    val month: String,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val net: Double = 0.0
)

// ─────────────────────────── Family ───────────────────────────

@Serializable
data class FamilyGroupDto(
    val id: Long,
    val name: String,
    val created_by_user_id: Long = 0,
    val created_at: String? = null,
    val member_role: String? = null,
    val member_count: Int = 0,
    val members: List<FamilyMemberDto> = emptyList()
)

@Serializable
data class FamilyMemberDto(
    val id: Long = 0,
    val user_id: Long,
    val role: String = "member",
    val joined_at: String? = null,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class CreateFamilyRequestDto(
    val name: String
)

@Serializable
data class InviteFamilyMemberRequestDto(
    val email: String? = null
)

@Serializable
data class FamilyInviteDto(
    val code: String,
    val expires_at: String? = null
)

@Serializable
data class AcceptInviteRequestDto(
    val code: String
)

// ─────────────────────────── Shared accounts ───────────────────────────

@Serializable
data class AccountMembersDto(
    val account_role: String? = null,
    val family_id: Long? = null,
    val is_shared: Boolean = false,
    val owner: AccountOwnerDto? = null,
    val members: List<AccountMemberDto> = emptyList()
)

@Serializable
data class AccountOwnerDto(
    val user_id: Long,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class AccountMemberDto(
    val id: Long = 0,
    val user_id: Long,
    val role: String = "member",
    val joined_at: String? = null,
    val name: String? = null,
    val email: String? = null
)

@Serializable
data class ShareAccountRequestDto(
    val userId: Long,
    val role: String = "member",
    val family_id: Long? = null
)

@Serializable
data class RemoveMemberRequestDto(
    val userId: Long
)

// ─────────────────────────── Errors ───────────────────────────

@Serializable
data class ErrorResponseDto(
    val error: String? = null,
    val code: String? = null
)
