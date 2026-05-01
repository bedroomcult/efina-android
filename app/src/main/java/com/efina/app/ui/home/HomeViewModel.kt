package com.efina.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.data.remote.api.EfinaApi
import com.efina.app.data.remote.dto.*
import com.efina.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val healthScore: Int? = null,
    val totalBalance: Double = 0.0,
    val currency: String = "IDR"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: EfinaApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getDashboard()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val accounts = body.accounts?.map { it.toDomain() } ?: emptyList()
                    val totalBalance = accounts.sumOf { it.balance }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = accounts,
                        totalBalance = totalBalance,
                        recentTransactions = body.recentTransactions?.map { it.toDomain() }?.take(5) ?: emptyList(),
                        budgets = body.budgets?.map { it.toDomain() } ?: emptyList(),
                        goals = body.goals?.map { it.toDomain() } ?: emptyList(),
                        healthScore = body.healthScore?.score
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load dashboard"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load dashboard"
                )
            }
        }
    }
}
