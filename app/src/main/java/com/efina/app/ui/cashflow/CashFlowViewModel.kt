package com.efina.app.ui.cashflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.repository.TransactionRepository
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.model.TransactionFilter
import com.efina.app.domain.model.TransactionType
import com.efina.app.ui.components.charts.CashFlowPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class CashFlowUiState(
    val loading: Boolean = true,
    val points: List<CashFlowPoint> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val error: String? = null
)

@HiltViewModel
class CashFlowViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CashFlowUiState())
    val state: StateFlow<CashFlowUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val (start, end) = dateRange()

            val result = transactionRepository.getTransactions(
                TransactionFilter(
                    limit = 1000,
                    offset = 0,
                    startDate = start,
                    endDate = end
                )
            )

            val transactions = result.getOrNull()?.transactions ?: emptyList()

            // Filter out transfers
            val filtered = transactions.filter { tx ->
                tx.type != TransactionType.TRANSFER && tx.transferPairId == null
            }

            // Aggregate daily net spend
            val dailyNet = mutableMapOf<String, Double>()
            filtered.forEach { tx ->
                val date = tx.date.substring(0, 10) // YYYY-MM-DD
                val amount = when (tx.type) {
                    TransactionType.INCOME -> tx.amount
                    TransactionType.EXPENSE -> -tx.amount
                    else -> 0.0
                }
                dailyNet[date] = (dailyNet[date] ?: 0.0) + amount
            }

            // Fill in all dates in range and compute cumulative
            val allDates = generateDateRange(start, end)
            val sortedDates = allDates.sorted()
            var cumulative = 0.0
            val points = mutableListOf<CashFlowPoint>()

            sortedDates.forEach { date ->
                cumulative += dailyNet[date] ?: 0.0
                points.add(CashFlowPoint(
                    date = date,
                    displayDate = formatDisplayDate(date),
                    value = cumulative
                ))
            }

            val totalIncome = filtered
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val totalExpense = filtered
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            _state.value = CashFlowUiState(
                loading = false,
                points = points,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                error = null
            )
        }
    }

    private fun dateRange(): Pair<String, String> {
        val end = Calendar.getInstance()
        val start = Calendar.getInstance()
        start.add(Calendar.DAY_OF_MONTH, -29)

        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return "${fmt.format(start.time)}" to "${fmt.format(end.time)}"
    }

    private fun generateDateRange(start: String, end: String): List<String> {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startDate = input.parse(start) ?: return emptyList()
        val endDate = input.parse(end) ?: return emptyList()

        val dates = mutableListOf<String>()
        val current = Calendar.getInstance()
        current.time = startDate
        val endCal = Calendar.getInstance()
        endCal.time = endDate

        while (!current.after(endCal)) {
            current.add(Calendar.DAY_OF_MONTH, 1)
            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(current.time)
            dates.add(formatted)
        }

        return dates
    }

    private fun formatDisplayDate(date: String): String {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("MMM dd", Locale.getDefault())
        return try {
            output.format(input.parse(date))
        } catch (e: Exception) {
            date
        }
    }
}
