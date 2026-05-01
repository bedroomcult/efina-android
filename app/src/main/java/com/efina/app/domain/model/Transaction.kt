package com.efina.app.domain.model

data class Transaction(
    val id: Long,
    val userId: Long,
    val accountId: Long,
    val accountName: String?,
    val categoryId: Long?,
    val categoryName: String?,
    val categoryColor: String?,
    val categoryIcon: String?,
    val toAccountId: Long?,
    val toAccountName: String?,
    val amount: Double,
    val type: TransactionType,
    val description: String?,
    val date: String,
    val mood: String?,
    val transferPairId: String?,
    val createdAt: String?
)

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer"),
    ADJUSTMENT("adjustment")
}

data class CreateTransactionRequest(
    val accountId: Long,
    val categoryId: Long?,
    val amount: Double,
    val type: TransactionType,
    val description: String? = null,
    val date: String,
    val mood: String? = null
)

data class UpdateTransactionRequest(
    val categoryId: Long? = null,
    val amount: Double? = null,
    val description: String? = null,
    val date: String? = null,
    val mood: String? = null
)

data class TransactionListResponse(
    val transactions: List<Transaction>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class TransactionFilter(
    val limit: Int = 20,
    val offset: Int = 0,
    val type: TransactionType? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val search: String? = null,
    val includeSubcategories: Boolean = false
)
