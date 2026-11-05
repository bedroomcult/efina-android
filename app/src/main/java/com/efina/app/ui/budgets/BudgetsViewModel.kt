package com.efina.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.BudgetRepository
import com.efina.app.domain.repository.CategoryRepository
import com.efina.app.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.efina.app.domain.repository.TransactionRepository
import com.efina.app.domain.model.TransactionType
import com.efina.app.domain.model.TransactionFilter
import com.efina.app.domain.model.Transaction
import com.efina.app.domain.repository.AuthRepository

data class BudgetsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** Transactions for the budget currently open in the detail sheet. */
    val detailTransactions: List<Transaction> = emptyList(),
    val detailLoading: Boolean = false,
    /** Signed-in user, so a row recorded by another member can be badged. */
    val currentUserId: Long? = null,
    val error: String? = null
) {
    val totalBudget: Double get() = budgets.sumOf { it.amount }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
    val overBudgetCount: Int get() = budgets.count { it.isOverBudget }
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val familyRepository: FamilyRepository,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetsUiState())
    val state: StateFlow<BudgetsUiState> = _state.asStateFlow()

    init {
        // Collect the current user so shared-budget rows can be badged. Wrapped
        // in try/catch: a null delegate during flow collection (for example
        // EncryptedSharedPreferences failing on MIUI) must not kill the app
        // during ViewModel construction.
        viewModelScope.launch {
            try {
                authRepository.currentUserFlow().collect { user ->
                    _state.value = _state.value.copy(currentUserId = user?.id)
                }
            } catch (e: Exception) {
                // currentUserId stays null; family badges simply won't show
            }
        }
        try { load() } catch (e: Exception) {
            _state.value = _state.value.copy(loading = false, error = "Init failed: ${e.message}")
        }
    }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val categories = categoryRepository.getCategories().getOrDefault(emptyList())
            repository.getBudgets()
                .onSuccess {
                    _state.value = _state.value.copy(
                        loading = false, budgets = it, categories = categories
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false, categories = categories, error = it.message
                    )
                }
        }
    }

    fun create(request: CreateBudgetRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createBudget(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false); load(); onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun update(id: Long, request: UpdateBudgetRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.updateBudget(id, request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false); load(); onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    /**
     * Toggle sharing.
     *
     * An unshare sends null. A share resolves the caller's family first: a personal budget
     * carries familyId = null, so reading the target family off the budget itself would send
     * null and silently do nothing. Matches the web client, which uses useFamilies.
     */
    fun toggleSharing(budget: Budget, onDone: () -> Unit = {}) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            val targetFamilyId = if (budget.isShared) {
                null
            } else {
                val families = familyRepository.getFamilies().getOrDefault(emptyList())
                if (families.isEmpty()) {
                    _state.value = _state.value.copy(
                        saving = false,
                        error = "Join a family before sharing a budget"
                    )
                    return@launch
                }
                families.first().id
            }
            repository.setBudgetSharing(id = budget.id, familyId = targetFamilyId)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load()
                    onDone()
                }
                .onFailure {
                    _state.value = _state.value.copy(saving = false, error = it.message)
                }
        }
    }

    /**
     * Loads the transactions that make up a budget's spend.
     *
     * Filtered by the budget's category (including subcategories) over its period, exactly
     * like the total on the card, so the list and the figure agree. Rows recorded by another
     * household member on a shared account come back too — they are part of the pooled total.
     */
    fun loadDetail(budget: Budget) {
        _state.value = _state.value.copy(detailLoading = true, detailTransactions = emptyList())
        viewModelScope.launch {
            transactionRepository.getTransactions(
                TransactionFilter(
                    limit = 100,
                    categoryId = budget.categoryId,
                    includeSubcategories = true,
                    startDate = budget.periodStart,
                    endDate = budget.periodEnd,
                    // Match the figure exactly: a shared budget counts family-tagged rows only.
                    familyOnly = budget.isShared,
                    type = TransactionType.EXPENSE
                )
            ).onSuccess {
                _state.value = _state.value.copy(
                    detailLoading = false,
                    detailTransactions = it.transactions
                )
            }.onFailure {
                _state.value = _state.value.copy(detailLoading = false, error = it.message)
            }
        }
    }

    fun clearDetail() {
        _state.value = _state.value.copy(detailTransactions = emptyList(), detailLoading = false)
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
