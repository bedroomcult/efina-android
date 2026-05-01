package com.efina.app.data.repository

import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.*
import com.efina.app.data.remote.interceptor.TokenManager
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.*
import com.efina.app.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: EfinaApi,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<AuthToken> {
        return try {
            val response = api.login(LoginRequestDto(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(body.token)
                tokenManager.saveCsrfToken(body.csrfToken ?: "")
                tokenManager.saveUserId(body.user.id)
                tokenManager.saveUserEmail(body.user.email)
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun register(name: String, email: String, password: String): Result<AuthToken> {
        return try {
            val response = api.register(RegisterRequestDto(name, email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(body.token)
                tokenManager.saveCsrfToken(body.csrfToken ?: "")
                tokenManager.saveUserId(body.user.id)
                tokenManager.saveUserEmail(body.user.email)
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setPin(pin: String): Result<Unit> {
        return try {
            val response = api.setPin(PinRequestDto(pin))
            if (response.isSuccessful) {
                tokenManager.savePin(pin)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to set PIN"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPin(pin: String): Result<AuthToken> {
        return try {
            val response = api.verifyPin(PinRequestDto(pin))
            if (response.isSuccessful) {
                val body = response.body()!!
                tokenManager.saveToken(body.token)
                Result.success(body.toDomain())
            } else {
                Result.failure(Exception("Invalid PIN"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSessions(): Result<List<Session>> {
        return try {
            val response = api.getSessions()
            if (response.isSuccessful) {
                Result.success(response.body()!!.sessions.map { it.toDomain() })
            } else {
                Result.failure(Exception("Failed to get sessions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun isLoggedIn(): Flow<Boolean> {
        return preferencesManager.onboardingComplete.map { it }
    }
    
    override suspend fun logout() {
        tokenManager.clearAll()
    }
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : AccountRepository {
    
    override suspend fun getAccounts(): Result<List<Account>> {
        return try {
            val response = api.getAccounts()
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get accounts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createAccount(request: CreateAccountRequest): Result<Account> {
        return try {
            val response = api.createAccount(request.toDto())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account> {
        return try {
            val map = mutableMapOf<String, Any?>()
            request.name?.let { map["name"] = it }
            request.balance?.let { map["balance"] = it }
            request.description?.let { map["description"] = it }
            val response = api.updateAccount(id, map)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAccount(id: Long): Result<Unit> {
        return try {
            val response = api.deleteAccount(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : TransactionRepository {
    
    override suspend fun getTransactions(filter: TransactionFilter): Result<TransactionListResponse> {
        return try {
            val response = api.getTransactions(
                limit = filter.limit,
                offset = filter.offset,
                type = filter.type?.value,
                accountId = filter.accountId,
                categoryId = filter.categoryId,
                startDate = filter.startDate,
                endDate = filter.endDate,
                search = filter.search,
                includeSubcategories = if (filter.includeSubcategories) true else null
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get transactions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> {
        return try {
            val response = api.createTransaction(request.toDto())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create transaction"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateTransaction(id: Long, request: UpdateTransactionRequest): Result<Transaction> {
        return try {
            val map = mutableMapOf<String, Any?>()
            request.categoryId?.let { map["category_id"] = it }
            request.amount?.let { map["amount"] = it }
            request.description?.let { map["description"] = it }
            request.date?.let { map["date"] = it }
            request.mood?.let { map["mood"] = it }
            val response = api.updateTransaction(id, map)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update transaction"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteTransaction(id: Long): Result<Unit> {
        return try {
            val response = api.deleteTransaction(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete transaction"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createTransfer(request: CreateTransferRequest): Result<Transaction> {
        return try {
            val dto = CreateTransferRequestDto(
                fromAccountId = request.fromAccountId,
                toAccountId = request.toAccountId,
                amount = request.amount,
                date = request.date,
                description = request.description
            )
            val response = api.createTransfer(dto)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create transfer"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : CategoryRepository {
    
    override suspend fun getCategories(type: CategoryType?): Result<List<Category>> {
        return try {
            val response = api.getCategories(type?.value)
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get categories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createCategory(request: CreateCategoryRequest): Result<Category> {
        return try {
            val response = api.createCategory(request.toDto())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create category"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category> {
        return try {
            val map = mutableMapOf<String, Any?>()
            request.name?.let { map["name"] = it }
            request.color?.let { map["color"] = it }
            request.icon?.let { map["icon"] = it }
            val response = api.updateCategory(id, map)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update category"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCategory(id: Long): Result<Unit> {
        return try {
            val response = api.deleteCategory(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete category"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : BudgetRepository {
    
    override suspend fun getBudgets(): Result<List<Budget>> {
        return try {
            val response = api.getBudgets()
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get budgets"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createBudget(request: CreateBudgetRequest): Result<Budget> {
        return try {
            val response = api.createBudget(request.toDto())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget> {
        return try {
            val map = mutableMapOf<String, Any?>()
            request.amount?.let { map["amount"] = it }
            request.periodType?.let { map["period_type"] = it.value }
            val response = api.updateBudget(id, map)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBudget(id: Long): Result<Unit> {
        return try {
            val response = api.deleteBudget(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete budget"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : GoalRepository {
    
    override suspend fun getGoals(): Result<List<Goal>> {
        return try {
            val response = api.getGoals()
            if (response.isSuccessful) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get goals"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createGoal(request: CreateGoalRequest): Result<Goal> {
        return try {
            val response = api.createGoal(request.toDto())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to create goal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal> {
        return try {
            val map = mutableMapOf<String, Any?>()
            request.name?.let { map["name"] = it }
            request.targetAmount?.let { map["target_amount"] = it }
            request.currentAmount?.let { map["current_amount"] = it }
            request.targetDate?.let { map["target_date"] = it }
            val response = api.updateGoal(id, map)
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update goal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteGoal(id: Long): Result<Unit> {
        return try {
            val response = api.deleteGoal(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete goal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : UserRepository {
    
    override suspend fun getUserProfile(): Result<UserProfile> {
        return try {
            val response = api.getUserStats()
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSettings(): Result<UserSettings> {
        return try {
            val response = api.getSettings()
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateSettings(settings: UserSettings): Result<UserSettings> {
        return try {
            val response = api.updateSettings(settings.toUpdateRequest())
            if (response.isSuccessful) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : ChatRepository {
    
    override suspend fun sendMessage(message: String, context: Map<String, Any>?): Result<ChatResponse> {
        return try {
            val response = api.chat(ChatRequestDto(message, context))
            if (response.isSuccessful) {
                val body = response.body()!!
                Result.success(ChatResponse(
                    response = body.response,
                    suggestedActions = body.suggestedActions?.map { 
                        SuggestedAction(it.type, it.data)
                    }
                ))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

