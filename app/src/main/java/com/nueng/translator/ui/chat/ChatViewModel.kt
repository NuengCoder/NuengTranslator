package com.nueng.translator.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.model.FirebaseChatMessage
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val uniqueId: String = "",
    val username: String = "Unknown",
    val isGuest: Boolean = false,
    val isConnected: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<FirebaseChatMessage>>(emptyList())
    val messages: StateFlow<List<FirebaseChatMessage>> = _messages.asStateFlow()

    private val database = FirebaseDatabase.getInstance("https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app")
    private val messagesRef = database.getReference("chat_messages")

    private var childListener: ChildEventListener? = null

    init {
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val username = user?.username ?: "Guest"

            // Use username as unique identifier (unique across all devices)
            _uiState.value = ChatUiState(
                uniqueId = username,
                username = username,
                isGuest = isGuest
            )
        }

        listenForMessages()
    }

    private fun listenForMessages() {
        val query = messagesRef.orderByChild("timestamp").limitToLast(100)

        childListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(FirebaseChatMessage::class.java)
                if (msg != null) {
                    val withId = msg.copy(id = snapshot.key ?: "")
                    val current = _messages.value.toMutableList()
                    if (current.none { it.id == withId.id }) {
                        current.add(withId)
                        current.sortByDescending { it.timestamp }
                        _messages.value = current
                    }
                }
                _uiState.value = _uiState.value.copy(isConnected = true)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key ?: return
                _messages.value = _messages.value.filter { it.id != key }
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatVM", "Firebase cancelled: ${error.message}")
                _uiState.value = _uiState.value.copy(isConnected = false)
            }
        }

        query.addChildEventListener(childListener!!)
        _uiState.value = _uiState.value.copy(isConnected = true)
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val state = _uiState.value

        val messageRef = messagesRef.push()
        val message = mapOf(
            "senderName" to state.username,
            "message" to text.trim(),
            "messageType" to "text",
            "timestamp" to ServerValue.TIMESTAMP
        )

        messageRef.setValue(message)
            .addOnFailureListener { e ->
                Log.e("ChatVM", "Send failed: ${e.message}")
            }
    }

    override fun onCleared() {
        super.onCleared()
        childListener?.let { messagesRef.removeEventListener(it) }
    }
}
