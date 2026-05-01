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
    
    @POST("auth/pin")
    suspend fun setPin(@Body request: PinRequestDto): Response<Unit>
    
    @POST("auth/verify-pin")
    suspend fun verifyPin(@Body request: PinRequestDto): Response<AuthResponseDto>
    
    @GET("auth/sessions")
    suspend fun getSessions(): Response<SessionsResponseDto>

    // ============ Accounts ============
    
    @GET("accounts")
    suspend fun getAccounts(): Response<List<AccountDto>>
    
    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequestDto): Response<AccountDto>
    
    @PUT("accounts/{id}")
    suspend fun updateAccount(
        @Path("id") id: Long,
        @Body request: Map<String, Any?>
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
    ): Response<TransactionListResponseDto>
    
    @POST("transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequestDto): Response<TransactionDto>
    
    @GET("transactions/{id}")
    suspend fun getTransaction(@Path("id") id: Long): Response<TransactionDto>
    
    @PUT("transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: Long,
        @Body request: Map<String, Any?>
    ): Response<TransactionDto>
    
    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Long): Response<Unit>

    // ============ Transfers ============
    
    @GET("transfers")
    suspend fun getTransfers(): Response<List<TransactionDto>>
    
    @POST("transfers")
    suspend fun createTransfer(@Body request: CreateTransferRequestDto): Response<TransactionDto>

    // ============ Categories ============
    
    @GET("categories")
    suspend fun getCategories(
        @Query("type") type: String? = null
    ): Response<List<CategoryDto>>
    
    @POST("categories")
    suspend fun createCategory(@Body request: CreateCategoryRequestDto): Response<CategoryDto>
    
    @PUT("categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: Long,
        @Body request: Map<String, Any?>
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
        @Body request: Map<String, Any?>
    ): Response<BudgetDto>
    
    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Long): Response<Unit>

    // ============ Goals ============
    
    @GET("goals")
    suspend fun getGoals(): Response<List<GoalDto>>
    
    @POST("goals")
    suspend fun createGoal(@Body request: CreateGoalRequestDto): Response<GoalDto>
    
    @PUT("goals/{id}")
    suspend fun updateGoal(
        @Path("id") id: Long,
        @Body request: Map<String, Any?>
    ): Response<GoalDto>
    
    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Long): Response<Unit>

    // ============ User Stats ============
    
    @GET("user/stats")
    suspend fun getUserStats(): Response<UserProfileResponseDto>
    
    @GET("user/settings")
    suspend fun getSettings(): Response<UserSettingsDto>
    
    @PUT("user/settings")
    suspend fun updateSettings(@Body request: UpdateSettingsRequestDto): Response<UserSettingsDto>

    // ============ AI ============
    
    @POST("ai/chat")
    suspend fun chat(@Body request: ChatRequestDto): Response<ChatResponseDto>

    // ============ Health Score ============
    
    @GET("health-score")
    suspend fun getHealthScore(): Response<Map<String, Any>>

    // ============ Forecast ============
    
    @GET("forecast")
    suspend fun getForecast(): Response<Map<String, Any>>

    // ============ Leaderboard ============
    
    @GET("leaderboard")
    suspend fun getLeaderboard(
        @Query("limit") limit: Int? = null
    ): Response<Map<String, Any>>
}
