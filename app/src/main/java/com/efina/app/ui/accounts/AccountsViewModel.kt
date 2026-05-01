package com.efina.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val accounts: List<Account> = emptyList(),
    val totalBalance: Double = 0.0,
    val currency: String = "IDR"
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    
    init {
        loadAccounts()
    }
    
    fun loadAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            accountRepository.getAccounts()
                .onSuccess { accounts ->
                    val total = accounts.sumOf { it.balance }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accounts = accounts,
                        totalBalance = total
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load accounts"
                    )
                }
        }
    }
    
    fun createAccount(request: CreateAccountRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            accountRepository.createAccount(request)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create account"
                    )
                }
        }
    }
    
    fun updateAccount(id: Long, request: UpdateAccountRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            accountRepository.updateAccount(id, request)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to update account"
                    )
                }
        }
    }
    
    fun deleteAccount(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            accountRepository.deleteAccount(id)
                .onSuccess {
                    loadAccounts()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete account"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
