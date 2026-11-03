package com.efina.app.domain.repository

import com.efina.app.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts. Each maps 1:1 onto endpoints that exist in
 * functions/api/, verified by tools/check-api-contract.mjs.
 */

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, password: String): Result<User>
    /** Restores the session from encrypted storage without a network call. */
    fun currentUserFlow(): Flow<User?>
    suspend fun refreshCurrentUser(): Result<User>

    /**
     * Re-issues the CSRF token. Required because CSRF JWTs expire in 24 h while
     * the session lasts 7 days; without it every write 403s after a day.
     */
    suspend fun refreshCsrfToken(): Result<Unit>
    suspend fun logout(): Result<Unit>
    fun isLoggedIn(): Boolean
}

interface AccountRepository {
    suspend fun getAccounts(): Result<List<Account>>
    suspend fun createAccount(request: CreateAccountRequest): Result<Account>
    suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account>
    suspend fun deleteAccount(id: Long): Result<Unit>
    suspend fun setShared(id: Long, familyId: Long?): Result<Account>
    suspend fun getMembers(id: Long): Result<AccountMembers>
    suspend fun shareWith(id: Long, userId: Long, role: AccountShareRole): Result<Unit>
    suspend fun removeMember(id: Long, userId: Long): Result<Unit>
}

data class AccountMembers(
    val role: AccountShareRole?,
    val owner: FamilyMember?,
    val members: List<AccountMember>
)

interface TransactionRepository {
    suspend fun getTransactions(filter: TransactionFilter): Result<TransactionPage>
    suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction>
    suspend fun updateTransaction(id: Long, request: UpdateTransactionRequest): Result<Transaction>
    suspend fun deleteTransaction(id: Long): Result<Unit>
    suspend fun createTransfer(request: CreateTransferRequest): Result<Unit>
}

data class TransactionPage(
    val transactions: List<Transaction>,
    val total: Int,
    val limit: Int,
    val offset: Int
) {
    val hasMore: Boolean get() = offset + transactions.size < total
}

interface CategoryRepository {
    suspend fun getCategories(): Result<List<Category>>
    suspend fun createCategory(request: CreateCategoryRequest): Result<Category>
    suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category>
    suspend fun deleteCategory(id: Long): Result<Unit>
}

interface BudgetRepository {
    suspend fun getBudgets(): Result<List<Budget>>
    suspend fun createBudget(request: CreateBudgetRequest): Result<Budget>
    suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget>
    suspend fun deleteBudget(id: Long): Result<Unit>
    /** Share with a family, or pass null to make it personal again. */
    suspend fun setBudgetSharing(id: Long, familyId: Long?): Result<Budget>
}

interface GoalRepository {
    suspend fun getGoals(): Result<List<Goal>>
    suspend fun createGoal(request: CreateGoalRequest): Result<Goal>
    suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal>
    suspend fun deleteGoal(id: Long): Result<Unit>
    suspend fun addFunds(id: Long, amount: Double, accountId: Long): Result<Unit>
}

interface DebtRepository {
    suspend fun getDebts(): Result<List<Debt>>
    suspend fun createDebt(request: CreateDebtRequest): Result<Debt>
    suspend fun updateDebt(id: Long, request: UpdateDebtRequest): Result<Debt>
    suspend fun deleteDebt(id: Long): Result<Unit>
    suspend fun payDebt(id: Long, request: PayDebtRequest): Result<Unit>
}

interface ScheduledRepository {
    suspend fun getScheduled(): Result<List<ScheduledTransaction>>
    suspend fun createScheduled(request: CreateScheduledRequest): Result<ScheduledTransaction>
    suspend fun deleteScheduled(id: Long): Result<Unit>
}

interface SettingsRepository {
    suspend fun getSettings(): Result<UserSettings>
    suspend fun updateSettings(request: UpdateSettingsRequest): Result<UserSettings>
    suspend fun changePassword(current: String, next: String): Result<Unit>
}

interface InsightsRepository {
    suspend fun getAchievements(): Result<List<Achievement>>
    suspend fun getLeaderboard(limit: Int = 20): Result<List<LeaderboardEntry>>
    suspend fun getForecast(): Result<Forecast>
    suspend fun getHealthScore(): Result<HealthScore>
}

interface FamilyRepository {
    suspend fun getFamilies(): Result<List<FamilyGroup>>
    suspend fun createFamily(name: String): Result<FamilyGroup>
    suspend fun getFamily(id: Long): Result<FamilyGroup>
    suspend fun createInvite(id: Long): Result<String>
    suspend fun acceptInvite(code: String): Result<FamilyGroup>
    suspend fun removeMember(id: Long, userId: Long): Result<Unit>
}

interface ChatRepository {
    suspend fun send(message: String, history: List<ChatMessage>): Result<ChatResponse>
}
