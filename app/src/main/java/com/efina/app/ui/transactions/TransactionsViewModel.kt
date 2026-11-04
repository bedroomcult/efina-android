package com.efina.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.TransactionPage
import com.efina.app.domain.repository.AccountRepository
import com.efina.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.efina.app.domain.repository.AuthRepository

data class TransactionsUiState(
    /** Signed-in user, so another member's rows can be badged. */
    val currentUserId: Long? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val saving: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val total: Int = 0,
    val page: TransactionPage? = null,
    val filter: TransactionFilter = TransactionFilter(),
    val error: String? = null
) {
    val isEmpty: Boolean get() = !loading && transactions.isEmpty()
    val hasMore: Boolean get() = page?.hasMore == true
}

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    init {
        // Collect the current user so shared-budget rows can be badged. Wrapped
        // in try/catch: a null delegate during flow collection (for example
        // EncryptedSharedPreferences failing on MIUI) must not kill the app
        // during ViewModel construction.
        viewModelScope.launch {
            try {
                authRepository.currentUserFlow().collect { u ->
                    _state.value = _state.value.copy(currentUserId = u?.id)
                }
            } catch (e: Exception) {
                // currentUserId stays null; family badges simply won't show
            }
        }
    }

    /** accountId -> ISO currency, so rows can render in their account's currency. */
    private var currencyByAccount: Map<Long, String> = emptyMap()

    private suspend fun loadCurrencies() {
        currencyByAccount = accountRepository.getAccounts()
            .getOrDefault(emptyList())
            .associate { it.id to it.currency }
    }

    /** The transactions endpoint does not return currency, so join it here. */
    private fun List<com.efina.app.domain.model.Transaction>.withCurrency() =
        map { tx -> tx.copy(currency = currencyByAccount[tx.accountId] ?: tx.currency) }

    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            if (currencyByAccount.isEmpty()) loadCurrencies()
            repository.getTransactions(_state.value.filter.copy(offset = 0))
                .onSuccess { page ->
                    _state.value = _state.value.copy(
                        loading = false,
                        transactions = page.transactions.withCurrency(),
                        total = page.total,
                        page = page
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }

    /** Appends the next page. The backend returns an envelope with `total`. */
    fun loadMore() {
        val current = _state.value
        if (current.loadingMore || !current.hasMore) return
        _state.value = current.copy(loadingMore = true)
        viewModelScope.launch {
            repository.getTransactions(
                current.filter.copy(offset = current.transactions.size)
            ).onSuccess { page ->
                _state.value = _state.value.copy(
                    loadingMore = false,
                    transactions = _state.value.transactions + page.transactions.withCurrency(),
                    total = page.total,
                    page = page
                )
            }.onFailure {
                _state.value = _state.value.copy(loadingMore = false, error = it.message)
            }
        }
    }

    fun applyFilter(filter: TransactionFilter) {
        _state.value = _state.value.copy(filter = filter.copy(offset = 0))
        load()
    }

    fun setType(type: TransactionType?) {
        applyFilter(_state.value.filter.copy(type = type))
    }

    fun search(query: String) {
        applyFilter(_state.value.filter.copy(search = query.ifBlank { null }))
    }

    fun create(request: CreateTransactionRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createTransaction(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load()
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun update(id: Long, request: UpdateTransactionRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.updateTransaction(id, request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load()
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun transfer(request: CreateTransferRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createTransfer(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load()
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
