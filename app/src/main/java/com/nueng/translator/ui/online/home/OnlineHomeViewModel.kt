package com.nueng.translator.ui.online.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineHomeUiState(
    val displayName: String = "..."
)

@HiltViewModel
class OnlineHomeViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineHomeUiState())
    val uiState: StateFlow<OnlineHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val name = when {
                isGuest    -> "Guest"
                userId > 0 -> userRepository.getUserById(userId)?.username ?: "User"
                else       -> "User"
            }
            _uiState.value = OnlineHomeUiState(displayName = name)
        }
    }
}
