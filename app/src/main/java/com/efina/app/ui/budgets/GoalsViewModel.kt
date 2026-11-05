package com.efina.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.AccountRepository
import com.efina.app.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val error: String? = null
) {
    val totalTarget: Double get() = goals.sumOf { it.targetAmount }
    val totalSaved: Double get() = goals.sumOf { it.currentAmount }
    val completedCount: Int get() = goals.count { it.isComplete }
}

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsUiState())
    val state: StateFlow<GoalsUiState> = _state.asStateFlow()

    init { try { load() } catch (e: Exception) {
        _state.value = _state.value.copy(loading = false, error = "Init failed: ${e.message}")
    } }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val accounts = accountRepository.getAccounts().getOrDefault(emptyList())
            repository.getGoals()
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, goals = it, accounts = accounts)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, accounts = accounts, error = it.message)
                }
        }
    }

    fun create(request: CreateGoalRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createGoal(request)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun update(id: Long, request: UpdateGoalRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.updateGoal(id, request)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    /** Moves money out of an account into the goal; the API enforces write access. */
    fun addFunds(id: Long, amount: Double, accountId: Long, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.addFunds(id, amount, accountId)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
