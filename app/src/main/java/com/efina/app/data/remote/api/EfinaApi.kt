package com.efina.app.data.remote.api

import com.efina.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit contract for the Efina Cloudflare Pages Functions backend.
 *
 * Every route here is verified against `functions/api/` by
 * `tools/check-api-contract.mjs`. That script runs in CI, so a verb or path that
 * the backend does not implement fails the build rather than the user's screen.
 *
 * Backend routing facts that shape this file:
 *  - `functions/api/<x>/index.js` -> `/api/<x>`, `[id].js` -> `/api/<x>/{id}`
 *  - Handlers export `onRequestGet/Post/Put/Patch/Delete`; a missing export is a
 *    405 at runtime. Updates are PUT, not PATCH, on every collection except
 *    accounts (which also accepts PATCH, for the sharing toggle only).
 *  - There is no `/api/dashboard`, no `/api/auth/me` and no `/api/ai/xp-summary`.
 *    The dashboard is composed client-side from accounts, transactions, budgets
 *    and goals; the current user comes from the login response or `GET user`.
 *  - Logout is `DELETE /api/auth/sessions`, not `POST /api/auth/logout`.
 */
interface EfinaApi {

    // ============ Auth ============

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>

    /** Invalidates the current session. The Authorization token identifies it. */
    @DELETE("auth/sessions")
    suspend fun logout(): Response<Unit>

    /** Current user + settings. Replaces the nonexistent `/auth/me`. */
    @GET("user")
    suspend fun getCurrentUser(): Response<UserDto>

    /**
     * Issues a fresh CSRF token. CSRF JWTs expire in 24 h while the session lives
     * 7 days, so without this every write starts returning 403 after a day.
     */
    @GET("auth/csrf")
    suspend fun refreshCsrfToken(): Response<CsrfTokenDto>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): Response<AuthResponseDto>

    @POST("auth/reset-password/request")
    suspend fun requestPasswordReset(@Body request: ForgotPasswordRequestDto): Response<Unit>

    // ============ Accounts ============

    /** Returns a bare array. Each row carries `account_role` and `member_count`. */
    @GET("accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>

    @GET("accounts/{id}")
    suspend fun getAccount(@Path("id") id: Long): Response<AccountDto>

    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequestDto): Response<AccountDto>

    @PUT("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: Long,
        @Body request: UpdateAccountRequestDto
    ): Response<AccountDto>

    /** Sharing toggle only: `{ family_id: Long | null }`. */
    @PATCH("accounts/{id}")
    suspend fun updateAccountSharing(
        @Path("id") id: Long,
        @Body request: UpdateAccountSharingRequestDto
    ): Response<SharingResultDto>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Long): Response<Unit>

    @GET("accounts/recalculate")
    suspend fun recalculateBalances(): Response<List<RecalculateResultDto>>

    // ============ Transactions ============

    /** Envelope, not an array: `{ transactions, total, limit, offset }`. */
    @GET("transactions")
    suspend fun getTransactions(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("type") type: String? = null,
        @Query("account_id") accountId: Long? = null,
        @Query("category_id") categoryId: Long? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("search") search: String? = null,
        @Query("family_only") familyOnly: Boolean? = null,
        @Query("include_subcategories") includeSubcategories: Boolean? = null
    ): Response<TransactionsPageDto>

    /** Returns `{ transaction, gamification }` — see CreatedTransactionDto. */
    @POST("transactions")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequestDto
    ): Response<CreatedTransactionDto>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body request: UpdateTransactionRequestDto
    ): Response<TransactionDto>

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>

    // ============ Transfers ============

    /** Creates a linked expense/income pair and moves both balances. */
    @POST("transfers")
    suspend fun createTransfer(@Body request: CreateTransferRequestDto): Response<TransferResultDto>

    @PUT("transfers/{id}")
    suspend fun updateTransfer(
        @Path("id") id: Long,
        @Body request: CreateTransferRequestDto
    ): Response<TransferResultDto>

    @DELETE("transfers/{id}")
    suspend fun deleteTransfer(@Path("id") id: Long): Response<Unit>

    // ============ Categories ============

    @GET("categories")
    suspend fun getCategories(@Query("type") type: String? = null): Response<List<CategoryDto>>

    @POST("categories")
    suspend fun createCategory(@Body request: CreateCategoryRequestDto): Response<CategoryDto>

    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: UpdateCategoryRequestDto
    ): Response<CategoryDto>

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // ============ Budgets ============

    @GET("budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>

    @POST("budgets")
    suspend fun createBudget(@Body request: CreateBudgetRequestDto): Response<BudgetDto>

    @PUT("budgets/{id}")
    suspend fun updateBudget(
        @Path("id") id: Long,
        @Body request: UpdateBudgetRequestDto
    ): Response<BudgetDto>

    /** Toggle sharing; mirrors PATCH accounts/{id} so the client has one sharing call. */
    @PATCH("budgets/{id}")
    suspend fun updateBudgetSharing(
        @Path("id") id: Long,
        @Body request: UpdateBudgetSharingRequestDto
    ): Response<BudgetDto>

    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Long): Response<Unit>

    @GET("budgets/vs-actual")
    suspend fun getBudgetVsActual(): Response<BudgetVsActualDto>

    // ============ Goals ============

    @GET("goals")
    suspend fun getGoals(): Response<List<GoalDto>>

    @POST("goals")
    suspend fun createGoal(@Body request: CreateGoalRequestDto): Response<GoalDto>

    @PUT("goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: Long,
        @Body request: UpdateGoalRequestDto
    ): Response<GoalDto>

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Long): Response<Unit>

    @POST("goals/{id}/add-funds")
    suspend fun addGoalFunds(
        @Path("id") id: Long,
        @Body request: AddGoalFundsRequestDto
    ): Response<GoalFundsResultDto>

    // ============ Debts ============

    @GET("debts")
    suspend fun getDebts(@Query("status") status: String? = null): Response<DebtsResponseDto>

    @POST("debts")
    suspend fun createDebt(@Body request: CreateDebtRequestDto): Response<DebtDto>

    @PUT("debts/{id}")
    suspend fun updateDebt(
        @Path("id") id: Long,
        @Body request: UpdateDebtRequestDto
    ): Response<DebtDto>

    @DELETE("debts/{id}")
    suspend fun deleteDebt(@Path("id") id: Long): Response<Unit>

    @POST("debts/{id}/pay")
    suspend fun payDebt(
        @Path("id") id: Long,
        @Body request: PayDebtRequestDto
    ): Response<DebtDto>

    // ============ Scheduled ============

    @GET("scheduled")
    suspend fun getScheduled(): Response<List<ScheduledDto>>

    @POST("scheduled")
    suspend fun createScheduled(@Body request: CreateScheduledRequestDto): Response<ScheduledDto>

    @PUT("scheduled/{id}")
    suspend fun updateScheduled(
        @Path("id") id: Long,
        @Body request: UpdateScheduledRequestDto
    ): Response<ScheduledDto>

    @DELETE("scheduled/{id}")
    suspend fun deleteScheduled(@Path("id") id: Long): Response<Unit>

    // ============ Notifications ============

    @GET("notifications")
    suspend fun getNotifications(
        @Query("unread_only") unreadOnly: Boolean? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<NotificationDto>>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Long): Response<Unit>

    @PUT("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>

    // ============ Settings ============

    @GET("settings")
    suspend fun getSettings(): Response<UserSettingsDto>

    /** PUT, not PATCH. Backend reads a partial body and COALESCEs the rest. */
    @PUT("settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequestDto): Response<UserSettingsDto>

    // ============ AI ============

    @POST("ai/chat")
    suspend fun chat(@Body request: ChatRequestDto): Response<ChatResponseDto>

    @GET("ai/context")
    suspend fun getAiContext(): Response<AiContextDto>

    // ============ Gamification / stats ============

    @GET("achievements")
    suspend fun getAchievements(): Response<List<AchievementDto>>

    @GET("gamification/history")
    suspend fun getXpHistory(
        @Query("limit") limit: Int? = null
    ): Response<XpHistoryDto>

    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("sort_by") sortBy: String? = null
    ): Response<LeaderboardPageDto>

    @GET("forecast")
    suspend fun getForecast(): Response<ForecastDto>

    @GET("health-score")
    suspend fun getHealthScore(): Response<HealthScoreDto>

    // ============ Reporting ============

    @GET("user/heatmap")
    suspend fun getHeatmap(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<List<HeatmapDayDto>>

    @GET("user/monthly-stats")
    suspend fun getMonthlyStats(
        @Query("months") months: Int? = null
    ): Response<List<MonthlyStatDto>>

    // ============ Family ============

    @GET("families")
    suspend fun getFamilies(): Response<List<FamilyGroupDto>>

    @POST("families")
    suspend fun createFamily(@Body request: CreateFamilyRequestDto): Response<FamilyGroupDto>

    @GET("families/{id}")
    suspend fun getFamily(@Path("id") id: Long): Response<FamilyGroupDto>

    @POST("families/{id}/members")
    suspend fun inviteFamilyMember(
        @Path("id") id: Long,
        @Body request: InviteFamilyMemberRequestDto
    ): Response<FamilyInviteDto>

    @DELETE("families/{id}/members")
    suspend fun removeFamilyMember(
        @Path("id") id: Long,
        @Body request: RemoveMemberRequestDto
    ): Response<Unit>

    @POST("families/invites/accept")
    suspend fun acceptFamilyInvite(
        @Body request: AcceptInviteRequestDto
    ): Response<AcceptInviteResponseDto>

    // ============ Shared accounts ============

    @GET("accounts/{id}/members")
    suspend fun getAccountMembers(
        @Path("id") id: Long
    ): Response<AccountMembersDto>

    @POST("accounts/{id}/members")
    suspend fun shareAccount(
        @Path("id") id: Long,
        @Body request: ShareAccountRequestDto
    ): Response<AccountMemberDto>

    @DELETE("accounts/{id}/members")
    suspend fun unshareAccount(
        @Path("id") id: Long,
        @Body request: RemoveMemberRequestDto
    ): Response<Unit>

    // ============ Account ============

    @PUT("user")
    suspend fun updateUser(@Body request: UpdateUserRequestDto): Response<UserDto>

    @POST("user/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequestDto): Response<Unit>
}
