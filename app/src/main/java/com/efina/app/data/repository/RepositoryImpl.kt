package com.efina.app.data.repository

import com.efina.app.data.remote.SessionEvents
import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.*
import com.efina.app.data.remote.interceptor.TokenManager
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a Retrofit response into a Result, surfacing the backend's `error` string.
 * Every handler in functions/api/ returns `{ error: "..." }` on failure, so the
 * user sees the real reason instead of "HTTP 400".
 */
internal fun <T> Response<T>.toResult(): Result<T> {
    // A 401 anywhere means the session is dead. Announce it centrally so every
    // screen logs out, not just whichever one made this call.
    if (code() == 401) {
        SessionEvents.emitUnauthorized()
    }

    if (isSuccessful) {
        val body = body()
        return if (body != null) Result.success(body)
        else Result.failure(ApiException(code(), "Empty response"))
    }
    val raw = errorBody()?.string().orEmpty()
    val message = runCatching {
        errorJson.decodeFromString<ErrorResponseDto>(raw).error
    }.getOrNull() ?: raw.takeIf { it.isNotBlank() } ?: "Request failed (${code()})"
    return Result.failure(ApiException(code(), message))
}

internal class ApiException(val status: Int, override val message: String) : Exception(message)

/** Shared reader for error bodies, so a failure does not allocate a Json each time. */
private val errorJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: EfinaApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    private val userState = MutableStateFlow<User?>(null)

    override suspend fun login(email: String, password: String): Result<User> =
        api.login(LoginRequestDto(email.trim(), password))
            .toResult()
            .onSuccess { persist(it) }
            .map { it.user.toDomain() }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        api.register(RegisterRequestDto(email.trim(), password, name.ifBlank { null }))
            .toResult()
            .onSuccess { persist(it) }
            .map { it.user.toDomain() }

    private fun persist(body: AuthResponseDto) {
        tokenManager.saveSession(
            token = body.token,
            csrfToken = body.csrfToken,
            userId = body.user.id,
            email = body.user.email
        )
        userState.value = body.user.toDomain()
    }

    override fun currentUserFlow(): Flow<User?> = userState.asStateFlow()

    override suspend fun refreshCurrentUser(): Result<User> =
        api.getCurrentUser().toResult().map { it.toDomain() }
            .onSuccess { userState.value = it }

    override suspend fun logout(): Result<Unit> {
        // DELETE auth/sessions invalidates the session row; ignore failures so a
        // dead network cannot trap the user in a logged-in shell.
        val result = runCatching { api.logout() }.getOrNull()
        tokenManager.clearAll()
        userState.value = null
        return if (result != null && result.isSuccessful) Result.success(Unit)
        else Result.success(Unit)
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : AccountRepository {

    override suspend fun getAccounts(): Result<List<Account>> =
        api.getAccounts().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createAccount(request: CreateAccountRequest): Result<Account> =
        api.createAccount(request.toDto()).toResult().map { it.toDomain() }

    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account> =
        api.updateAccount(id, request.toDto()).toResult().map { it.toDomain() }

    override suspend fun deleteAccount(id: Long): Result<Unit> =
        api.deleteAccount(id).toResult().map { }

    override suspend fun setShared(id: Long, familyId: Long?): Result<Account> =
        api.updateAccountSharing(id, UpdateAccountSharingRequestDto(familyId))
            .toResult().map { it.toDomain() }

    override suspend fun getMembers(id: Long): Result<AccountMembers> =
        api.getAccountMembers(id).toResult().map { dto ->
            AccountMembers(
                role = AccountShareRole.from(dto.account_role),
                owner = dto.owner?.let {
                    FamilyMember(it.user_id, "admin", it.name ?: "", it.email, null)
                },
                members = dto.members.map { it.toDomain() }
            )
        }

    override suspend fun shareWith(id: Long, userId: Long, role: AccountShareRole): Result<Unit> =
        api.shareAccount(id, ShareAccountRequestDto(userId, role.value)).toResult().map { }

    override suspend fun removeMember(id: Long, userId: Long): Result<Unit> =
        api.unshareAccount(id, RemoveMemberRequestDto(userId)).toResult().map { }
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : TransactionRepository {

    override suspend fun getTransactions(filter: TransactionFilter): Result<TransactionPage> =
        api.getTransactions(
            limit = filter.limit,
            offset = filter.offset,
            type = filter.type?.value,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            startDate = filter.startDate,
            endDate = filter.endDate,
            search = filter.search?.takeIf { it.isNotBlank() },
            includeSubcategories = filter.includeSubcategories
        ).toResult().map { page ->
            TransactionPage(
                transactions = page.transactions.map { it.toDomain() },
                total = page.total,
                limit = page.limit,
                offset = page.offset
            )
        }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> =
        api.createTransaction(request.toDto()).toResult().map { it.toDomain() }

    override suspend fun updateTransaction(id: Long, request: UpdateTransactionRequest): Result<Transaction> =
        api.updateTransaction(id, request.toDto()).toResult().map { it.toDomain() }

    override suspend fun deleteTransaction(id: Long): Result<Unit> =
        api.deleteTransaction(id).toResult().map { }

    override suspend fun createTransfer(request: CreateTransferRequest): Result<Unit> =
        api.createTransfer(
            CreateTransferRequestDto(
                from_account_id = request.fromAccountId,
                to_account_id = request.toAccountId,
                amount = request.amount,
                date = request.date,
                description = request.description
            )
        ).toResult().map { }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        api.getCategories().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createCategory(request: CreateCategoryRequest): Result<Category> =
        api.createCategory(request.toDto()).toResult().map { it.toDomain() }

    override suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category> =
        api.updateCategory(id, request.toDto()).toResult().map { it.toDomain() }

    override suspend fun deleteCategory(id: Long): Result<Unit> =
        api.deleteCategory(id).toResult().map { }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : BudgetRepository {

    override suspend fun getBudgets(): Result<List<Budget>> =
        api.getBudgets().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createBudget(request: CreateBudgetRequest): Result<Budget> =
        api.createBudget(request.toDto()).toResult().map { it.toDomain() }

    override suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget> =
        api.updateBudget(id, request.toDto()).toResult().map { it.toDomain() }

    override suspend fun deleteBudget(id: Long): Result<Unit> =
        api.deleteBudget(id).toResult().map { }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> =
        api.getGoals().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createGoal(request: CreateGoalRequest): Result<Goal> =
        api.createGoal(request.toDto()).toResult().map { it.toDomain() }

    override suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal> =
        api.updateGoal(id, request.toDto()).toResult().map { it.toDomain() }

    override suspend fun deleteGoal(id: Long): Result<Unit> =
        api.deleteGoal(id).toResult().map { }

    override suspend fun addFunds(id: Long, amount: Double, accountId: Long): Result<Goal> =
        api.addGoalFunds(id, AddGoalFundsRequestDto(amount, accountId))
            .toResult().map { it.toDomain() }
}

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : DebtRepository {

    override suspend fun getDebts(): Result<List<Debt>> =
        api.getDebts().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createDebt(request: CreateDebtRequest): Result<Debt> =
        api.createDebt(
            CreateDebtRequestDto(
                name = request.name,
                amount = request.amount,
                due_date = request.dueDate,
                type = request.type.value,
                notes = request.notes
            )
        ).toResult().map { it.toDomain() }

    override suspend fun updateDebt(id: Long, request: UpdateDebtRequest): Result<Debt> =
        api.updateDebt(
            id,
            UpdateDebtRequestDto(
                name = request.name,
                amount = request.amount,
                due_date = request.dueDate,
                type = request.type?.value,
                notes = request.notes
            )
        ).toResult().map { it.toDomain() }

    override suspend fun deleteDebt(id: Long): Result<Unit> =
        api.deleteDebt(id).toResult().map { }

    override suspend fun payDebt(id: Long, request: PayDebtRequest): Result<Debt> =
        api.payDebt(
            id,
            PayDebtRequestDto(
                amount = request.amount,
                account_id = request.accountId,
                create_transaction = request.createTransaction,
                date = request.date
            )
        ).toResult().map { it.toDomain() }
}

@Singleton
class ScheduledRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : ScheduledRepository {

    override suspend fun getScheduled(): Result<List<ScheduledTransaction>> =
        api.getScheduled().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createScheduled(request: CreateScheduledRequest): Result<ScheduledTransaction> =
        api.createScheduled(
            CreateScheduledRequestDto(
                account_id = request.accountId,
                category_id = request.categoryId,
                amount = request.amount,
                type = request.type.value,
                description = request.description,
                frequency = request.frequency,
                next_date = request.nextDate
            )
        ).toResult().map { it.toDomain() }

    override suspend fun deleteScheduled(id: Long): Result<Unit> =
        api.deleteScheduled(id).toResult().map { }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : SettingsRepository {

    override suspend fun getSettings(): Result<UserSettings> =
        api.getSettings().toResult().map { it.toDomain() }

    override suspend fun updateSettings(request: UpdateSettingsRequest): Result<UserSettings> =
        api.updateSettings(
            UpdateSettingsRequestDto(
                currency = request.currency,
                date_format = request.dateFormat,
                language = request.language,
                timezone = request.timezone,
                theme = request.theme,
                animation_mode = request.animationMode,
                animation_level = request.animationLevel
            )
        ).toResult().map { it.toDomain() }

    override suspend fun changePassword(current: String, next: String): Result<Unit> =
        api.changePassword(ChangePasswordRequestDto(current, next)).toResult().map { }
}

@Singleton
class InsightsRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : InsightsRepository {

    override suspend fun getAchievements(): Result<List<Achievement>> =
        api.getAchievements().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntry>> =
        api.getLeaderboard(limit = limit).toResult()
            .map { page -> page.leaderboard.map { it.toDomain() } }

    override suspend fun getForecast(): Result<Forecast> =
        api.getForecast().toResult().map { dto ->
            Forecast(
                points = dto.forecast.map {
                    ForecastPoint(it.month, it.predicted_expense.takeIf { p -> p != 0.0 }
                        ?: (it.predictedExpense ?: 0.0))
                },
                averageMonthlyExpense = dto.average_monthly_expense.takeIf { it != 0.0 }
                    ?: (dto.averageMonthlyExpense ?: 0.0)
            )
        }

    override suspend fun getHealthScore(): Result<HealthScore> =
        api.getHealthScore().toResult().map { dto ->
            HealthScore(
                score = dto.score,
                factors = dto.factors.mapValues { (_, f) ->
                    HealthFactor(f.score, f.value, f.label, f.recommendation)
                }
            )
        }
}

@Singleton
class FamilyRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : FamilyRepository {

    override suspend fun getFamilies(): Result<List<FamilyGroup>> =
        api.getFamilies().toResult().map { list -> list.map { it.toDomain() } }

    override suspend fun createFamily(name: String): Result<FamilyGroup> =
        api.createFamily(CreateFamilyRequestDto(name)).toResult().map { it.toDomain() }

    override suspend fun getFamily(id: Long): Result<FamilyGroup> =
        api.getFamily(id).toResult().map { it.toDomain() }

    override suspend fun createInvite(id: Long): Result<String> =
        api.inviteFamilyMember(id, InviteFamilyMemberRequestDto()).toResult().map { it.code }

    override suspend fun acceptInvite(code: String): Result<FamilyGroup> =
        api.acceptFamilyInvite(AcceptInviteRequestDto(code.trim().uppercase()))
            .toResult().map { it.toDomain() }

    override suspend fun removeMember(id: Long, userId: Long): Result<Unit> =
        api.removeFamilyMember(id, RemoveMemberRequestDto(userId)).toResult().map { }
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : ChatRepository {

    override suspend fun send(message: String, history: List<ChatMessage>): Result<ChatResponse> =
        api.chat(
            ChatRequestDto(
                message = message,
                history = history.map { ChatMessageDto(it.role, it.content) }
            )
        ).toResult().map { dto ->
            ChatResponse(
                text = dto.response.ifBlank { dto.reply.orEmpty() },
                actions = dto.actions.map { SuggestedAction(it.type, it.data) }
            )
        }
}
