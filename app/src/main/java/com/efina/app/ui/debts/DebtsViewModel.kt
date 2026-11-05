package com.efina.app.ui.debts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.AccountRepository
import com.efina.app.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebtsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val debts: List<Debt> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val error: String? = null
) {
    val owedByMe: Double get() = debts.filter { it.type == DebtType.BORROW }
        .sumOf { it.remainingAmount }
    val owedToMe: Double get() = debts.filter { it.type == DebtType.LEND }
        .sumOf { it.remainingAmount }
    val activeCount: Int get() = debts.count { !it.isSettled }
}

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repository: DebtRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _state = runCatching { MutableStateFlow(DebtsUiState()) }
        .getOrDefault { MutableStateFlow(DebtsUiState()) }
    val state: StateFlow<DebtsUiState> = _state.asStateFlow()

    init { try { load() } catch (e: Exception) {
        _state.value = _state.value.copy(loading = false, error = "Init failed: ${e.message}")
    } }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val accounts = accountRepository.getAccounts().getOrDefault(emptyList())
            repository.getDebts()
                .onSuccess {
                    _state.value = _state.value.copy(loading = false, debts = it, accounts = accounts)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, accounts = accounts, error = it.message)
                }
        }
    }

    fun create(request: CreateDebtRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createDebt(request)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    /**
     * Records a payment. `create_transaction` is only meaningful when an account
     * is chosen, because the backend books the expense/income against it.
     */
    fun pay(id: Long, amount: Double, accountId: Long?, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.payDebt(
                id,
                PayDebtRequest(
                    amount = amount,
                    accountId = accountId,
                    createTransaction = accountId != null
                )
            ).onSuccess {
                _state.value = _state.value.copy(saving = false); load(); onDone()
            }.onFailure {
                _state.value = _state.value.copy(saving = false, error = it.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteDebt(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
