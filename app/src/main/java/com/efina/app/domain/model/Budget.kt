package com.efina.app.domain.model

data class Budget(
    val id: Long,
    val categoryId: Long,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val amount: Double,
    val periodType: PeriodType,
    val periodStart: String,
    val periodEnd: String?,
    val spent: Double,
    val percentage: Int
)

enum class PeriodType(val value: String) {
    WEEKLY("weekly"),
    MONTHLY("monthly"),
    YEARLY("yearly")
}

data class CreateBudgetRequest(
    val categoryId: Long,
    val amount: Double,
    val periodType: PeriodType
)

data class UpdateBudgetRequest(
    val amount: Double? = null,
    val periodType: PeriodType? = null
)
