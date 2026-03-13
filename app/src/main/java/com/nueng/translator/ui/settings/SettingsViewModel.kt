package com.nueng.translator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.data.repository.UserDataRepository
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val user: User? = null,
    val isGuest: Boolean = false,
    val isAdmin: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val userDataRepository: UserDataRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val isDarkMode = preferencesManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val uiLanguage = preferencesManager.uiLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()

            if (userId > 0 && !isGuest) {
                val user = userRepository.getUserById(userId)
                _uiState.value = SettingsUiState(
                    user = user,
                    isGuest = false,
                    isAdmin = user?.role == "admin"
                )
            } else {
                _uiState.value = SettingsUiState(isGuest = true)
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun setUiLanguage(langCode: String) {
        viewModelScope.launch {
            preferencesManager.setUiLanguage(langCode)
        }
    }

    fun deleteMyNoteData() {
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            if (userId > 0) {
                userDataRepository.deleteAllNotes(userId)
                _uiState.value = _uiState.value.copy(deleteSuccess = true)
            }
        }
    }

    fun clearDeleteSuccess() {
        _uiState.value = _uiState.value.copy(deleteSuccess = false)
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }
}
