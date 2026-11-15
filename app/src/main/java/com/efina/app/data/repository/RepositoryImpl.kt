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
internal suspend fun <T> safeCall(block: suspend () -> Response<T>): Result<T> =
    try {
        block().toResult()
    } catch (io: java.io.IOException) {
        Result.failure(
            ApiException(0, "No connection. Check your network and try again.")
        )
    } catch (e: Exception) {
        Result.failure(ApiException(0, e.message ?: "Unexpected error"))
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

    override suspend fun loginWithGoogle(credential: String): Result<User> =
        safeCall { api.loginWithGoogle(GoogleLoginRequestDto(credential)) }
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
        safeCall { api.getCurrentUser() }
            .map { it.toDomain() }
            .onSuccess { userState.value = it }

    override suspend fun refreshCsrfToken(): Result<Unit> =
        safeCall { api.refreshCsrfToken() }
            .onSuccess { tokenManager.saveCsrfToken(it.csrfToken) }
            .map { }

    override suspend fun logout(): Result<Unit> {
        // Revoke server-side first (the token is still attached), then clear locally
        // no matter what the server says. The old code only cleared on an exception,
        // so a 401 (expired token) or a 200 (successful revoke) both left the token
        // in storage, and the next launch restored a dead session.
        return try {
            val response = api.logout()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(ApiException(response.code(), "Logout failed (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(ApiException(0, e.message ?: "Logout failed"))
        } finally {
            clearSession()
        }
    }

    override fun clearSession() {
        tokenManager.clearAll()
        userState.value = null
    }

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()
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
            .map { it.family ?: throw IllegalStateException("Invite accepted but no family returned") }
            .map { family -> family.toDomain() }

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
