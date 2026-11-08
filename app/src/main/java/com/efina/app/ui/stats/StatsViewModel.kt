package com.efina.app.ui.stats

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

data class StatsUiState(
    val loading: Boolean = true,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val byCategory: List<CategorySlice> = emptyList(),
    val forecast: Forecast? = null,
    val health: HealthScore? = null,
    val achievements: List<Achievement> = emptyList(),
    val leaderboard: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
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
    private val insightsRepository: InsightsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val (start, end) = currentMonthRange()

            val txJob = async {
                transactionRepository.getTransactions(
                    TransactionFilter(limit = 500, offset = 0, startDate = start, endDate = end)
                )
            }
            val forecastJob = async { insightsRepository.getForecast() }
            val healthJob = async { insightsRepository.getHealthScore() }
            val achievementsJob = async { insightsRepository.getAchievements() }
            val leaderboardJob = async { insightsRepository.getLeaderboard(10) }

            val txPage = txJob.await().getOrNull()
            val transactions = txPage?.transactions ?: emptyList()

            val income = transactions.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val byCategory = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryName ?: "Uncategorised" }
                .map { (name, list) -> CategorySlice(name, list.sumOf { it.amount }) }
                .sortedByDescending { it.total }

            // Await each job once and store results, then inspect for failures.
            val forecastResult = forecastJob.await()
            val healthResult = healthJob.await()
            val achievementsResult = achievementsJob.await()
            val leaderboardResult = leaderboardJob.await()

            // Track which jobs failed so we can surface an error instead of silently
            // rendering empty lists.
            val failedJobs = listOf(
                txJob to txPage,
                forecastJob to forecastResult,
                healthJob to healthResult,
                achievementsJob to achievementsResult,
                leaderboardJob to leaderboardResult
            ).filter { (job, result) -> result == null && job.exceptionOrNull() != null }

            val allFailed = txPage == null && transactions.isEmpty() &&
                forecastResult == null && healthResult == null &&
                achievementsResult == null && leaderboardResult == null

            _state.value = StatsUiState(
                loading = false,
                income = income,
                expense = expense,
                byCategory = byCategory,
                forecast = forecastResult,
                health = healthResult,
                achievements = achievementsResult ?: emptyList(),
                leaderboard = leaderboardResult ?: emptyList(),
                error = if (allFailed) {
                    failedJobs.firstOrNull()?.let { (job, _) ->
                        job.exceptionOrNull()?.message
                    } ?: "Could not load statistics."
                } else if (failedJobs.isNotEmpty()) {
                    failedJobs.firstOrNull()?.let { (job, _) ->
                        job.exceptionOrNull()?.message
                    } ?: null
                } else null
            )
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
