package com.efina.app.ui.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.AccountRepository
import com.efina.app.domain.repository.ScheduledRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduledUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val scheduled: List<ScheduledTransaction> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val error: String? = null
) {
    val activeCount: Int get() = scheduled.count { it.isActive }
    val monthlyTotal: Double get() = scheduled
        .filter { it.isActive && it.frequency == "monthly" && it.type == TransactionType.EXPENSE }
        .sumOf { it.amount }
}

@HiltViewModel
class ScheduledViewModel @Inject constructor(
    private val repository: ScheduledRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = runCatching { MutableStateFlow(ScheduledUiState()) }
        .getOrDefault { MutableStateFlow(ScheduledUiState()) }
    val state: StateFlow<ScheduledUiState> = _state.asStateFlow()

    init { try { load() } catch (e: Exception) {
        _state.value = _state.value.copy(loading = false, error = "Init failed: ${e.message}")
    } }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val accounts = accountRepository.getAccounts().getOrDefault(emptyList())
            repository.getScheduled()
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, scheduled = it, accounts = accounts)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, accounts = accounts, error = it.message)
                }
        }
    }

    fun create(request: CreateScheduledRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createScheduled(request)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteScheduled(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
