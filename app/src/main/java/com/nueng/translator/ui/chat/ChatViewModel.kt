package com.nueng.translator.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.ChatMessage
import com.nueng.translator.data.repository.ChatRepository
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

data class ChatUiState(
    val userId: Long = -1L,
    val username: String = "Unknown",
    val isGuest: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages = chatRepository.getRecentMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null

            _uiState.value = ChatUiState(
                userId = userId,
                username = user?.username ?: "Guest",
                isGuest = isGuest
            )

            // Cleanup old messages
            chatRepository.cleanupOldMessages(7)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value

        viewModelScope.launch {
            chatRepository.sendMessage(
                ChatMessage(
                    userId = state.userId,
                    username = state.username,
                    message = text.trim()
                )
            )
        }
    }
}
