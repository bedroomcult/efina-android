package com.efina.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efina.app.domain.model.UpdateSettingsRequest
import com.efina.app.ui.theme.ThemePreference
import com.efina.app.domain.model.User
import com.efina.app.domain.model.UserSettings
import com.efina.app.domain.repository.AuthRepository
import com.efina.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val settings: UserSettings? = null,
    val user: User? = null,
    val error: String? = null,
    val savedMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            val userResult = authRepository.refreshCurrentUser().getOrNull()
            settingsRepository.getSettings()
                .onSuccess {
                    // Push the saved preference into the theme so a restart shows
                    // the user's choice rather than the default.
                    ThemePreference.apply(it.theme)
                    _state.value = _state.value.copy(loading = false, settings = it, user = userResult)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, user = userResult, error = it.message)
                }
        }
    }

    fun update(request: UpdateSettingsRequest) {
        _state.value = _state.value.copy(saving = true, error = null, savedMessage = null)
        viewModelScope.launch {
            settingsRepository.updateSettings(request)
                .onSuccess {
                    ThemePreference.apply(it.theme)
                    _state.value = _state.value.copy(
                        saving = false,
                        settings = it,
                        savedMessage = "Saved"
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(saving = false, error = it.message)
                }
        }
    }

    fun changePassword(current: String, next: String, onDone: () -> Unit) {
        _state.value = _state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            settingsRepository.changePassword(current, next)
                .onSuccess {
                    _state.value = _state.value.copy(saving = false, savedMessage = "Password updated")
                    onDone()
                }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }

    fun clearMessages() {
        _state.value = _state.value.copy(error = null, savedMessage = null)
    }
}
