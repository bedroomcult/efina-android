package com.efina.app.domain.model

data class HealthScore(
    val score: Int,
    val factors: Map<String, HealthFactor>
)

data class HealthFactor(
    val score: Int,
    val value: String
)

data class Forecast(
    val forecast: List<MonthlyForecast>,
    val averageMonthlyExpense: Double
)

data class MonthlyForecast(
    val month: String,
    val predictedExpense: Double
)

data class Transfer(
    val id: Long,
    val fromAccountId: Long,
    val fromAccountName: String,
    val toAccountId: Long,
    val toAccountName: String,
    val amount: Double,
    val description: String?,
    val date: String
)

data class CreateTransferRequest(
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val date: String,
    val description: String? = null
)

data class Debt(
    val id: Long,
    val name: String,
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: String?,
    val type: DebtType,
    val notes: String?
)

enum class DebtType(val value: String) {
    LEND("lend"),
    BORROW("borrow")
}

data class CreateDebtRequest(
    val name: String,
    val amount: Double,
    val dueDate: String? = null,
    val type: DebtType,
    val notes: String? = null
)

data class Notification(
    val id: Long,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: String,
    val data: Map<String, String>?
)
