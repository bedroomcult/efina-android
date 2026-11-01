package com.efina.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.*
import com.efina.app.domain.repository.BudgetRepository
import com.efina.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BudgetsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null
) {
    val totalBudget: Double get() = budgets.sumOf { it.amount }
    val totalSpent: Double get() = budgets.sumOf { it.spent }
    val overBudgetCount: Int get() = budgets.count { it.isOverBudget }
}

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val repository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BudgetsUiState())
    val state: StateFlow<BudgetsUiState> = _state.asStateFlow()

    init { load() }

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

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteBudget(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
