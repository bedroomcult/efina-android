package com.efina.app.domain.model

/**
 * Domain models for the money path.
 *
 * These are shaped for the UI, but every field is derived from a real backend
 * field. Constraints that come from the schema and are easy to get wrong:
 *
 *  - `categories` has NO `type` column. A category is income or expense only
 *    through the transaction that uses it, so [Category] carries no type.
 *  - `accounts.type` is one of Cash / Bank / E-Wallet / Credit / Other, matching
 *    CreateAccountSchema in src/lib/validations.ts.
 *  - `transactions` include an `adjustment` type produced by balance
 *    recalculations; it is not user-creatable but must render.
 */

// ─────────────────────────── Accounts ───────────────────────────

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "IDR",
    val description: String? = null,
    val color: String? = null,
    val displayOrder: Int = 0,
    val disabled: Boolean = false,
    val initialBalance: Double = 0.0,
    val createdAt: String? = null,
    // Sharing
    val isShared: Boolean = false,
    val familyId: Long? = null,
    val role: AccountShareRole? = null,
    val memberCount: Int = 0,
    val ownerUserId: Long = 0
) {
    /** Only an admin may edit, delete or re-share the account. */
    val canAdmin: Boolean get() = role == null || role == AccountShareRole.ADMIN
    /** A viewer can see the account and its transactions but not write to it. */
    val canWrite: Boolean get() = canAdmin || role == AccountShareRole.MEMBER
}

enum class AccountType(val displayName: String, val value: String) {
    CASH("Cash", "Cash"),
    BANK("Bank", "Bank"),
    E_WALLET("E-Wallet", "E-Wallet"),
    CREDIT("Credit", "Credit"),
    OTHER("Other", "Other");

    companion object {
        fun from(value: String?): AccountType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: BANK
    }
}

enum class AccountShareRole(val value: String) {
    ADMIN("admin"),
    MEMBER("member"),
    VIEWER("viewer");

    companion object {
        fun from(value: String?): AccountShareRole? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

data class CreateAccountRequest(
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val currency: String = "IDR",
    val description: String? = null,
    val color: String? = null,
    val familyId: Long? = null
)

data class UpdateAccountRequest(
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String,
    val description: String? = null,
    val color: String? = null
)

// ─────────────────────────── Transactions ───────────────────────────

data class Transaction(
    val id: Long,
    val userId: Long,
    val accountId: Long,
    val accountName: String?,
    val accountRole: AccountShareRole? = null,
    /** Currency of the account this row belongs to. */
    val currency: String = "IDR",
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
    val familyId: Long? = null,
    val createdAt: String?
)

enum class TransactionType(val value: String) {
    INCOME("income"),
    EXPENSE("expense"),
    TRANSFER("transfer"),
    ADJUSTMENT("adjustment");

    companion object {
        fun from(value: String?): TransactionType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: EXPENSE
    }
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
    val accountId: Long,
    val categoryId: Long? = null,
    val amount: Double,
    val type: TransactionType,
    val description: String? = null,
    val date: String,
    val mood: String? = null
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

// ─────────────────────────── Categories ───────────────────────────

data class Category(
    val id: Long,
    val name: String,
    val color: String,
    val icon: String?,
    val parentId: Long?,
    val parentName: String? = null,
    val isSystem: Boolean = false
)

data class CreateCategoryRequest(
    val name: String,
    val color: String,
    val icon: String? = null,
    val parentId: Long? = null
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val parentId: Long? = null
)

// ─────────────────────────── Budgets ───────────────────────────

data class Budget(
    val id: Long,
    val categoryId: Long,
    val categoryName: String?,
    val categoryColor: String? = null,
    val amount: Double,
    val spent: Double,
    val periodType: PeriodType,
    val periodStart: String? = null
) {
    val remaining: Double get() = amount - spent
    val percentage: Float
        get() = if (amount <= 0.0) 0f else ((spent / amount) * 100.0).toFloat().coerceIn(0f, 999f)
    val isOverBudget: Boolean get() = spent > amount
}

enum class PeriodType(val value: String, val label: String) {
    WEEKLY("weekly", "Weekly"),
    MONTHLY("monthly", "Monthly"),
    YEARLY("yearly", "Yearly");

    companion object {
        fun from(value: String?): PeriodType =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: MONTHLY
    }
}

data class CreateBudgetRequest(
    val categoryId: Long,
    val amount: Double,
    val periodType: PeriodType
)

data class UpdateBudgetRequest(
    val categoryId: Long? = null,
    val amount: Double? = null,
    val periodType: PeriodType? = null
)

// ─────────────────────────── Goals ───────────────────────────

data class Goal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val startDate: String?,
    val targetDate: String?,
    val periodType: String?,
    val linkedAccountId: Long?,
    val accountName: String? = null,
    val icon: String? = null
) {
    val progressPercentage: Float
        get() = if (targetAmount <= 0.0) 0f
        else ((currentAmount / targetAmount) * 100.0).toFloat().coerceIn(0f, 100f)
    val isComplete: Boolean get() = currentAmount >= targetAmount
}

data class CreateGoalRequest(
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val startDate: String? = null,
    val targetDate: String? = null,
    val periodType: String? = null,
    val linkedAccountId: Long? = null,
    val icon: String? = null
)

data class UpdateGoalRequest(
    val name: String? = null,
    val targetAmount: Double? = null,
    val currentAmount: Double? = null,
    val targetDate: String? = null,
    val linkedAccountId: Long? = null,
    val icon: String? = null
)
