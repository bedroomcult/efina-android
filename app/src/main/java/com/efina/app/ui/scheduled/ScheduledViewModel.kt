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

    private val _state = MutableStateFlow(ScheduledUiState())
    val state: StateFlow<ScheduledUiState> = _state.asStateFlow()

    init {
        // Room is the source of truth for both lists, so the screen is populated
        // offline and redraws when a write lands.
        viewModelScope.launch {
            repository.observeScheduled().collect { rows ->
                _state.value = _state.value.copy(loading = false, scheduled = rows)
            }
        }
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                _state.value = _state.value.copy(accounts = accounts)
            }
        }
        refresh()
    }

    /** Pull the mirror up to date. Failure is silent: the mirror still has data. */
    fun refresh() {
        viewModelScope.launch {
            repository.getScheduled().onFailure {
                if (_state.value.scheduled.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
            }
            accountRepository.getAccounts()
        }
    }

    /** Kept for callers that used to force a reload. */
    fun load() = refresh()

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
