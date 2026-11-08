package com.efina.app.data.repository

import com.efina.app.data.remote.DataEvents
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
 * Runs a network call and converts transport failures into a Result.
 *
 * `Response<T>.toResult()` can only map an HTTP response; Retrofit still THROWS
 * IOException on connect failure, timeout or DNS error, and every caller was a
 * bare `viewModelScope.launch { repo.x().onSuccess{} }`. An uncaught exception in
 * viewModelScope reaches the default handler and kills the process, so a cold
 * start with no network crashed the app instead of showing an error.
 *
 * Every repository method now goes through here, so no call site can forget.
 *
 * CSRF retry: when `retryable` is true and the call fails with a 403 CSRF error,
 * `onCsrfRefresh` is invoked (typically `refreshCsrfToken()`) and the block is
 * retried once. Only idempotent methods (GET/PUT/DELETE) should pass
 * `retryable = true`; POST writes are never auto-retried.
 */
internal suspend fun <T> safeCall(
    block: suspend () -> Response<T>,
    retryable: Boolean = false,
    onCsrfRefresh: suspend () -> Unit = {}
): Result<T> {
    val result = try {
        block().toResult()
    } catch (io: java.io.IOException) {
        Result.failure(
            ApiException(0, "No connection. Check your network and try again.")
        )
    } catch (e: Exception) {
        Result.failure(ApiException(0, e.message ?: "Unexpected error"))
    }

    if (retryable && result is Result.Failure) {
        val exception = result.exceptionOrNull()
        if (exception is ApiException && exception.status == 403 &&
            (exception.message ?: "").contains("CSRF", ignoreCase = true)) {
            onCsrfRefresh()
            return try {
                block().toResult()
            } catch (io: java.io.IOException) {
                Result.failure(
                    ApiException(0, "No connection. Check your network and try again.")
                )
            } catch (e: Exception) {
                Result.failure(ApiException(0, e.message ?: "Unexpected error"))
            }
        }
    }

    return result
}

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
    // The middleware answers a stale CSRF token with 403 "CSRF token invalid".
    // That is recoverable: refresh the token rather than making the user sign out.
    if (code() == 403 && raw.contains("CSRF", ignoreCase = true)) {
        SessionEvents.emitCsrfExpired()
    }
    val message = runCatching {
        errorJson.decodeFromString<ErrorResponseDto>(raw).error
    }.getOrNull() ?: raw.takeIf { it.isNotBlank() } ?: "Request failed (${code()})"
    return Result.failure(ApiException(code(), message))
}

internal class ApiException(val status: Int, override val message: String) : Exception(message)


/** Emits a refresh signal for [domain] when the write succeeds. */
private inline fun <T> Result<T>.announce(domain: DataEvents.Domain): Result<T> =
    onSuccess { DataEvents.emit(domain) }

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
        safeCall { api.login(LoginRequestDto(email.trim(), password)) }
            .onSuccess { persist(it) }
            .map { it.user.toDomain() }

    override suspend fun register(name: String, email: String, password: String): Result<User> =
        safeCall { api.register(RegisterRequestDto(email.trim(), password, name.ifBlank { null })) }
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
        safeCall(
            block = { api.getCurrentUser() },
            retryable = true,
            onCsrfRefresh = { refreshCsrfToken() }
        ).map { it.toDomain() }
            .onSuccess { userState.value = it }

    override suspend fun refreshCsrfToken(): Result<Unit> =
        safeCall { api.refreshCsrfToken() }
            .onSuccess { tokenManager.saveCsrfToken(it.csrfToken) }
            .map { }

    override suspend fun logout(): Result<Unit> {
        // DELETE auth/sessions invalidates the session row. Surface failures so the
        // caller can decide whether to retry, rather than silently returning success.
        return runCatching { api.logout() }
            .map { Unit }
            .onFailure { tokenManager.clearAll(); userState.value = null }
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : AccountRepository {

    override suspend fun getAccounts(): Result<List<Account>> =
        safeCall { api.getAccounts() }.map { list -> list.map { it.toDomain() } }

    override suspend fun createAccount(request: CreateAccountRequest): Result<Account> =
        safeCall { api.createAccount(request.toDto()) }.announce(DataEvents.Domain.ACCOUNTS).map { it.toDomain() }

    override suspend fun updateAccount(id: Long, request: UpdateAccountRequest): Result<Account> =
        safeCall { api.updateAccount(id, request.toDto()) }.announce(DataEvents.Domain.ACCOUNTS).map { it.toDomain() }

    override suspend fun deleteAccount(id: Long): Result<Unit> =
        safeCall { api.deleteAccount(id) }.announce(DataEvents.Domain.ACCOUNTS).map { }

    override suspend fun setShared(id: Long, familyId: Long?): Result<Account> =
        safeCall { api.updateAccountSharing(id, UpdateAccountSharingRequestDto(familyId)) }
            .fold(
                // PATCH answers with a stub when unsharing, so re-read the row.
                onSuccess = { safeCall { api.getAccount(id) }.map { it.toDomain() } },
                onFailure = { Result.failure(it) }
            )
            .announce(DataEvents.Domain.ACCOUNTS)

    override suspend fun getMembers(id: Long): Result<AccountMembers> =
        safeCall { api.getAccountMembers(id) }.map { dto ->
            AccountMembers(
                role = AccountShareRole.from(dto.account_role),
                owner = dto.owner?.let {
                    FamilyMember(it.user_id, "admin", it.name ?: "", it.email, null)
                },
                members = dto.members.map { it.toDomain() }
            )
        }

    override suspend fun shareWith(id: Long, userId: Long, role: AccountShareRole): Result<Unit> =
        safeCall { api.shareAccount(id, ShareAccountRequestDto(userId, role.value)) }.map { }

    override suspend fun removeMember(id: Long, userId: Long): Result<Unit> =
        safeCall { api.unshareAccount(id, RemoveMemberRequestDto(userId)) }.map { }
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : TransactionRepository {

    override suspend fun getTransactions(filter: TransactionFilter): Result<TransactionPage> =
        safeCall { api.getTransactions(
            limit = filter.limit,
            offset = filter.offset,
            type = filter.type?.value,
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            startDate = filter.startDate,
            endDate = filter.endDate,
            search = filter.search?.takeIf { it.isNotBlank() },
            includeSubcategories = filter.includeSubcategories,
            familyOnly = filter.familyOnly.takeIf { it }
        ) }.map { page ->
            TransactionPage(
                transactions = page.transactions.map { it.toDomain() },
                total = page.total,
                limit = page.limit,
                offset = page.offset
            )
        }

    override suspend fun createTransaction(request: CreateTransactionRequest): Result<Transaction> =
        safeCall { api.createTransaction(request.toDto()) }
            .announce(DataEvents.Domain.TRANSACTIONS)
            .map { it.transaction.toDomain() }

    override suspend fun updateTransaction(id: Long, request: UpdateTransactionRequest): Result<Transaction> =
        safeCall { api.updateTransaction(id, request.toDto()) }.announce(DataEvents.Domain.TRANSACTIONS).map { it.toDomain() }

    override suspend fun deleteTransaction(id: Long): Result<Unit> =
        safeCall { api.deleteTransaction(id) }.announce(DataEvents.Domain.TRANSACTIONS).map { }

    override suspend fun createTransfer(request: CreateTransferRequest): Result<Unit> =
        safeCall { api.createTransfer(
            CreateTransferRequestDto(
                from_account_id = request.fromAccountId,
                to_account_id = request.toAccountId,
                amount = request.amount,
                date = request.date,
                description = request.description
            )
        ) }.announce(DataEvents.Domain.TRANSACTIONS).map { }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : CategoryRepository {

    override suspend fun getCategories(): Result<List<Category>> =
        safeCall { api.getCategories() }.map { roots ->
            // Keep the one-level tree the API returns: the picker expands parents and
            // allows picking the parent itself, so flattening would lose that.
            roots.map { it.toDomain() }
        }

    override suspend fun createCategory(request: CreateCategoryRequest): Result<Category> =
        safeCall { api.createCategory(request.toDto()) }.announce(DataEvents.Domain.CATEGORIES).map { it.toDomain() }

    override suspend fun updateCategory(id: Long, request: UpdateCategoryRequest): Result<Category> =
        safeCall { api.updateCategory(id, request.toDto()) }.announce(DataEvents.Domain.CATEGORIES).map { it.toDomain() }

    override suspend fun deleteCategory(id: Long): Result<Unit> =
        safeCall { api.deleteCategory(id) }.announce(DataEvents.Domain.CATEGORIES).map { }
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : BudgetRepository {

    override suspend fun getBudgets(): Result<List<Budget>> =
        safeCall { api.getBudgets() }.map { list -> list.map { it.toDomain() } }

    override suspend fun createBudget(request: CreateBudgetRequest): Result<Budget> =
        safeCall { api.createBudget(request.toDto()) }.announce(DataEvents.Domain.BUDGETS).map { it.toDomain() }

    override suspend fun updateBudget(id: Long, request: UpdateBudgetRequest): Result<Budget> =
        safeCall { api.updateBudget(id, request.toDto()) }.announce(DataEvents.Domain.BUDGETS).map { it.toDomain() }

    override suspend fun deleteBudget(id: Long): Result<Unit> =
        safeCall { api.deleteBudget(id) }.announce(DataEvents.Domain.BUDGETS).map { }

    override suspend fun setBudgetSharing(id: Long, familyId: Long?): Result<Budget> =
        safeCall { api.updateBudgetSharing(id, UpdateBudgetSharingRequestDto(familyId)) }
            .announce(DataEvents.Domain.BUDGETS)
            .map { it.toDomain() }
}

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> =
        safeCall { api.getGoals() }.map { list -> list.map { it.toDomain() } }

    override suspend fun createGoal(request: CreateGoalRequest): Result<Goal> =
        safeCall { api.createGoal(request.toDto()) }.announce(DataEvents.Domain.GOALS).map { it.toDomain() }

    override suspend fun updateGoal(id: Long, request: UpdateGoalRequest): Result<Goal> =
        safeCall { api.updateGoal(id, request.toDto()) }.announce(DataEvents.Domain.GOALS).map { it.toDomain() }

    override suspend fun deleteGoal(id: Long): Result<Unit> =
        safeCall { api.deleteGoal(id) }.announce(DataEvents.Domain.GOALS).map { }

    override suspend fun addFunds(id: Long, amount: Double, accountId: Long): Result<Unit> =
        safeCall { api.addGoalFunds(id, AddGoalFundsRequestDto(amount, accountId)) }
            .announce(DataEvents.Domain.GOALS).map { }
}

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : DebtRepository {

    override suspend fun getDebts(): Result<List<Debt>> =
        safeCall { api.getDebts() }.map { response -> response.debts.map { it.toDomain() } }

    override suspend fun createDebt(request: CreateDebtRequest): Result<Debt> =
        safeCall { api.createDebt(
            CreateDebtRequestDto(
                person_name = request.name,
                amount = request.amount,
                due_date = request.dueDate,
                type = request.type.value,
                description = request.notes
            )
        ) }.announce(DataEvents.Domain.DEBTS).map { it.toDomain() }

    override suspend fun updateDebt(id: Long, request: UpdateDebtRequest): Result<Debt> =
        safeCall { api.updateDebt(
            id,
            UpdateDebtRequestDto(
                person_name = request.name,
                amount = request.amount,
                due_date = request.dueDate,
                type = request.type?.value,
                description = request.notes
            )
        ) }.announce(DataEvents.Domain.DEBTS).map { it.toDomain() }

    override suspend fun deleteDebt(id: Long): Result<Unit> =
        safeCall { api.deleteDebt(id) }.announce(DataEvents.Domain.DEBTS).map { }

    override suspend fun payDebt(id: Long, request: PayDebtRequest): Result<Unit> =
        safeCall { api.payDebt(
            id,
            PayDebtRequestDto(
                amount = request.amount,
                account_id = request.accountId,
                create_transaction = request.createTransaction,
                date = request.date
            )
        ) }.announce(DataEvents.Domain.DEBTS).map { }
}

@Singleton
class ScheduledRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : ScheduledRepository {

    override suspend fun getScheduled(): Result<List<ScheduledTransaction>> =
        safeCall { api.getScheduled() }.map { list -> list.map { it.toDomain() } }

    override suspend fun createScheduled(request: CreateScheduledRequest): Result<ScheduledTransaction> =
        safeCall { api.createScheduled(
            CreateScheduledRequestDto(
                account_id = request.accountId,
                category_id = request.categoryId,
                amount = request.amount,
                type = request.type.value,
                description = request.description,
                frequency = request.frequency,
                next_run_date = request.nextDate.orEmpty()
            )
        ) }.announce(DataEvents.Domain.SCHEDULED).map { it.toDomain() }

    override suspend fun deleteScheduled(id: Long): Result<Unit> =
        safeCall { api.deleteScheduled(id) }.announce(DataEvents.Domain.SCHEDULED).map { }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : SettingsRepository {

    override suspend fun getSettings(): Result<UserSettings> =
        safeCall { api.getSettings() }.map { it.toDomain() }

    override suspend fun updateSettings(request: UpdateSettingsRequest): Result<UserSettings> =
        safeCall { api.updateSettings(
            UpdateSettingsRequestDto(
                currency = request.currency,
                date_format = request.dateFormat,
                language = request.language,
                timezone = request.timezone,
                theme = request.theme,
                animation_mode = request.animationMode,
                animation_level = request.animationLevel
            )
        ) }.map { it.toDomain() }

    override suspend fun changePassword(current: String, next: String): Result<Unit> =
        safeCall { api.changePassword(ChangePasswordRequestDto(current, next)) }.map { }
}

@Singleton
class InsightsRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : InsightsRepository {

    override suspend fun getAchievements(): Result<List<Achievement>> =
        safeCall { api.getAchievements() }.map { list -> list.map { it.toDomain() } }

    override suspend fun getLeaderboard(limit: Int): Result<List<LeaderboardEntry>> =
        safeCall { api.getLeaderboard(limit = limit) }
            .map { page -> page.leaderboard.map { it.toDomain() } }

    override suspend fun getForecast(): Result<Forecast> =
        safeCall { api.getForecast() }.map { dto ->
            Forecast(
                points = dto.forecast.map {
                    ForecastPoint(it.date, it.projectedBalance)
                },
                averageMonthlyExpense = dto.average_monthly_expense
            )
        }

    override suspend fun getHealthScore(): Result<HealthScore> =
        safeCall { api.getHealthScore() }.map { dto ->
            HealthScore(
                score = dto.score,
                factors = dto.components.mapValues { (_, f) ->
                    HealthFactor(f.score, f.value.toString(), f.label, f.recommendation)
                }
            )
        }
}

@Singleton
class FamilyRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : FamilyRepository {

    override suspend fun getFamilies(): Result<List<FamilyGroup>> =
        safeCall { api.getFamilies() }.map { list -> list.map { it.toDomain() } }

    override suspend fun createFamily(name: String): Result<FamilyGroup> =
        safeCall { api.createFamily(CreateFamilyRequestDto(name)) }.announce(DataEvents.Domain.FAMILY).map { it.toDomain() }

    override suspend fun getFamily(id: Long): Result<FamilyGroup> =
        safeCall { api.getFamily(id) }.map { it.toDomain() }

    override suspend fun createInvite(id: Long): Result<String> =
        safeCall { api.inviteFamilyMember(id, InviteFamilyMemberRequestDto()) }.map { it.code }

    override suspend fun acceptInvite(code: String): Result<FamilyGroup> =
        safeCall { api.acceptFamilyInvite(AcceptInviteRequestDto(code.trim().uppercase())) }
            .map { it.family }
            .mapNotNull { family -> family.toDomain() }
            .map { family ->
                if (family == null) throw IllegalStateException("Invite accepted but no family returned")
                family
            }

    override suspend fun removeMember(id: Long, userId: Long): Result<Unit> =
        safeCall { api.removeFamilyMember(id, RemoveMemberRequestDto(userId)) }.map { }
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: EfinaApi
) : ChatRepository {

    override suspend fun send(message: String, history: List<ChatMessage>): Result<ChatResponse> =
        safeCall { api.chat(
            ChatRequestDto(
                message = message,
                history = history.map { ChatMessageDto(it.role, it.content) }
            )
        ) }.map { dto ->
            ChatResponse(
                text = dto.response.ifBlank { dto.reply.orEmpty() },
                actions = dto.actions.map { SuggestedAction(it.type, it.data) }
            )
        }
}
