package com.efina.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.data.remote.SessionEvents
import com.efina.app.data.repository.ApiException
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
    /** null while the stored session is being checked. */
    val checked: Boolean = false,
    /**
     * True when a token is stored, regardless of whether the profile has loaded.
     * The start destination keys off this, not off `user`: offline, the profile
     * call fails but the session is still valid, and sending the user to the login
     * form would discard a working token.
     */
    val hasSession: Boolean = false
) {
    val isLoggedIn: Boolean get() = hasSession
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        // Collector first: restore() fires a network call, and a 401 arriving
        // before anyone is listening would be dropped (SharedFlow with replay=0
        // and no subscriber). Order here is load-bearing.
        observeUnauthorized()
        observeCsrfExpiry()
        restore()
    }

    /**
     * Any 401 from any repository logs out, mirroring the web client's
     * auth:unauthorized event. Collected for the whole ViewModel lifetime.
     */
    /**
     * CSRF tokens expire after 24 h while the session lives 7 days. Without a
     * refresh every write starts returning 403 after a day and the only way back
     * is to sign out. Refreshed on start and whenever the server rejects one.
     */
    private fun observeCsrfExpiry() {
        viewModelScope.launch {
            SessionEvents.csrfExpired.collect {
                if (_state.value.isLoggedIn) repository.refreshCsrfToken()
            }
        }
    }

    private fun observeUnauthorized() {
        viewModelScope.launch {
            SessionEvents.unauthorized.collect {
                if (_state.value.isLoggedIn) {
                    repository.logout()
                    _state.value = AuthUiState(
                        checked = true,
                        error = "Session expired, please sign in again."
                    )
                }
            }
        }
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
        _state.value = AuthUiState(checked = true, loading = true, hasSession = true)
        viewModelScope.launch {
            repository.refreshCurrentUser()
                .onSuccess { _state.value = AuthUiState(checked = true, user = it, hasSession = true) }
                .onFailure {
                    // A 401 already cleared the session via SessionEvents; anything
                    // else (offline, 500) must not destroy a valid token.
                    val status = (it as? ApiException)?.status
                    if (status == 401) {
                        _state.value = AuthUiState(
                            checked = true,
                            error = "Session expired, please sign in again."
                        )
                    } else {
                        // Keep the session: the token is still good, only the
                        // profile refresh failed. Screens will surface the error.
                        _state.value = AuthUiState(
                            checked = true,
                            user = null,
                            hasSession = true,
                            error = it.message
                        )
                    }
                }
        }
    }

    fun login(email: String, password: String) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { _state.value = AuthUiState(checked = true, user = it, hasSession = true) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.register(name, email, password)
                .onSuccess { _state.value = AuthUiState(checked = true, user = it, hasSession = true) }
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
