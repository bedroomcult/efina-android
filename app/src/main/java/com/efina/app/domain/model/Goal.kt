package com.efina.app.domain.model

data class Goal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val startDate: String,
    val targetDate: String,
    val periodType: GoalPeriodType,
    val linkedAccountId: Long?,
    val accountName: String?,
    val totalPeriods: Int,
    val remainingPeriods: Int,
    val remainingAmount: Double,
    val suggestedSaving: Double
)

enum class GoalPeriodType(val value: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    MONTHLY("monthly")
}

data class CreateGoalRequest(
    val name: String,
    val targetAmount: Double,
    val startDate: String,
    val targetDate: String,
    val periodType: GoalPeriodType,
    val linkedAccountId: Long? = null
)

data class UpdateGoalRequest(
    val name: String? = null,
    val targetAmount: Double? = null,
    val currentAmount: Double? = null,
    val targetDate: String? = null
)
