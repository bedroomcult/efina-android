package com.efina.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.AccountMembers
import com.efina.app.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val error: String? = null,
    val members: AccountMembers? = null,
    val membersLoading: Boolean = false
) {
    val totalBalance: Double get() = accounts.sumOf { it.balance }
    val sharedCount: Int get() = accounts.count { it.isShared }
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AccountsUiState())
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    init {
        // Room is the source of truth: the list redraws the moment a write lands
        // locally, and it is populated offline. The network refresh below only
        // feeds the mirror, so its failure is not an error the user needs to see.
        viewModelScope.launch {
            repository.observeAccounts().collect { accounts ->
                _state.value = _state.value.copy(loading = false, accounts = accounts)
            }
        }
        refresh()
    }

    /** Pull the mirror up to date. Failure is silent: the mirror still has data. */
    fun refresh() {
        viewModelScope.launch {
            repository.getAccounts().onFailure {
                if (_state.value.accounts.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
            }
        }
    }

    /** Kept for callers that used to force a reload. */
    fun load() = refresh()

    fun create(request: CreateAccountRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createAccount(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun update(id: Long, request: UpdateAccountRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.updateAccount(id, request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteAccount(id)
                .onSuccess { }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun setShared(id: Long, familyId: Long?) {
        viewModelScope.launch {
            repository.setShared(id, familyId)
                .onSuccess { refresh() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun loadMembers(id: Long) {
        _state.value = _state.value.copy(membersLoading = true, members = null)
        viewModelScope.launch {
            repository.getMembers(id)
                .onSuccess { _state.value = _state.value.copy(membersLoading = false, members = it) }
                .onFailure {
                    _state.value = _state.value.copy(membersLoading = false, error = it.message)
                }
        }
    }

    fun shareWith(id: Long, userId: Long, role: AccountShareRole) {
        viewModelScope.launch {
            repository.shareWith(id, userId, role)
                .onSuccess { loadMembers(id) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun removeMember(id: Long, userId: Long) {
        viewModelScope.launch {
            repository.removeMember(id, userId)
                .onSuccess { loadMembers(id) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
