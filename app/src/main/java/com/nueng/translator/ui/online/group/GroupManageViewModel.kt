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

data class ManageMemberItem(
    val username: String = "",
    val displayName: String = "",
    val avatarLetter: Char = '?',
    val role: String = "member"
)

data class PendingInvite(
    val inviteeUsername: String = "",
    val fromUsername: String = "",
    val timestamp: Long = 0L
)

data class GroupRequest(
    val username: String = "",
    val displayName: String = "",
    val avatarLetter: Char = '?',
    val invitedBy: String = "",
    val invitedByRole: String = "member",
    val timestamp: Long = 0L
)

data class GroupManageUiState(
    val groupId: String = "",
    val groupName: String = "",
    val myUsername: String = "",
    val myRole: String = "member",
    val members: List<ManageMemberItem> = emptyList(),
    val filteredMembers: List<ManageMemberItem> = emptyList(),
    val pendingInvites: List<PendingInvite> = emptyList(),
    val requests: List<GroupRequest> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val membersExpanded: Boolean = true,
    val requestsExpanded: Boolean = true,
    val autoAccept: Boolean = false,
    val snackMessage: String = "",
    val shouldNavigateBack: Boolean = false,
    val groupAvatarUrl: String = ""
)

@HiltViewModel
class GroupManageViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupManageUiState())
    val uiState: StateFlow<GroupManageUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    fun load(groupId: String) {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val myUsername = if (userId > 0 && !isGuest)
                userRepository.getUserById(userId)?.username ?: "" else ""

            _uiState.value = _uiState.value.copy(
                groupId = groupId, myUsername = myUsername, isLoading = true
            )

            // Load group name
            db.getReference("group_chats").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val name      = snap.child("name").getValue(String::class.java) ?: "Group"
                    val avatarUrl = snap.child("avatarUrl").getValue(String::class.java) ?: ""
                    _uiState.value = _uiState.value.copy(groupName = name, groupAvatarUrl = avatarUrl)
                }

            // Load auto-accept setting
            db.getReference("group_settings").child(groupId).child("autoAccept").get()
                .addOnSuccessListener { snap ->
                    val aa = snap.getValue(Boolean::class.java) ?: false
                    _uiState.value = _uiState.value.copy(autoAccept = aa)
                }

            // Load join requests
            db.getReference("group_requests").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val reqs = mutableListOf<GroupRequest>()
                    var loaded = 0
                    val children = snap.children.toList()
                    if (children.isEmpty()) {
                        _uiState.value = _uiState.value.copy(requests = emptyList())
                        return@addOnSuccessListener
                    }
                    for (child in children) {
                        val uname      = child.key ?: continue
                        val invitedBy  = child.child("from").getValue(String::class.java) ?: ""
                        val invByRole  = child.child("fromRole").getValue(String::class.java) ?: "member"
                        val ts         = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        // Get display name from online_profiles
                        db.getReference("online_profiles").child(uname).get()
                            .addOnSuccessListener { pSnap ->
                                val nick    = pSnap.child("nickname").getValue(String::class.java) ?: ""
                                val display = nick.ifBlank { uname }
                                val letter  = display.firstOrNull()?.uppercaseChar() ?: '?'
                                reqs.add(GroupRequest(uname, display, letter, invitedBy, invByRole, ts))
                                loaded++
                                if (loaded == children.size)
                                    _uiState.value = _uiState.value.copy(requests = reqs.sortedBy { it.timestamp })
                            }
                            .addOnFailureListener {
                                reqs.add(GroupRequest(uname, uname, uname.firstOrNull()?.uppercaseChar() ?: '?', invitedBy, invByRole, ts))
                                loaded++
                                if (loaded == children.size)
                                    _uiState.value = _uiState.value.copy(requests = reqs.sortedBy { it.timestamp })
                            }
                    }
                }

            // Load pending invites
            db.getReference("group_invites").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val invites = snap.children.mapNotNull { child ->
                        val invitee = child.key ?: return@mapNotNull null
                        val from    = child.child("from").getValue(String::class.java) ?: ""
                        val ts      = child.child("timestamp").getValue(Long::class.java) ?: 0L
                        PendingInvite(invitee, from, ts)
                    }
                    _uiState.value = _uiState.value.copy(pendingInvites = invites)
                }

            // Load members
            db.getReference("group_members").child(groupId).get()
                .addOnSuccessListener { snap ->
                    val usernames = snap.children.mapNotNull { it.key }
                    val roleMap   = usernames.associateWith { u ->
                        snap.child(u).child("role").getValue(String::class.java) ?: "member"
                    }
                    val myRole = roleMap[myUsername] ?: "member"
                    _uiState.value = _uiState.value.copy(myRole = myRole)
                    loadProfiles(usernames, roleMap)
                }
        }
    }

    private fun loadProfiles(usernames: List<String>, roleMap: Map<String, String>) {
        if (usernames.isEmpty()) { _uiState.value = _uiState.value.copy(isLoading = false); return }
        val result = mutableListOf<ManageMemberItem>()
        var loaded = 0
        for (u in usernames) {
            db.getReference("online_profiles").child(u).get()
                .addOnSuccessListener { snap ->
                    val nick    = snap.child("nickname").getValue(String::class.java) ?: ""
                    val display = nick.ifBlank { u }
                    val letter  = display.firstOrNull()?.uppercaseChar() ?: '?'
                    result.add(ManageMemberItem(u, display, letter, roleMap[u] ?: "member"))
                    loaded++
                    if (loaded == usernames.size) finalizeMembers(result)
                }
                .addOnFailureListener {
                    result.add(ManageMemberItem(u, u, u.firstOrNull()?.uppercaseChar() ?: '?', roleMap[u] ?: "member"))
                    loaded++
                    if (loaded == usernames.size) finalizeMembers(result)
                }
        }
    }

    private fun roleOrder(r: String) = when(r) { "creator"->0; "admin"->1; else->2 }

    private fun finalizeMembers(result: List<ManageMemberItem>) {
        val sorted = result.sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        _uiState.value = _uiState.value.copy(
            members = sorted, filteredMembers = sorted, isLoading = false
        )
    }

    fun onSearch(query: String) {
        val filtered = if (query.isBlank()) _uiState.value.members
        else _uiState.value.members.filter {
            it.displayName.contains(query, ignoreCase = true) ||
            it.username.contains(query, ignoreCase = true)
        }
        _uiState.value = _uiState.value.copy(searchQuery = query, filteredMembers = filtered)
    }

    fun promote(target: ManageMemberItem) {
        val state   = _uiState.value
        val newRole = if (target.role == "member") "admin" else "admin"
        db.getReference("group_members").child(state.groupId).child(target.username).child("role").setValue(newRole)
        db.getReference("user_groups").child(target.username).child(state.groupId).child("role").setValue(newRole)
        updateMember(target.username, newRole)
        _uiState.value = _uiState.value.copy(snackMessage = "${target.displayName} is now Admin!")
    }

    fun demote(target: ManageMemberItem) {
        val state = _uiState.value
        db.getReference("group_members").child(state.groupId).child(target.username).child("role").setValue("member")
        db.getReference("user_groups").child(target.username).child(state.groupId).child("role").setValue("member")
        updateMember(target.username, "member")
        _uiState.value = _uiState.value.copy(snackMessage = "${target.displayName} demoted to Member.")
    }

    fun kick(target: ManageMemberItem) {
        val state    = _uiState.value
        val updated  = state.members.filter { it.username != target.username }
        val newCount = updated.size
        db.getReference("group_members").child(state.groupId).child(target.username).removeValue()
        db.getReference("user_groups").child(target.username).child(state.groupId).removeValue()
        db.getReference("group_chats").child(state.groupId).child("member_count").setValue(newCount)
        val filtered = _uiState.value.filteredMembers.filter { it.username != target.username }
        _uiState.value = _uiState.value.copy(
            members = updated, filteredMembers = filtered,
            snackMessage = "${target.displayName} removed."
        )
        if (newCount == 0) deleteGroup(state.groupId)
    }

    fun wipeChat() {
        val state = _uiState.value
        if (state.myRole != "creator") return
        db.getReference("group_chats").child(state.groupId).child("messages").removeValue()
        _uiState.value = _uiState.value.copy(snackMessage = "Chat wiped!")
    }

    private fun updateMember(username: String, newRole: String) {
        val updated = _uiState.value.members.map {
            if (it.username == username) it.copy(role = newRole) else it
        }.sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        val filtered = _uiState.value.filteredMembers.map {
            if (it.username == username) it.copy(role = newRole) else it
        }.sortedWith(compareBy({ roleOrder(it.role) }, { it.displayName }))
        _uiState.value = _uiState.value.copy(members = updated, filteredMembers = filtered)
    }

    private fun deleteGroup(groupId: String) {
        db.getReference("group_members").child(groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    db.getReference("user_groups").child(uid).child(groupId).removeValue()
                }
                db.getReference("group_members").child(groupId).removeValue()
                db.getReference("group_chats").child(groupId).removeValue()
            }
        _uiState.value = _uiState.value.copy(shouldNavigateBack = true)
    }

    private fun updateMemberCountForAll(groupId: String, newCount: Int) {
        // Update group_chats member_count
        db.getReference("group_chats").child(groupId).child("member_count").setValue(newCount)
        // Update user_groups memberCount for all current members
        db.getReference("group_members").child(groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    db.getReference("user_groups").child(uid).child(groupId)
                        .child("memberCount").setValue(newCount)
                }
            }
    }

    fun acceptRequest(req: GroupRequest) {
        val state    = _uiState.value
        val groupId  = state.groupId
        val newCount = state.members.size + 1
        val memberData = mapOf("role" to "member", "joinedAt" to com.google.firebase.database.ServerValue.TIMESTAMP)
        db.getReference("group_members").child(groupId).child(req.username).setValue(memberData)
        db.getReference("user_groups").child(req.username).child(groupId).setValue(
            mapOf("group_name" to state.groupName, "role" to "member", "memberCount" to newCount))
        db.getReference("group_requests").child(groupId).child(req.username).removeValue()
        updateMemberCountForAll(groupId, newCount)
        // Notify user
        db.getReference("user_invites").child(req.username).push().setValue(mapOf(
            "groupId"    to groupId,
            "groupName"  to state.groupName,
            "from"       to state.myUsername,
            "fromRole"   to state.myRole,
            "accepted"   to true,
            "acceptedBy" to state.myUsername,
            "timestamp"  to com.google.firebase.database.ServerValue.TIMESTAMP
        ))
        val updated = state.requests.filter { it.username != req.username }
        _uiState.value = _uiState.value.copy(requests = updated, snackMessage = "${req.displayName} joined!")
        load(groupId)
    }

    fun rejectRequest(req: GroupRequest) {
        val state = _uiState.value
        db.getReference("group_requests").child(state.groupId).child(req.username).removeValue()
        val updated = state.requests.filter { it.username != req.username }
        _uiState.value = _uiState.value.copy(requests = updated, snackMessage = "Request rejected.")
    }

    fun toggleAutoAccept() {
        val state  = _uiState.value
        val newVal = !state.autoAccept
        db.getReference("group_settings").child(state.groupId).child("autoAccept").setValue(newVal)
        _uiState.value = _uiState.value.copy(autoAccept = newVal)
    }

    fun toggleRequestsExpanded() {
        _uiState.value = _uiState.value.copy(requestsExpanded = !_uiState.value.requestsExpanded)
    }

    fun acceptInvite(invite: PendingInvite) {
        val state   = _uiState.value
        val groupId = state.groupId
        // Add to group
        val memberData = mapOf("role" to "member", "joinedAt" to com.google.firebase.database.ServerValue.TIMESTAMP)
        db.getReference("group_members").child(groupId).child(invite.inviteeUsername).setValue(memberData)
        db.getReference("user_groups").child(invite.inviteeUsername).child(groupId).setValue(
            mapOf("group_name" to state.groupName, "role" to "member")
        )
        // Remove invite
        db.getReference("group_invites").child(groupId).child(invite.inviteeUsername).removeValue()
        val updated = state.pendingInvites.filter { it.inviteeUsername != invite.inviteeUsername }
        _uiState.value = _uiState.value.copy(
            pendingInvites = updated,
            snackMessage   = "${invite.inviteeUsername} joined the group!"
        )
        // Reload members
        load(groupId)
    }

    fun rejectInvite(invite: PendingInvite) {
        val state = _uiState.value
        db.getReference("group_invites").child(state.groupId).child(invite.inviteeUsername).removeValue()
        val updated = state.pendingInvites.filter { it.inviteeUsername != invite.inviteeUsername }
        _uiState.value = _uiState.value.copy(
            pendingInvites = updated,
            snackMessage   = "Invite rejected."
        )
    }

    fun renameGroup(newName: String) {
        val state = _uiState.value
        if (newName.isBlank() || state.myRole !in listOf("creator","admin")) return
        db.getReference("group_chats").child(state.groupId).child("name").setValue(newName.trim())
        db.getReference("group_members").child(state.groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    db.getReference("user_groups").child(uid).child(state.groupId)
                        .child("group_name").setValue(newName.trim())
                }
            }
        _uiState.value = _uiState.value.copy(groupName = newName.trim(), snackMessage = "Group name updated!")
    }

    fun toggleMembersExpanded() {
        _uiState.value = _uiState.value.copy(membersExpanded = !_uiState.value.membersExpanded)
    }

    fun uploadGroupAvatar(base64Jpeg: String) {
        val state = _uiState.value
        if (state.myRole !in listOf("creator","admin")) return
        viewModelScope.launch {
            val imgUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val apiKey   = com.nueng.translator.BuildConfig.IMGBB_API_KEY
                    val url      = java.net.URL("https://api.imgbb.com/1/upload")
                    val boundary = "----FormBoundary" + System.currentTimeMillis().toString()
                    val conn     = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput     = true
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
                    conn.connectTimeout = 30000
                    conn.readTimeout    = 30000
                    val os = conn.outputStream
                    val nl = "\r\n".toByteArray(Charsets.UTF_8)
                    val dd = "--".toByteArray(Charsets.UTF_8)
                    val b  = boundary.toByteArray(Charsets.UTF_8)
                    os.write(dd); os.write(b); os.write(nl)
                    os.write("Content-Disposition: form-data; name=\"key\"".toByteArray(Charsets.UTF_8))
                    os.write(nl); os.write(nl)
                    os.write(apiKey.toByteArray(Charsets.UTF_8)); os.write(nl)
                    os.write(dd); os.write(b); os.write(nl)
                    os.write("Content-Disposition: form-data; name=\"image\"".toByteArray(Charsets.UTF_8))
                    os.write(nl); os.write(nl)
                    os.write(base64Jpeg.toByteArray(Charsets.UTF_8)); os.write(nl)
                    os.write(dd); os.write(b); os.write(dd); os.write(nl)
                    os.flush(); os.close()
                    val resp      = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val unescaped = resp.replace("\\/", "/")
                    val match     = Regex("\"display_url\":\"([^\"]+)\"").find(unescaped)
                    match?.groupValues?.get(1)
                } catch (_: Exception) { null }
            }
            if (imgUrl != null) {
                db.getReference("group_chats").child(state.groupId).child("avatarUrl").setValue(imgUrl)
                _uiState.value = _uiState.value.copy(groupAvatarUrl = imgUrl, snackMessage = "Group photo updated!")
            } else {
                _uiState.value = _uiState.value.copy(snackMessage = "Upload failed, try again")
            }
        }
    }

    fun clearSnack() { _uiState.value = _uiState.value.copy(snackMessage = "") }
    fun clearNavigateBack() { _uiState.value = _uiState.value.copy(shouldNavigateBack = false) }
}
