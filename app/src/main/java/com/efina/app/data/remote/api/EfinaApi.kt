package com.efina.app.data.remote.api

import com.efina.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface EfinaApi {

    // ============ Auth ============
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>
    
    @POST("auth/logout")
    suspend fun logout(): Response<Unit>
    
    @GET("auth/me")
    suspend fun getMe(): Response<UserDto>

    // ============ Accounts ============
    
    @GET("accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>
    
    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequestDto): Response<AccountDto>
    
    @PATCH("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<AccountDto>
    
    @DELETE("accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: Long): Response<Unit>

    // ============ Transactions ============
    
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
        @Query("include_subcategories") includeSubcategories: Boolean? = null
    ): Response<List<TransactionDto>>
    
    @POST("transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequestDto): Response<TransactionDto>
    
    @PATCH("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<TransactionDto>
    
    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>

    // ============ Transfers ============
    
    @POST("transfers")
    suspend fun createTransfer(@Body request: CreateTransferRequestDto): Response<TransactionDto>

    // ============ Categories ============
    
    @GET("categories")
    suspend fun getCategories(
        @Query("type") type: String? = null
    ): Response<List<CategoryDto>>
    
    @POST("categories")
    suspend fun createCategory(@Body request: CreateCategoryRequestDto): Response<CategoryDto>
    
    @PATCH("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<CategoryDto>
    
    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // ============ Budgets ============
    
    @GET("budgets")
    suspend fun getBudgets(): Response<List<BudgetDto>>
    
    @POST("budgets")
    suspend fun createBudget(@Body request: CreateBudgetRequestDto): Response<BudgetDto>
    
    @PATCH("budgets/{id}")
    suspend fun updateBudget(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<BudgetDto>
    
    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Long): Response<Unit>

    // ============ Goals ============
    
    @GET("goals")
    suspend fun getGoals(): Response<List<GoalDto>>
    
    @POST("goals")
    suspend fun createGoal(@Body request: CreateGoalRequestDto): Response<GoalDto>
    
    @PATCH("goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: Long,
        @Body request: Map<String, @JvmSuppressWildcards Any?>
    ): Response<GoalDto>
    
    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Long): Response<Unit>

    // ============ Dashboard ============
    
    @GET("dashboard")
    suspend fun getDashboard(): Response<Map<String, Any>>

    // ============ Settings ============
    
    @GET("settings")
    suspend fun getSettings(): Response<UserSettingsDto>
    
    @PATCH("settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequestDto): Response<UserSettingsDto>

    // ============ AI ============
    
    @POST("ai/chat")
    suspend fun chat(@Body request: ChatRequestDto): Response<ChatResponseDto>
    
    @GET("ai/xp-summary")
    suspend fun getXpSummary(): Response<XpSummaryDto>

    // ============ Achievements ============
    
    @GET("achievements")
    suspend fun getAchievements(): Response<AchievementsListDto>

    // ============ Forecast ============
    
    @GET("forecast")
    suspend fun getForecast(): Response<ForecastDto>

    // ============ Health Score ============
    
    @GET("health-score")
    suspend fun getHealthScore(): Response<HealthScoreDto>

    // ============ Leaderboard ============
    
    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("limit") limit: Int? = null
    ): Response<Map<String, Any>>
}
