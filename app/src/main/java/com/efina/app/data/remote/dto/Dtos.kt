package com.efina.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============ Standard Wrapper ============

data class ApiListResponse<T>(
    @SerializedName("total_count")
    val totalCount: Int
)

// ============ Auth DTOs ============

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)

data class PinRequestDto(
    val pin: String
)

data class AuthResponseDto(
    val token: String,
    val user: UserDto
)

data class UserDto(
    val id: Long,
    val email: String,
    val name: String
)

// Account wrapper after creation
data class AccountCreatedDto(
    val account: AccountDto
)

// ============ Account DTOs ============

data class AccountsListDto(
    val accounts: List<AccountDto>,
    @SerializedName("total_count")
    val totalCount: Int
)

data class AccountDto(
    val id: Long,
    val name: String,
    val type: String,
    val balance: Double,
    val currency: String?,
    val description: String?,
    val color: String?,
    val icon: String?,
    @SerializedName("display_order")
    val displayOrder: Int?,
    @SerializedName("include_in_total")
    val includeInTotal: Boolean?,
    @SerializedName("show_in_dashboard")
    val showInDashboard: Boolean?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class CreateAccountRequestDto(
    val name: String,
    val type: String,
    val balance: Double?,
    val currency: String?,
    val description: String?,
    val color: String?,
    val icon: String?,
    @SerializedName("include_in_total")
    val includeInTotal: Boolean?,
    @SerializedName("show_in_dashboard")
    val showInDashboard: Boolean?
)

// ============ Transaction DTOs ============

data class TransactionsListDto(
    val transactions: List<TransactionDto>,
    @SerializedName("total_count")
    val totalCount: Int
)

data class TransactionDto(
    val id: Long,
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
    @SerializedName("xp_gained")
    val xpGained: Int?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class TransactionCreatedDto(
    val transaction: TransactionDto,
    @SerializedName("xp_gained")
    val xpGained: Int?
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
    val mood: String?,
    @SerializedName("is_recurring")
    val isRecurring: Boolean? = null
)

// ============ Category DTOs ============

data class CategoriesListDto(
    val categories: List<CategoryDto>
)

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

data class BudgetsListDto(
    val budgets: List<BudgetDto>
)

data class BudgetDto(
    val id: Long,
    @SerializedName("category_id")
    val categoryId: Long,
    @SerializedName("category_name")
    val categoryName: String?,
    val amount: Double,
    val spent: Double,
    val remaining: Double,
    @SerializedName("period_type")
    val periodType: String,
    @SerializedName("period_start")
    val periodStart: String,
    @SerializedName("period_end")
    val periodEnd: String?
)

data class CreateBudgetRequestDto(
    @SerializedName("category_id")
    val categoryId: Long,
    val amount: Double,
    @SerializedName("period_type")
    val periodType: String
)

// ============ Goal DTOs ============

data class GoalsListDto(
    val goals: List<GoalDto>
)

data class GoalDto(
    val id: Long,
    val name: String,
    @SerializedName("target_amount")
    val targetAmount: Double,
    @SerializedName("current_amount")
    val currentAmount: Double,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("target_date")
    val targetDate: String,
    @SerializedName("period_type")
    val periodType: String?,
    @SerializedName("linked_account_id")
    val linkedAccountId: Long?,
    @SerializedName("account_name")
    val accountName: String?,
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    val status: String?
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

// ============ Dashboard DTO ============

data class DashboardDto(
    val accounts: List<AccountDto>?,
    @SerializedName("recent_transactions")
    val recentTransactions: List<TransactionDto>?,
    val budgets: List<BudgetDto>?,
    val goals: List<GoalDto>?,
    @SerializedName("health_score")
    val healthScore: HealthScoreDto?
)

data class HealthScoreDto(
    val score: Int,
    val grade: String?,
    val factors: Map<String, HealthFactorDto>?
)

data class HealthFactorDto(
    val score: Int,
    val weight: Double?
)

// ============ Settings DTOs ============

data class UserSettingsDto(
    val theme: String?,
    val language: String?,
    val currency: String?,
    val timezone: String?
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
    val reply: String,
    @SerializedName("action_proposal")
    val actionProposal: String?
)

// ============ XP DTO ============

data class XpSummaryDto(
    val xp: Int?,
    val level: Int?,
    val tier: String?,
    @SerializedName("active_streak")
    val activeStreak: Int?,
    @SerializedName("total_transactions")
    val totalTransactions: Int?
)

// ============ Achievements ============

data class AchievementsListDto(
    val achievements: List<AchievementDto>
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

// ============ Forecast ============

data class ForecastDto(
    val forecast: ForecastDataDto
)

data class ForecastDataDto(
    @SerializedName("monthly_average")
    val monthlyAverage: Double?,
    @SerializedName("projected_balance")
    val projectedBalance: Double?,
    @SerializedName("next_month_income")
    val nextMonthIncome: Double?,
    @SerializedName("next_month_expenses")
    val nextMonthExpenses: Double?,
    val projection: List<ProjectionDto>?,
    val alerts: List<String>?
)

data class ProjectionDto(
    val month: String,
    val income: Double,
    val expenses: Double,
    val balance: Double
)

// ============ Error DTO ============

data class ErrorResponseDto(
    val error: String,
    val code: String?
)
