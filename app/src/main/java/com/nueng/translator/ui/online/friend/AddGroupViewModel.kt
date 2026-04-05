package com.nueng.translator.ui.online.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectableFriend(
    val userId: String,
    val displayName: String,
    val avatarLetter: Char,
    val isSelected: Boolean = false
)

data class AddGroupUiState(
    val groupName: String = "",
    val friends: List<SelectableFriend> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String = "",
    val createdGroupId: String? = null
)

@HiltViewModel
class AddGroupViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddGroupUiState())
    val uiState: StateFlow<AddGroupUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private var myUserId = ""

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            // KEY FIX: use username as identity, not local Room int
            myUserId = if (userId > 0 && !isGuest)
                userRepository.getUserById(userId)?.username ?: ""
            else ""
            if (myUserId.isNotEmpty()) loadFriends()
            else _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadFriends() {
        db.getReference("friends").child(myUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Skip self-chat entry if present in friends node
                    val ids = snapshot.children
                        .map { it.key ?: "" }
                        .filter { it.isNotEmpty() && it != myUserId }
                    if (ids.isEmpty()) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        return
                    }
                    val result = mutableListOf<SelectableFriend>()
                    var loaded = 0
                    for (friendId in ids) {
                        db.getReference("online_profiles").child(friendId).get()
                            .addOnSuccessListener { snap ->
                                val nick   = snap.child("nickname").getValue(String::class.java) ?: ""
                                val uname  = snap.child("username").getValue(String::class.java) ?: friendId
                                val name   = nick.ifBlank { uname }
                                val letter = if (name.isNotEmpty()) name.first().uppercaseChar() else '?'
                                result.add(SelectableFriend(friendId, name, letter))
                                loaded++
                                if (loaded == ids.size) {
                                    _uiState.value = _uiState.value.copy(
                                        friends   = result.sortedBy { it.displayName },
                                        isLoading = false
                                    )
                                }
                            }
                            .addOnFailureListener {
                                loaded++
                                if (loaded == ids.size) {
                                    _uiState.value = _uiState.value.copy(
                                        friends   = result.sortedBy { it.displayName },
                                        isLoading = false
                                    )
                                }
                            }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            })
    }

    fun onGroupNameChange(name: String) {
        _uiState.value = _uiState.value.copy(groupName = name, errorMessage = "")
    }

    fun toggleFriend(userId: String) {
        val updated = _uiState.value.friends.map { f ->
            if (f.userId == userId) f.copy(isSelected = !f.isSelected) else f
        }
        _uiState.value = _uiState.value.copy(friends = updated)
    }

    fun createGroup() {
        val state = _uiState.value
        val name  = state.groupName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a group name")
            return
        }
        val selectedFriends = state.friends.filter { it.isSelected }
        if (selectedFriends.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Select at least one friend")
            return
        }
        if (myUserId.isEmpty()) return

        _uiState.value = _uiState.value.copy(isCreating = true, errorMessage = "")

        val allMembers  = selectedFriends.map { it.userId } + myUserId
        val memberCount = allMembers.size

        val groupRef = db.getReference("group_chats").push()
        val groupId  = groupRef.key ?: return

        // 1. Write group info under group_chats/{groupId}
        groupRef.setValue(mapOf(
            "name"         to name,
            "created_by"   to myUserId,
            "created_at"   to System.currentTimeMillis(),
            "member_count" to memberCount,
            "last_message" to mapOf("text" to "", "timestamp" to 0L, "senderId" to "")
        ))

        // 2. Write group roster: group_members/{groupId}/{userId} = {role, joined_at}
        val rosterRef = db.getReference("group_members").child(groupId)
        for (memberId in allMembers) {
            val role = if (memberId == myUserId) "creator" else "member"
            rosterRef.child(memberId).setValue(
                mapOf("role" to role, "joined_at" to System.currentTimeMillis())
            )
        }

        // 3. Write reverse index: user_groups/{userId}/{groupId} = {role, joined_at}
        for (memberId in allMembers) {
            val role = if (memberId == myUserId) "creator" else "member"
            db.getReference("user_groups").child(memberId).child(groupId).setValue(
                mapOf(
                    "role"        to role,
                    "group_name"  to name,
                    "joined_at"   to System.currentTimeMillis(),
                    "memberCount" to memberCount
                )
            )
        }

        _uiState.value = _uiState.value.copy(isCreating = false, createdGroupId = groupId)
    }

    fun onNavigatedToGroup() {
        _uiState.value = _uiState.value.copy(createdGroupId = null)
    }
}
