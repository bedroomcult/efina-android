package com.efina.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val data: DashboardData = DashboardData(),
    val healthScore: HealthScore? = null,
    val error: String? = null
)

/**
 * The dashboard is composed client-side.
 *
 * There is no `/api/dashboard` on the backend (verified: functions/api has no
 * dashboard directory), so this fans out to the real endpoints in parallel and
 * assembles the same view the web app builds from its react-query caches.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val goalRepository: GoalRepository,
    private val insightsRepository: InsightsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { load() }

    fun load(isRefresh: Boolean = false) {
        _state.value = _state.value.copy(
            loading = !isRefresh && _state.value.data.accounts.isEmpty(),
            refreshing = isRefresh,
            error = null
        )

        viewModelScope.launch {
            val (startDate, endDate) = currentMonthRange()

            // Fan out; a failure in one panel must not blank the whole dashboard.
            val accountsJob = async { accountRepository.getAccounts() }
            val txJob = async {
                transactionRepository.getTransactions(
                    TransactionFilter(
                        // Summed below for the monthly totals, so this must cover the
                        // whole month, not just the rows the dashboard displays.
                        // limit = 10000: effectively no cap for a single month; users
                        // with >10k transactions in a month would need backend-side
                        // aggregation (see ponytail note in original code).
                        limit = 10000,
                        offset = 0,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
            }
            val budgetJob = async { budgetRepository.getBudgets() }
            val goalJob = async { goalRepository.getGoals() }
            val healthJob = async { insightsRepository.getHealthScore() }

            val accountsResult = accountsJob.await()
            val txPageResult = txJob.await()
            val budgetsResult = budgetJob.await()
            val goalsResult = goalJob.await()
            val healthResult = healthJob.await()

            val accounts = accountsResult.getOrDefault(emptyList())
            val txPage = txPageResult.getOrNull()
            val budgets = budgetsResult.getOrDefault(emptyList())
            val goals = goalsResult.getOrDefault(emptyList())
            val health = healthResult.getOrNull()

            // Track which critical jobs failed so we can surface an error instead of
            // silently rendering empty panels. Health score is optional — a failure
            // there just means no health card is shown, not an error message.
            val failedJobs = listOf(
                accountsJob to accountsResult,
                txJob to txPageResult,
                budgetJob to budgetsResult,
                goalJob to goalsResult
            ).filter { (_, result) -> result.isFailure }

            val transactions = txPage?.transactions ?: emptyList()
            val income = transactions.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            // Surface a total failure: when every panel failed to load, show an error
            // instead of zeros that look like a real empty account.
            val allFailed = accounts.isEmpty() && txPage == null && budgets.isEmpty() && goals.isEmpty()
            val failureMessage = if (allFailed) {
                failedJobs.firstOrNull()?.let { (_, result) ->
                    result.exceptionOrNull()?.message
                } ?: "Could not load your data. Pull to retry."
            } else if (failedJobs.isNotEmpty()) {
                // Partial failure: at least one panel failed. Surface the first error
                // but still show whatever data loaded.
                failedJobs.firstOrNull()?.let { (_, result) ->
                    result.exceptionOrNull()?.message
                } ?: null
            } else null

            _state.value = HomeUiState(
                loading = false,
                refreshing = false,
                data = DashboardData(
                    totalBalance = accounts.sumOf { it.balance },
                    accounts = accounts,
                    recentTransactions = transactions.take(10),
                    budgets = budgets,
                    goals = goals,
                    monthlyIncome = income,
                    monthlyExpense = expense
                ),
                healthScore = health,
                error = failureMessage
            )
        }
    }

    fun refresh() = load(isRefresh = true)

    private fun currentMonthRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val start = cal.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        val end = cal.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "${fmt.format(start.time)}T00:00:00" to "${fmt.format(end.time)}T23:59:59"
    }
}
