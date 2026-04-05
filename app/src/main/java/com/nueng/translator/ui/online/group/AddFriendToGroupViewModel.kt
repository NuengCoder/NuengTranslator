package com.nueng.translator.ui.online.group

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

data class InvitableFriend(
    val username: String,
    val displayName: String,
    val avatarLetter: Char,
    val isSelected: Boolean = false,
    val alreadyMember: Boolean = false
)

data class AddFriendToGroupUiState(
    val groupId: String = "",
    val groupName: String = "",
    val myUsername: String = "",
    val myRole: String = "member",
    val friends: List<InvitableFriend> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val sendDone: Boolean = false,
    val snackMessage: String = ""
)

@HiltViewModel
class AddFriendToGroupViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendToGroupUiState())
    val uiState: StateFlow<AddFriendToGroupUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    fun load(groupId: String) {
        viewModelScope.launch {
            val userId   = preferencesManager.loggedInUserId.first()
            val isGuest  = preferencesManager.isGuest.first()
            val myUsername = if (userId > 0 && !isGuest)
                userRepository.getUserById(userId)?.username ?: "" else ""
            _uiState.value = _uiState.value.copy(groupId = groupId, myUsername = myUsername, isLoading = true)

            // Load group info
            db.getReference("group_chats").child(groupId).get().addOnSuccessListener { snap ->
                val name = snap.child("name").getValue(String::class.java) ?: "Group"
                _uiState.value = _uiState.value.copy(groupName = name)
            }

            // Load my role + current members
            db.getReference("group_members").child(groupId).get().addOnSuccessListener { mSnap ->
                val currentMembers = mSnap.children.mapNotNull { it.key }.toSet()
                val myRole = mSnap.child(myUsername).child("role").getValue(String::class.java) ?: "member"
                _uiState.value = _uiState.value.copy(myRole = myRole)

                // Load friends
                if (myUsername.isNotEmpty()) {
                    db.getReference("friends").child(myUsername).get().addOnSuccessListener { fSnap ->
                        val friendIds = fSnap.children.mapNotNull { it.key }
                            .filter { it.isNotEmpty() && it != myUsername }
                        if (friendIds.isEmpty()) { _uiState.value = _uiState.value.copy(isLoading = false); return@addOnSuccessListener }
                        val result = mutableListOf<InvitableFriend>()
                        var loaded = 0
                        for (fId in friendIds) {
                            db.getReference("online_profiles").child(fId).get().addOnSuccessListener { pSnap ->
                                val nick    = pSnap.child("nickname").getValue(String::class.java) ?: ""
                                val display = nick.ifBlank { fId }
                                val letter  = display.firstOrNull()?.uppercaseChar() ?: '?'
                                result.add(InvitableFriend(fId, display, letter, alreadyMember = fId in currentMembers))
                                loaded++
                                if (loaded == friendIds.size) {
                                    _uiState.value = _uiState.value.copy(
                                        friends = result.sortedBy { it.displayName }, isLoading = false)
                                }
                            }.addOnFailureListener {
                                result.add(InvitableFriend(fId, fId, fId.firstOrNull()?.uppercaseChar() ?: '?', alreadyMember = fId in currentMembers))
                                loaded++
                                if (loaded == friendIds.size) {
                                    _uiState.value = _uiState.value.copy(
                                        friends = result.sortedBy { it.displayName }, isLoading = false)
                                }
                            }
                        }
                    }.addOnFailureListener { _uiState.value = _uiState.value.copy(isLoading = false) }
                } else { _uiState.value = _uiState.value.copy(isLoading = false) }
            }.addOnFailureListener { _uiState.value = _uiState.value.copy(isLoading = false) }
        }
    }

    fun toggle(username: String) {
        val updated = _uiState.value.friends.map {
            if (it.username == username && !it.alreadyMember) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(friends = updated)
    }

    fun sendInvites() {
        val state    = _uiState.value
        val selected = state.friends.filter { it.isSelected }
        if (selected.isEmpty() || state.groupId.isEmpty()) return
        _uiState.value = _uiState.value.copy(isSending = true)

        val isAdminOrCreator = state.myRole in listOf("admin","creator")

        for (friend in selected) {
            // Build chatId between me and the friend
            val chatId = if (state.myUsername < friend.username)
                state.myUsername + "_" + friend.username
            else friend.username + "_" + state.myUsername

            // Send a group_invite message card to the friend's FriendChat
            // autoAdd=true  -> receiver taps Accept -> instantly joins (no queue)
            // autoAdd=false -> receiver taps Accept -> goes to request queue
            val inviteMsg = mapOf(
                "senderId"       to state.myUsername,
                "senderName"     to state.myUsername,
                "msgType"        to "group_invite",
                "text"           to "Group invite: \${state.groupName}",
                "groupId"        to state.groupId,
                "groupName"      to state.groupName,
                "invitedBy"      to state.myUsername,
                "invitedByRole"  to state.myRole,
                "autoAdd"        to isAdminOrCreator,
                "timestamp"      to ServerValue.TIMESTAMP
            )
            db.getReference("friend_chats").child(chatId).child("messages").push().setValue(inviteMsg)
            // NOTE: We do NOT add to group_members here — we wait for receiver to accept
        }
        _uiState.value = _uiState.value.copy(isSending = false, sendDone = true,
            snackMessage = if (isAdminOrCreator) "Members added!" else "Invites sent!")
    }

    fun clearSnack() { _uiState.value = _uiState.value.copy(snackMessage = "") }
}
