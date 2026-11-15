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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import androidx.paging.cachedIn
import androidx.paging.PagingData
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

    /** accountId -> ISO currency, so rows can render in their account's currency. */
    private var currencyByAccount: Map<Long, String> = emptyMap()

    /** Declared before init so it is initialized before the launch in init runs. */
    private val _state = MutableStateFlow(TransactionsUiState())
    val state: StateFlow<TransactionsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserFlow().collect { u ->
                _state.value = _state.value.copy(currentUserId = u?.id)
            }
        }
    }

    private suspend fun loadCurrencies() {
        currencyByAccount = accountRepository.getAccounts()
            .getOrDefault(emptyList())
            .associate { it.id to it.currency }
    }

    /** The transactions endpoint does not return currency, so join it here. */
    private fun List<com.efina.app.domain.model.Transaction>.withCurrency() =
        map { tx -> tx.copy(currency = currencyByAccount[tx.accountId] ?: tx.currency) }

    init {
        // Room is the source of truth. The list redraws the moment a write lands
        // locally and is populated offline; the network refresh only feeds the
        // mirror, so its failure is not something the user needs to see.
        viewModelScope.launch {
            repository.observeTransactions().collect { rows ->
                _state.value = _state.value.copy(
                    loading = false,
                    transactions = rows.withCurrency(),
                    total = rows.size,
                )
            }
        }
        refresh()
    }

    /** Pull the mirror up to date. Failure is silent: the mirror still has data. */
    fun refresh() {
        viewModelScope.launch {
            if (currencyByAccount.isEmpty()) loadCurrencies()
            repository.getTransactions(_state.value.filter.copy(offset = 0))
                .onFailure {
                    if (_state.value.transactions.isEmpty()) {
                        _state.value = _state.value.copy(loading = false, error = it.message)
                    }
                }
        }
    }

    /** Kept for callers that used to force a reload. */
    fun load() = refresh()

    /**
     * The paged list.
     *
     * Re-created whenever the filter changes, and cached so a rotation or a trip
     * to another tab does not re-query. Because the mirror holds the whole
     * window, paging is a local query: scrolling never waits and works offline.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val paged: kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<com.efina.app.domain.model.Transaction>> =
        _state.map { it.filter }
            .distinctUntilChanged()
            .flatMapLatest { filter -> repository.pagedTransactions(filter) }
            .cachedIn(viewModelScope)

    /**
     * No-op now.
     *
     * The mirror already holds the whole 12-month window, so there is no next
     * page to fetch. Kept so the screen's scroll listener still compiles; the
     * list simply stops asking for more.
     */
    fun loadMore() = Unit

    fun applyFilter(filter: TransactionFilter) {
        _state.value = _state.value.copy(filter = filter.copy(offset = 0))
        refresh()
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
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
                .onSuccess { }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun transfer(request: CreateTransferRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createTransfer(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
