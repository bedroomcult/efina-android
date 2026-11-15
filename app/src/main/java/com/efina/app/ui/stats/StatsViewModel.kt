package com.efina.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.*
import com.efina.app.ui.components.charts.CashFlowPoint
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

enum class TimeRange(val label: String, val days: Int) {
    Days7("7 days", 7),
    Days30("30 days", 30),
    Days365("365 days", 365);

    fun toDateRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val end = cal.clone() as Calendar
        val start = cal.clone() as Calendar
        start.add(Calendar.DAY_OF_YEAR, -days)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "${fmt.format(start.time)}T00:00:00" to "${fmt.format(end.time)}T23:59:59"
    }
}

data class StatsUiState(
    val loading: Boolean = true,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val byCategory: List<CategorySlice> = emptyList(),
    val forecast: Forecast? = null,
    val health: HealthScore? = null,
    val achievements: List<Achievement> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val monthlyStats: List<MonthlyStat> = emptyList(),
    val budgetVsActual: List<BudgetVsActual> = emptyList(),
    val error: String? = null,
    val fatalError: Boolean = false,
    val timeRange: TimeRange = TimeRange.Days7,
    val chartPoints: List<CashFlowPoint> = emptyList(),
    /** When the server-computed figures below were last fetched. */
    val lastUpdatedAt: Long? = null
) {
    val net: Double get() = income - expense
    val savingsRate: Float
        get() = if (income <= 0.0) 0f else ((net / income) * 100.0).toFloat()
}

data class CategorySlice(val name: String, val total: Double)

/**
 * Statistics are derived client-side from the transaction list plus the real
 * insight endpoints (forecast, health-score, achievements, leaderboard), since
 * the backend has no aggregate statistics route.
 */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val insightsRepository: InsightsRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init { load(TimeRange.Days7) }

    fun load(range: TimeRange) {
        _state.value = _state.value.copy(loading = true, error = null, timeRange = range)
        viewModelScope.launch {
            val (start, end) = range.toDateRange()

            val txJob = async {
                transactionRepository.getTransactions(
                    TransactionFilter(limit = 500, offset = 0, startDate = start, endDate = end)
                )
            }
            val forecastJob = async { insightsRepository.getForecast() }
            val healthJob = async { insightsRepository.getHealthScore() }
            val achievementsJob = async { insightsRepository.getAchievements() }
            val leaderboardJob = async { insightsRepository.getLeaderboard(10) }
            val budgetVsActualJob = async { budgetRepository.getBudgetVsActual() }

            // The backend's monthly-stats route is a one-month recap, not a series.
            // Group a six-month transaction fetch client-side instead of sending an
            // invalid `months` query the handler rejects with 400.
            val sixMonthEnd = Calendar.getInstance()
            val sixMonthStart = (sixMonthEnd.clone() as Calendar).apply {
                add(Calendar.MONTH, -5)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val monthlyTxJob = async {
                transactionRepository.getTransactions(
                    TransactionFilter(
                        limit = 1000,
                        offset = 0,
                        startDate = "${dateFmt.format(sixMonthStart.time)}T00:00:00",
                        endDate = "${dateFmt.format(sixMonthEnd.time)}T23:59:59"
                    )
                )
            }

            val txPageResult = txJob.await()
            val transactions = txPageResult.getOrNull()?.transactions ?: emptyList()

            val income = transactions.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val byCategory = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryName ?: "Uncategorised" }
                .map { (name, list) -> CategorySlice(name, list.sumOf { it.amount }) }
                .sortedByDescending { it.total }

            val chartPoints = buildChartPoints(transactions, range)

            // Await each job once and store results, then inspect for failures.
            val forecastResult = forecastJob.await()
            val healthResult = healthJob.await()
            val achievementsResult = achievementsJob.await()
            val leaderboardResult = leaderboardJob.await()
            val monthlyTxResult = monthlyTxJob.await()
            val budgetVsActualResult = budgetVsActualJob.await()

            val monthlyStats = (monthlyTxResult.getOrNull()?.transactions ?: emptyList())
                .groupBy { it.date.take(7) }
                .toSortedMap()
                .map { (month, list) ->
                    val monthIncome = list.filter { it.type == TransactionType.INCOME }
                        .sumOf { it.amount }
                    val monthExpense = list.filter { it.type == TransactionType.EXPENSE }
                        .sumOf { it.amount }
                    MonthlyStat(month, monthIncome, monthExpense, monthIncome - monthExpense)
                }

            // Track which jobs failed so we can surface an error instead of silently
            // rendering empty lists.
            val failedJobs = listOf(
                txJob to txPageResult,
                forecastJob to forecastResult,
                healthJob to healthResult,
                achievementsJob to achievementsResult,
                leaderboardJob to leaderboardResult,
                monthlyTxJob to monthlyTxResult,
                budgetVsActualJob to budgetVsActualResult
            ).filter { (_, result) -> result.isFailure }

            val allFailed = txPageResult.isFailure && transactions.isEmpty() &&
                forecastResult.isFailure && healthResult.isFailure &&
                achievementsResult.isFailure && leaderboardResult.isFailure

            _state.value = StatsUiState(
                loading = false,
                income = income,
                expense = expense,
                byCategory = byCategory,
                forecast = forecastResult.getOrNull(),
                health = healthResult.getOrNull(),
                lastUpdatedAt = System.currentTimeMillis(),
                achievements = achievementsResult.getOrDefault(emptyList()),
                leaderboard = leaderboardResult.getOrDefault(emptyList()),
                monthlyStats = monthlyStats,
                budgetVsActual = budgetVsActualResult.getOrDefault(emptyList()),
                timeRange = range,
                chartPoints = chartPoints,
                fatalError = allFailed,
                error = if (allFailed) {
                    failedJobs.firstOrNull()?.let { (_, result) ->
                        result.exceptionOrNull()?.message
                    } ?: "Could not load statistics."
                } else null
            )
        }
    }

    /** Bucket transactions into chart points based on the selected range. */
    private fun buildChartPoints(transactions: List<Transaction>, range: TimeRange): List<CashFlowPoint> {
        val netByDate = transactions.groupBy { it.date.substringBefore('T') }
            .mapValues { (_, list) ->
                list.sumOf { t ->
                    when (t.type) {
                        TransactionType.INCOME -> t.amount
                        TransactionType.ADJUSTMENT -> if (t.amount >= 0) t.amount else -Math.abs(t.amount)
                        else -> -Math.abs(t.amount)
                    }
                }
            }

        return when (range) {
            TimeRange.Days7 -> {
                val fmt = SimpleDateFormat("EEE", Locale.US)
                val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                (0 until 7).map { i ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -(6 - i))
                    val dateKey = dateFmt.format(cal.time)
                    CashFlowPoint(
                        date = dateKey,
                        displayDate = fmt.format(cal.time),
                        value = netByDate[dateKey] ?: 0.0
                    )
                }
            }
            TimeRange.Days30 -> {
                val fmt = SimpleDateFormat("MMM d", Locale.US)
                val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                (0 until 10).map { i ->
                    val end = Calendar.getInstance()
                    end.add(Calendar.DAY_OF_YEAR, -(i * 3))
                    val start = Calendar.getInstance()
                    start.add(Calendar.DAY_OF_YEAR, -((i + 1) * 3))
                    val dateKey = dateFmt.format(start.time)
                    val total = netByDate.entries.sumOf { (dk, v) ->
                        val d = dateFmt.parse(dk) ?: return@sumOf 0.0
                        val cd = Calendar.getInstance()
                        cd.time = d
                        if (cd.after(start) && !cd.after(end)) v else 0.0
                    }
                    CashFlowPoint(
                        date = dateKey,
                        displayDate = fmt.format(start.time),
                        value = total
                    )
                }.reversed()
            }
            TimeRange.Days365 -> {
                val fmt = SimpleDateFormat("MMM", Locale.US)
                (0 until 12).map { i ->
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.MONTH, -(11 - i))
                    val yearMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
                    val total = netByDate.entries.sumOf { (dk, v) ->
                        if (dk.startsWith(yearMonth)) v else 0.0
                    }
                    CashFlowPoint(
                        date = yearMonth,
                        displayDate = fmt.format(cal.time),
                        value = total
                    )
                }
            }
        }
    }

    private fun currentMonthRange(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val start = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val end = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "${fmt.format(start.time)}T00:00:00" to "${fmt.format(end.time)}T23:59:59"
    }
}
