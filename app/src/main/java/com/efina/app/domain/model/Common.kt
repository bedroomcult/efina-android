package com.efina.app.domain.model

/**
 * Secondary domain models. Kept separate from MoneyModels.kt because these are
 * supporting features; the money path lives there.
 */

data class Transfer(
    val id: Long,
    val fromAccountId: Long,
    val fromAccountName: String?,
    val toAccountId: Long,
    val toAccountName: String?,
    val amount: Double,
    val description: String?,
    val date: String,
    val transferPairId: String?
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
    val status: String,
    val notes: String?
) {
    val isSettled: Boolean get() = remainingAmount <= 0.0
    val progress: Float
        get() = if (amount <= 0.0) 0f
        else (((amount - remainingAmount) / amount) * 100.0).toFloat().coerceIn(0f, 100f)
}

/**
 * `type` records which side owes. The backend stores 'borrow' and 'lend'.
 */
enum class DebtType(val value: String, val label: String) {
    BORROW("borrow", "I owe"),
    LEND("lend", "Owed to me");

    companion object {
        fun from(value: String?): DebtType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: BORROW
    }
}

data class CreateDebtRequest(
    val name: String,
    val amount: Double,
    val dueDate: String? = null,
    val type: DebtType = DebtType.BORROW,
    val notes: String? = null
)

data class UpdateDebtRequest(
    val name: String? = null,
    val amount: Double? = null,
    val dueDate: String? = null,
    val type: DebtType? = null,
    val notes: String? = null
)

data class PayDebtRequest(
    val amount: Double,
    val accountId: Long? = null,
    val createTransaction: Boolean = false,
    val date: String? = null
)

data class ScheduledTransaction(
    val id: Long,
    val accountId: Long,
    val categoryId: Long?,
    val amount: Double,
    val type: TransactionType,
    val description: String?,
    val frequency: String,
    val nextDate: String?,
    val isActive: Boolean
)

data class CreateScheduledRequest(
    val accountId: Long,
    val categoryId: Long? = null,
    val amount: Double,
    val type: TransactionType,
    val description: String? = null,
    val frequency: String = "monthly",
    val nextDate: String? = null
)

data class HealthScore(
    val score: Int,
    val factors: Map<String, HealthFactor>
)

data class HealthFactor(
    val score: Int,
    val value: String?,
    val label: String?,
    val recommendation: String?
)

data class Forecast(
    val points: List<ForecastPoint>,
    val averageMonthlyExpense: Double
)

data class ForecastPoint(
    val month: String,
    val predictedExpense: Double
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatResponse(
    val text: String,
    val actions: List<SuggestedAction> = emptyList()
)

data class SuggestedAction(
    val type: String,
    val data: Map<String, String> = emptyMap()
)

data class UserSettings(
    val currency: String,
    val dateFormat: String?,
    val language: String,
    val timezone: String,
    val theme: String,
    val hasSeenTutorial: Boolean,
    val animationMode: String?,
    val animationLevel: Int
)

data class UpdateSettingsRequest(
    val currency: String? = null,
    val dateFormat: String? = null,
    val language: String? = null,
    val timezone: String? = null,
    val theme: String? = null,
    val animationMode: String? = null,
    val animationLevel: Int? = null
)
