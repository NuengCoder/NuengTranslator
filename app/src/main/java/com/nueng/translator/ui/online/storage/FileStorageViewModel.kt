package com.nueng.translator.ui.online.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import com.nueng.translator.data.repository.UserDataRepository
import com.nueng.translator.data.repository.UserDirectoryRepository
import com.nueng.translator.util.NtfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedFile(
    val id: String = "",
    val senderName: String = "",
    val dirName: String = "",
    val fileName: String = "",
    val fileData: String = "",
    val fileSize: Long = 0L,
    val timestamp: Long = 0L
)

data class FileStorageUiState(
    val ownerName: String = "",
    val files: List<SharedFile> = emptyList(),
    val isLoading: Boolean = true,
    val snackMessage: String = "",
    val isImporting: Boolean = false
)

@HiltViewModel
class FileStorageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userDataRepository: UserDataRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileStorageUiState())
    val uiState: StateFlow<FileStorageUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private var listener: ValueEventListener? = null
    private var listenerRef: com.google.firebase.database.DatabaseReference? = null
    private var myRoomUserId = -1L

    /**
     * @param chatType  "friend" or "group"
     * @param chatId    For groups: the groupId.
     *                  For friends: the friendUserId (we build the combined chat ID internally).
     * @param ownerName Display name for the title bar.
     */
    fun init(chatType: String, chatId: String, ownerName: String) {
        _uiState.value = _uiState.value.copy(ownerName = ownerName, isLoading = true)

        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            myRoomUserId = userId

            val myId = if (userId > 0 && !isGuest) userRepository.getUserById(userId)?.username ?: "" else ""

            // For friend chats, build the combined chatId (same logic as FriendChatViewModel)
            val resolvedChatId = when (chatType) {
                "friend" -> buildChatId(myId, chatId)
                else     -> chatId  // group chats use groupId directly
            }

            // Determine RTDB path
            val messagesPath = when (chatType) {
                "friend" -> "friend_chats"
                "group"  -> "group_chats"
                else     -> "friend_chats"
            }

            listenerRef = db.getReference(messagesPath).child(resolvedChatId).child("messages")
            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val files = mutableListOf<SharedFile>()
                    for (child in snapshot.children) {
                        val dataType = child.child("dataType").getValue(String::class.java) ?: ""
                        val msgType  = child.child("msgType").getValue(String::class.java) ?: ""
                        if (dataType != "file" && msgType != "file") continue

                        val fileData = child.child("fileData").getValue(String::class.java) ?: ""
                        if (fileData.isBlank()) continue

                        files.add(SharedFile(
                            id         = child.key ?: "",
                            senderName = child.child("senderName").getValue(String::class.java) ?: "",
                            dirName    = child.child("dirName").getValue(String::class.java) ?: "",
                            fileName   = child.child("fileName").getValue(String::class.java) ?: "",
                            fileData   = fileData,
                            fileSize   = child.child("fileSize").getValue(Long::class.java) ?: 0L,
                            timestamp  = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        ))
                    }
                    _uiState.value = _uiState.value.copy(
                        files     = files.sortedByDescending { it.timestamp },
                        isLoading = false
                    )
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
            listenerRef!!.addValueEventListener(listener!!)
        }
    }

    /** Same logic as FriendChatViewModel.buildChatId — sorted pair */
    private fun buildChatId(a: String, b: String): String =
        if (a < b) a + "_" + b else b + "_" + a

    fun importFile(file: SharedFile) {
        if (file.fileData.isBlank() || myRoomUserId <= 0) return
        _uiState.value = _uiState.value.copy(isImporting = true)

        viewModelScope.launch {
            try {
                val result = NtfImporter.import(
                    json          = file.fileData,
                    userId        = myRoomUserId,
                    directoryRepo = userDirectoryRepository,
                    userDataRepo  = userDataRepository
                )
                _uiState.value = _uiState.value.copy(
                    isImporting  = false,
                    snackMessage = if (result.success)
                        "Imported " + result.directoryName + " (" + result.wordCount + " words) into My Note!"
                    else
                        "Import failed: " + result.error
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting  = false,
                    snackMessage = "Import failed: " + (e.message ?: "unknown error")
                )
            }
        }
    }

    fun clearSnackMessage() {
        _uiState.value = _uiState.value.copy(snackMessage = "")
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { listenerRef?.removeEventListener(it) }
    }
}
