package com.efina.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.User
import com.efina.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    /** null while the stored session is still being checked. */
    val checked: Boolean = false
) {
    val isLoggedIn: Boolean get() = user != null
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        restore()
    }

    /**
     * Restores the session from encrypted storage immediately, then refreshes the
     * profile in the background. The UI renders on the stored token so a slow or
     * absent network never blocks the first frame — the same fix the web client
     * needed for its blank-screen complaint.
     */
    private fun restore() {
        if (!repository.isLoggedIn()) {
            _state.value = AuthUiState(checked = true)
            return
        }
        _state.value = AuthUiState(checked = true, loading = true)
        viewModelScope.launch {
            repository.refreshCurrentUser()
                .onSuccess { _state.value = AuthUiState(checked = true, user = it) }
                .onFailure {
                    // Token exists but the profile call failed: stay logged in and
                    // surface the message; only a 401 should log out.
                    val message = it.message.orEmpty()
                    val unauthorized = message.contains("401", true) ||
                        message.contains("Unauthorized", true) ||
                        message.contains("Session expired", true)
                    if (unauthorized) {
                        repository.logout()
                        _state.value = AuthUiState(checked = true, error = "Session expired, please sign in again.")
                    } else {
                        _state.value = AuthUiState(checked = true, user = null, error = message)
                    }
                }
        }
    }

    fun login(email: String, password: String) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { _state.value = AuthUiState(checked = true, user = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.register(name, email, password)
                .onSuccess { _state.value = AuthUiState(checked = true, user = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _state.value = AuthUiState(checked = true)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
