package com.efina.app.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.FamilyGroup
import com.efina.app.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val families: List<FamilyGroup> = emptyList(),
    val inviteCode: String? = null,
    val error: String? = null
) {
    /** Free plan allows one family; the backend enforces it too. */
    val atLimit: Boolean get() = families.isNotEmpty()
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val repository: FamilyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FamilyUiState())
    val state: StateFlow<FamilyUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.getFamilies()
                .onSuccess { _state.value = _state.value.copy(loading = false, families = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun create(name: String, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createFamily(name)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun createInvite(familyId: Long) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.createInvite(familyId)
                .onSuccess { _state.value = _state.value.copy(saving = false, inviteCode = it) }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun acceptInvite(code: String, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            repository.acceptInvite(code)
                .onSuccess { _state.value = _state.value.copy(saving = false); load(); onDone() }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun removeMember(familyId: Long, userId: Long) {
        viewModelScope.launch {
            repository.removeMember(familyId, userId)
                .onSuccess { load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun dismissInvite() { _state.value = _state.value.copy(inviteCode = null) }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}
