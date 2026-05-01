package com.efina.app.ui.budgets

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

data class BudgetsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val budgets: List<Budget> = emptyList(),
    val categories: List<Category> = emptyList()
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetsUiState())
    val uiState: StateFlow<BudgetsUiState> = _uiState.asStateFlow()
    
    init {
        loadBudgets()
        loadCategories()
    }
    
    fun loadBudgets() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            budgetRepository.getBudgets()
                .onSuccess { budgets ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        budgets = budgets
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load budgets"
                    )
                }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategories(CategoryType.EXPENSE)
                .onSuccess { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
        }
    }
    
    fun createBudget(request: CreateBudgetRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            budgetRepository.createBudget(request)
                .onSuccess {
                    loadBudgets()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create budget"
                    )
                }
        }
    }
    
    fun deleteBudget(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            budgetRepository.deleteBudget(id)
                .onSuccess {
                    loadBudgets()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete budget"
                    )
                }
        }
    }
}

data class GoalsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val goals: List<Goal> = emptyList(),
    val accounts: List<Account> = emptyList()
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    
    init {
        loadGoals()
        loadAccounts()
    }
    
    fun loadGoals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            goalRepository.getGoals()
                .onSuccess { goals ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        goals = goals
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load goals"
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
    
    fun createGoal(request: CreateGoalRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            goalRepository.createGoal(request)
                .onSuccess {
                    loadGoals()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create goal"
                    )
                }
        }
    }
    
    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            goalRepository.deleteGoal(id)
                .onSuccess {
                    loadGoals()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete goal"
                    )
                }
        }
    }
}
