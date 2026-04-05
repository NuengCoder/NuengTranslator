package com.nueng.translator.ui.online.forward

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForwardTarget(
    val id: String,
    val displayName: String,
    val avatarLetter: Char,
    val type: ForwardType,
    val username: String = ""
)

enum class ForwardType { SELF, FRIEND, GROUP }

data class ForwardUiState(
    val myUsername: String = "",
    val myDisplayName: String = "",
    val selfTarget: ForwardTarget? = null,
    val friends: List<ForwardTarget> = emptyList(),
    val groups: List<ForwardTarget> = emptyList(),
    val filteredTargets: List<ForwardTarget> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val sendDone: Boolean = false
)

@HiltViewModel
class ForwardViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForwardUiState())
    val uiState: StateFlow<ForwardUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user    = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val username = user?.username ?: ""

            db.getReference("online_profiles").child(username).get()
                .addOnSuccessListener { snap ->
                    val nick    = snap.child("nickname").getValue(String::class.java) ?: ""
                    val uname   = snap.child("username").getValue(String::class.java) ?: username
                    val display = nick.ifBlank { uname }
                    val self = ForwardTarget(
                        id           = "${username}_${username}",
                        displayName  = "Saved Messages",
                        avatarLetter = 'S',
                        type         = ForwardType.SELF,
                        username     = username
                    )
                    _uiState.value = _uiState.value.copy(
                        myUsername    = username,
                        myDisplayName = display,
                        selfTarget    = self
                    )
                    loadFriendsAndGroups(username)
                }
        }
    }

    private fun loadFriendsAndGroups(username: String) {
        db.getReference("friends").child(username).get()
            .addOnSuccessListener { snap ->
                val friends = mutableListOf<ForwardTarget>()
                for (child in snap.children) {
                    val friendUsername = child.key ?: continue
                    val dName  = child.child("displayName").getValue(String::class.java) ?: friendUsername
                    val chatId = child.child("chatId").getValue(String::class.java)
                        ?: if (username < friendUsername) "${username}_${friendUsername}"
                        else "${friendUsername}_${username}"
                    friends.add(ForwardTarget(
                        id           = chatId,
                        displayName  = dName,
                        avatarLetter = dName.firstOrNull()?.uppercaseChar() ?: '?',
                        type         = ForwardType.FRIEND,
                        username     = friendUsername
                    ))
                }
                // Remove self-chat from friends to avoid duplicate keys
                val myId = _uiState.value.myUsername
                val filtered = friends.filter { it.id != "${myId}_${myId}" }
                _uiState.value = _uiState.value.copy(friends = filtered)
                updateFilter()
            }

        db.getReference("user_groups").child(username).get()
            .addOnSuccessListener { snap ->
                val groups = mutableListOf<ForwardTarget>()
                for (child in snap.children) {
                    val groupId   = child.key ?: continue
                    val groupName = child.child("group_name").getValue(String::class.java) ?: "Group"
                    groups.add(ForwardTarget(
                        id           = groupId,
                        displayName  = groupName,
                        avatarLetter = groupName.firstOrNull()?.uppercaseChar() ?: 'G',
                        type         = ForwardType.GROUP
                    ))
                }
                _uiState.value = _uiState.value.copy(groups = groups, isLoading = false)
                updateFilter()
            }
    }

    private fun updateFilter() {
        val state = _uiState.value
        val query = state.searchQuery
        val all = mutableListOf<ForwardTarget>()
        state.selfTarget?.let { all.add(it) }
        all.addAll(state.friends)
        all.addAll(state.groups)
        val filtered = if (query.isBlank()) all
        else all.filter { it.displayName.contains(query, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(filteredTargets = filtered)
    }

    fun onSearchQuery(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
        updateFilter()
    }

    fun toggleTarget(target: ForwardTarget) {
        val current = _uiState.value.selectedIds.toMutableSet()
        if (target.id in current) current.remove(target.id) else current.add(target.id)
        _uiState.value = _uiState.value.copy(selectedIds = current)
    }

    private fun forwardVoiceToTarget(target: ForwardTarget, msgId: String, sourceChatId: String, isGroup: Boolean) {
        val state = _uiState.value
        val srcRef = if (isGroup)
            db.getReference("group_chats").child(sourceChatId).child("messages").child(msgId)
        else
            db.getReference("friend_chats").child(sourceChatId).child("messages").child(msgId)
        srcRef.get().addOnSuccessListener { snap ->
            val voiceData = snap.child("voiceData").getValue(String::class.java) ?: return@addOnSuccessListener
            val duration  = snap.child("voiceDuration").getValue(Int::class.java) ?: 0
            val payload = mapOf(
                "senderId"      to state.myUsername,
                "senderName"    to state.myDisplayName,
                "dataType"      to "voice",
                "voiceData"     to voiceData,
                "voiceDuration" to duration,
                "text"          to "\uD83C\uDF99 Voice message",
                "isForwarded"   to true,
                "timestamp"     to ServerValue.TIMESTAMP
            )
            when (target.type) {
                ForwardType.SELF, ForwardType.FRIEND ->
                    db.getReference("friend_chats").child(target.id).child("messages").push().setValue(payload)
                ForwardType.GROUP ->
                    db.getReference("group_chats").child(target.id).child("messages").push().setValue(payload)
            }
            _uiState.value = _uiState.value.copy(sendDone = true)
        }
    }

    private fun forwardFileToTarget(target: ForwardTarget, msgId: String, sourceChatId: String, isGroup: Boolean) {
        val state = _uiState.value
        val srcRef = if (isGroup)
            db.getReference("group_chats").child(sourceChatId).child("messages").child(msgId)
        else
            db.getReference("friend_chats").child(sourceChatId).child("messages").child(msgId)
        srcRef.get().addOnSuccessListener { snap ->
            val fileData = snap.child("fileData").getValue(String::class.java) ?: return@addOnSuccessListener
            val fileName = snap.child("fileName").getValue(String::class.java) ?: ""
            val dirName  = snap.child("dirName").getValue(String::class.java) ?: ""
            val fileSize = snap.child("fileSize").getValue(Long::class.java) ?: 0L
            val payload = mapOf(
                "senderId"    to state.myUsername,
                "senderName"  to state.myDisplayName,
                "dataType"    to "file",
                "fileData"    to fileData,
                "fileName"    to fileName,
                "dirName"     to dirName,
                "fileSize"    to fileSize,
                "text"        to "[File: $dirName]",
                "isForwarded" to true,
                "timestamp"   to ServerValue.TIMESTAMP
            )
            when (target.type) {
                ForwardType.SELF, ForwardType.FRIEND ->
                    db.getReference("friend_chats").child(target.id).child("messages").push().setValue(payload)
                ForwardType.GROUP ->
                    db.getReference("group_chats").child(target.id).child("messages").push().setValue(payload)
            }
            _uiState.value = _uiState.value.copy(sendDone = true)
        }
    }

    private fun forwardImageToTarget(target: ForwardTarget, msgId: String, sourceChatId: String, isGroup: Boolean) {
        val state = _uiState.value
        val srcRef = if (isGroup)
            db.getReference("group_chats").child(sourceChatId).child("messages").child(msgId)
        else
            db.getReference("friend_chats").child(sourceChatId).child("messages").child(msgId)
        srcRef.get().addOnSuccessListener { snap ->
            val imageData   = snap.child("imageData").getValue(String::class.java) ?: return@addOnSuccessListener
            val imageWidth  = snap.child("imageWidth").getValue(Int::class.java) ?: 0
            val imageHeight = snap.child("imageHeight").getValue(Int::class.java) ?: 0
            val payload = mapOf(
                "senderId"    to state.myUsername,
                "senderName"  to state.myDisplayName,
                "dataType"    to "image",
                "imageData"   to imageData,
                "imageWidth"  to imageWidth,
                "imageHeight" to imageHeight,
                "text"        to "\uD83D\uDDBC\uFE0F Image",
                "isForwarded" to true,
                "timestamp"   to ServerValue.TIMESTAMP
            )
            when (target.type) {
                ForwardType.SELF, ForwardType.FRIEND ->
                    db.getReference("friend_chats").child(target.id).child("messages").push().setValue(payload)
                ForwardType.GROUP ->
                    db.getReference("group_chats").child(target.id).child("messages").push().setValue(payload)
            }
            _uiState.value = _uiState.value.copy(sendDone = true)
        }
    }

    fun sendForward(text: String) {
        val state = _uiState.value
        if (state.selectedIds.isEmpty()) return
        _uiState.value = _uiState.value.copy(isSending = true)

        val allTargets = mutableListOf<ForwardTarget>()
        state.selfTarget?.let { if (it.id in state.selectedIds) allTargets.add(it) }
        allTargets.addAll(state.friends.filter { it.id in state.selectedIds })
        allTargets.addAll(state.groups.filter { it.id in state.selectedIds })
        val selected = allTargets

        // Detect special forward types: __VOICE__msgId, __FILE__msgId, __IMAGE__msgId
        when {
            text.startsWith("__VOICE__") -> {
                val msgId = text.removePrefix("__VOICE__")
                for (target in selected) forwardVoiceFromAnySource(target, msgId)
            }
            text.startsWith("__FILE__") -> {
                val msgId = text.removePrefix("__FILE__")
                for (target in selected) forwardFileFromAnySource(target, msgId)
            }
            text.startsWith("__IMAGE__") -> {
                val msgId = text.removePrefix("__IMAGE__")
                for (target in selected) forwardImageFromAnySource(target, msgId)
            }
            else -> {
                val payload = mapOf(
                    "senderId"    to state.myUsername,
                    "senderName"  to state.myDisplayName,
                    "dataType"    to "message",
                    "text"        to text,
                    "isForwarded" to true,
                    "timestamp"   to ServerValue.TIMESTAMP
                )
                for (target in selected) {
                    when (target.type) {
                        ForwardType.SELF, ForwardType.FRIEND ->
                            db.getReference("friend_chats").child(target.id).child("messages").push().setValue(payload)
                        ForwardType.GROUP ->
                            db.getReference("group_chats").child(target.id).child("messages").push().setValue(payload)
                    }
                }
                _uiState.value = _uiState.value.copy(sendDone = true)
            }
        }
    }

    private fun forwardVoiceFromAnySource(target: ForwardTarget, msgId: String) {
        val state = _uiState.value
        val friendChatIds = state.friends.map { it.id } + listOf("${state.myUsername}_${state.myUsername}")
        val groupIds = state.groups.map { it.id }
        var found = false
        friendChatIds.forEachIndexed { idx, chatId ->
            if (found) return@forEachIndexed
            db.getReference("friend_chats").child(chatId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardVoiceToTarget(target, msgId, friendChatIds[idx], false)
                    }
                }
        }
        groupIds.forEachIndexed { gi, groupId ->
            if (found) return@forEachIndexed
            db.getReference("group_chats").child(groupId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardVoiceToTarget(target, msgId, groupIds[gi], true)
                    }
                }
        }
    }

    private fun forwardFileFromAnySource(target: ForwardTarget, msgId: String) {
        val state = _uiState.value
        val friendChatIds = state.friends.map { it.id } + listOf("${state.myUsername}_${state.myUsername}")
        val groupIds = state.groups.map { it.id }
        var found = false
        friendChatIds.forEachIndexed { idx, chatId ->
            if (found) return@forEachIndexed
            db.getReference("friend_chats").child(chatId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardFileToTarget(target, msgId, friendChatIds[idx], false)
                    }
                }
        }
        groupIds.forEachIndexed { gi, groupId ->
            if (found) return@forEachIndexed
            db.getReference("group_chats").child(groupId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardFileToTarget(target, msgId, groupIds[gi], true)
                    }
                }
        }
    }

    private fun forwardImageFromAnySource(target: ForwardTarget, msgId: String) {
        val state = _uiState.value
        val friendChatIds = state.friends.map { it.id } + listOf("${state.myUsername}_${state.myUsername}")
        val groupIds = state.groups.map { it.id }
        var found = false
        friendChatIds.forEachIndexed { idx, chatId ->
            if (found) return@forEachIndexed
            db.getReference("friend_chats").child(chatId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardImageToTarget(target, msgId, friendChatIds[idx], false)
                    }
                }
        }
        groupIds.forEachIndexed { gi, groupId ->
            if (found) return@forEachIndexed
            db.getReference("group_chats").child(groupId).child("messages").child(msgId).get()
                .addOnSuccessListener { snap ->
                    if (snap.exists() && !found) {
                        found = true
                        forwardImageToTarget(target, msgId, groupIds[gi], true)
                    }
                }
        }
    }

    fun resetSendDone() { _uiState.value = _uiState.value.copy(sendDone = false, isSending = false, selectedIds = emptySet()) }
}