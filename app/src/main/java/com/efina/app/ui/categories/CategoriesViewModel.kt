package com.efina.app.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.Category
import com.efina.app.domain.model.CreateCategoryRequest
import com.efina.app.domain.model.UpdateCategoryRequest
import com.efina.app.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val categories: List<Category> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesUiState())
    val state: StateFlow<CategoriesUiState> = _state.asStateFlow()

    init {
        // Room is the source of truth: the list redraws when a write lands and
        // is populated offline. The refresh only feeds the mirror.
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _state.value = _state.value.copy(loading = false, categories = categories)
            }
        }
        refresh()
    }

    /** Pull the mirror up to date. Failure is silent: the mirror still has data. */
    fun refresh() {
        viewModelScope.launch {
            repository.getCategories().onFailure {
                if (_state.value.categories.isEmpty()) {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
            }
        }
    }

    /** Kept for callers that used to force a reload. */
    fun load() = refresh()

    fun create(request: CreateCategoryRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createCategory(request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load(); onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun update(id: Long, request: UpdateCategoryRequest, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.updateCategory(id, request)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false)
                    load(); onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
