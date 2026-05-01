package com.efina.app.ui.transactions

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

data class TransactionsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val transactions: List<Transaction> = emptyList(),
    val total: Int = 0,
    val filter: TransactionFilter = TransactionFilter(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    init {
        loadTransactions()
        loadAccounts()
        loadCategories()
    }
    
    fun loadTransactions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            transactionRepository.getTransactions(_uiState.value.filter)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        transactions = response.transactions,
                        total = response.total
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load transactions"
                    )
                }
        }
    }
    
    private fun loadAccounts() {
        viewModelScope.launch {
            accountRepository.getAccounts()
                .onSuccess { accounts ->
                    _uiState.value = _uiState.value.copy(accounts = accounts)
                }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories()
                .onSuccess { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
        }
    }
    
    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.transactions.size >= current.total) return
        
        val newOffset = current.filter.offset + current.filter.limit
        val newFilter = current.filter.copy(offset = newOffset)
        _uiState.value = current.copy(filter = newFilter)
        
        viewModelScope.launch {
            transactionRepository.getTransactions(newFilter)
                .onSuccess { response ->
                    _uiState.value = current.copy(
                        transactions = current.transactions + response.transactions,
                        total = response.total,
                        filter = newFilter
                    )
                }
        }
    }
    
    fun updateFilter(filter: TransactionFilter) {
        _uiState.value = _uiState.value.copy(filter = filter.copy(offset = 0))
        loadTransactions()
    }
    
    fun search(query: String) {
        updateFilter(_uiState.value.filter.copy(search = query.ifBlank { null }))
    }
    
    fun filterByType(type: TransactionType?) {
        updateFilter(_uiState.value.filter.copy(type = type))
    }
    
    fun filterByAccount(accountId: Long?) {
        updateFilter(_uiState.value.filter.copy(accountId = accountId))
    }
    
    fun filterByCategory(categoryId: Long?) {
        updateFilter(_uiState.value.filter.copy(categoryId = categoryId))
    }
    
    fun createTransaction(request: CreateTransactionRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            transactionRepository.createTransaction(request)
                .onSuccess {
                    loadTransactions()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create transaction"
                    )
                }
        }
    }
    
    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            transactionRepository.deleteTransaction(id)
                .onSuccess {
                    loadTransactions()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete transaction"
                    )
                }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
