package com.nueng.translator.ui.online.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupMemberItem(
    val userId: String,
    val displayName: String,
    val avatarLetter: Char,
    val role: String = "member"
)

data class GroupInfoUiState(
    val groupId: String = "",
    val groupName: String = "",
    val groupAvatarLetter: Char = 'G',
    val members: List<GroupMemberItem> = emptyList(),
    val myUserId: String = "",
    val myRole: String = "member",
    val isLoading: Boolean = true,
    val actionMessage: String = "",
    val shouldNavigateBack: Boolean = false
)

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupInfoUiState())
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private var myUserId = ""

    fun load(groupId: String) {
        if (_uiState.value.groupId == groupId && !_uiState.value.isLoading) return
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
              myUserId = if (userId > 0 && !isGuest) userRepository.getUserById(userId)?.username ?: "" else ""
            _uiState.value = _uiState.value.copy(
                groupId  = groupId,
                myUserId = myUserId,
                isLoading = true
            )

            // Load group name
            db.getReference("group_chats").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val name   = snap.child("name").getValue(String::class.java) ?: "Group"
                    val letter = if (name.isNotEmpty()) name.first().uppercaseChar() else 'G'
                    _uiState.value = _uiState.value.copy(groupName = name, groupAvatarLetter = letter)
                }

            // Load members + roles
            db.getReference("group_members").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val memberIds = snap.children.map { it.key ?: "" }.filter { it.isNotEmpty() }
                    val roleMap   = memberIds.associateWith { id ->
                        snap.child(id).child("role").getValue(String::class.java) ?: "member"
                    }
                    val myRole = roleMap[myUserId] ?: "member"
                    _uiState.value = _uiState.value.copy(myRole = myRole)
                    loadMemberProfiles(memberIds, roleMap)
                }
                .addOnFailureListener {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        }
    }

    private fun loadMemberProfiles(memberIds: List<String>, roleMap: Map<String, String>) {
        if (memberIds.isEmpty()) { _uiState.value = _uiState.value.copy(isLoading = false); return }
        val result = mutableListOf<GroupMemberItem>()
        var loaded = 0
        for (memberId in memberIds) {
            db.getReference("online_profiles").child(memberId).get()
                .addOnSuccessListener { snap ->
                    val nick   = snap.child("nickname").getValue(String::class.java) ?: ""
                    val uname  = snap.child("username").getValue(String::class.java) ?: memberId
                    val name   = nick.ifBlank { uname }
                    val letter = if (name.isNotEmpty()) name.first().uppercaseChar() else '?'
                    val role   = roleMap[memberId] ?: "member"
                    result.add(GroupMemberItem(memberId, name, letter, role))
                    loaded++
                    if (loaded == memberIds.size) finalize(result)
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded == memberIds.size) finalize(result)
                }
        }
    }

    private fun roleOrder(role: String) = when (role) { "creator" -> 0; "admin" -> 1; else -> 2 }

    private fun finalize(result: MutableList<GroupMemberItem>) {
        val sorted = result.sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        _uiState.value = _uiState.value.copy(members = sorted, isLoading = false)
    }

    fun grantAdmin(targetUserId: String) {
        val state   = _uiState.value
        val groupId = state.groupId
        db.getReference("group_members").child(groupId).child(targetUserId).child("role").setValue("admin")
        db.getReference("user_groups").child(targetUserId).child(groupId).child("role").setValue("admin")
        val name    = state.members.find { it.userId == targetUserId }?.displayName ?: ""
        val updated = state.members.map { if (it.userId == targetUserId) it.copy(role = "admin") else it }
            .sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        _uiState.value = _uiState.value.copy(members = updated, actionMessage = "$name is now an admin!")
    }

    fun revokeAdmin(targetUserId: String) {
        val state   = _uiState.value
        val groupId = state.groupId
        db.getReference("group_members").child(groupId).child(targetUserId).child("role").setValue("member")
        db.getReference("user_groups").child(targetUserId).child(groupId).child("role").setValue("member")
        val name    = state.members.find { it.userId == targetUserId }?.displayName ?: ""
        val updated = state.members.map { if (it.userId == targetUserId) it.copy(role = "member") else it }
            .sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        _uiState.value = _uiState.value.copy(members = updated, actionMessage = "$name is now a member.")
    }

    fun kickMember(targetUserId: String) {
        val state    = _uiState.value
        val groupId  = state.groupId
        val name     = state.members.find { it.userId == targetUserId }?.displayName ?: ""
        val updated  = state.members.filter { it.userId != targetUserId }
        val newCount = updated.size
        db.getReference("group_members").child(groupId).child(targetUserId).removeValue()
        db.getReference("user_groups").child(targetUserId).child(groupId).removeValue()
        db.getReference("group_chats").child(groupId).child("member_count").setValue(newCount)
        _uiState.value = _uiState.value.copy(members = updated, actionMessage = "$name was removed.")
        if (newCount == 0) deleteGroup(groupId)
    }

    fun leaveGroup() {
        if (myUserId.isEmpty()) return
        val state    = _uiState.value
        val groupId  = state.groupId
        val remaining = state.members.filter { it.userId != myUserId }
        val newCount  = remaining.size

        if (newCount == 0) {
            // Last member — delete group
            db.getReference("group_members").child(groupId).child(myUserId).removeValue()
            db.getReference("user_groups").child(myUserId).child(groupId).removeValue()
            deleteGroup(groupId)
            _uiState.value = _uiState.value.copy(actionMessage = "Group deleted.", shouldNavigateBack = true)
            return
        }

        // If leaving as creator — transfer ownership
        if (state.myRole == "creator") {
            val newOwner = remaining.firstOrNull { it.role == "admin" } ?: remaining.random()
            db.getReference("group_members").child(groupId).child(newOwner.userId)
                .child("role").setValue("creator")
            db.getReference("user_groups").child(newOwner.userId).child(groupId)
                .child("role").setValue("creator")
        }

        db.getReference("group_members").child(groupId).child(myUserId).removeValue()
        db.getReference("user_groups").child(myUserId).child(groupId).removeValue()
        db.getReference("group_chats").child(groupId).child("member_count").setValue(newCount)
        // Update memberCount in user_groups for all remaining members
        db.getReference("group_members").child(groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    db.getReference("user_groups").child(uid).child(groupId)
                        .child("memberCount").setValue(newCount)
                }
            }
        _uiState.value = _uiState.value.copy(actionMessage = "You left the group.", shouldNavigateBack = true)
    }

    fun wipeChat() {
        if (_uiState.value.myRole != "creator") return
        val groupId = _uiState.value.groupId
        db.getReference("group_chats").child(groupId).child("messages").removeValue()
        _uiState.value = _uiState.value.copy(actionMessage = "Chat wiped.")
    }

    private fun deleteGroup(groupId: String) {
        // Remove all remaining member entries
        db.getReference("group_members").child(groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    db.getReference("user_groups").child(uid).child(groupId).removeValue()
                }
                db.getReference("group_members").child(groupId).removeValue()
                db.getReference("group_chats").child(groupId).removeValue()
            }
    }

    fun sendInvite(inviteeUsername: String) {
        val state = _uiState.value
        if (inviteeUsername.isBlank() || state.groupId.isEmpty()) return
        // Check not already a member
        if (state.members.any { it.userId == inviteeUsername }) {
            _uiState.value = _uiState.value.copy(actionMessage = "$inviteeUsername is already in this group.")
            return
        }
        val invite = mapOf(
            "from"      to state.myUserId,
            "groupName" to state.groupName,
            "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP
        )
        db.getReference("group_invites").child(state.groupId).child(inviteeUsername)
            .setValue(invite)
        _uiState.value = _uiState.value.copy(actionMessage = "Invite sent to $inviteeUsername!")
    }

    fun clearActionMessage() { _uiState.value = _uiState.value.copy(actionMessage = "") }
    fun clearNavigateBack()  { _uiState.value = _uiState.value.copy(shouldNavigateBack = false) }
}
