package com.efina.app.domain.repository

import com.efina.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthToken>
    suspend fun register(name: String, email: String, password: String): Result<AuthToken>
    suspend fun setPin(pin: String): Result<Unit>
    suspend fun verifyPin(pin: String): Result<AuthToken>
    suspend fun getSessions(): Result<List<Session>>
    fun isLoggedIn(): Flow<Boolean>
    suspend fun logout()
}

interface AccountRepository {
    suspend fun getAccounts(): Result<List<Account>>
    suspend fun createAccount(request: CreateAccountRequest): Result<Account>
    suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account>
    suspend fun deleteAccount(id: Long): Result<Unit>
}

interface TransactionRepository {
    suspend fun getTransactions(filter: TransactionFilter): Result<TransactionListResponse>
    suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction>
    suspend fun updateTransaction(id: Long, request: UpdateTransactionRequest): Result<Transaction>
    suspend fun deleteTransaction(id: Long): Result<Unit>
    suspend fun createTransfer(request: CreateTransferRequest): Result<Transaction>
}

interface CategoryRepository {
    suspend fun getCategories(type: CategoryType? = null): Result<List<Category>>
    suspend fun createCategory(request: CreateCategoryRequest): Result<Category>
    suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category>
    suspend fun deleteCategory(id: Long): Result<Unit>
}

interface BudgetRepository {
    suspend fun getBudgets(): Result<List<Budget>>
    suspend fun createBudget(request: CreateBudgetRequest): Result<Budget>
    suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget>
    suspend fun deleteBudget(id: Long): Result<Unit>
}

interface GoalRepository {
    suspend fun getGoals(): Result<List<Goal>>
    suspend fun createGoal(request: CreateGoalRequest): Result<Goal>
    suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal>
    suspend fun deleteGoal(id: Long): Result<Unit>
}

interface UserRepository {
    suspend fun getUserProfile(): Result<UserProfile>
    suspend fun getSettings(): Result<UserSettings>
    suspend fun updateSettings(settings: UserSettings): Result<UserSettings>
}

interface ChatRepository {
    suspend fun sendMessage(message: String, context: Map<String, Any>? = null): Result<ChatResponse>
}
