package com.nueng.translator.ui.online.friend

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

data class FriendInfoUiState(
    val friendUserId: String = "",   // friend's username
    val username: String = "",
    val nickname: String = "",
    val bio: String = "",
    val rank: String = "Normal",
    val isBlocked: Boolean = false,       // I blocked them
    val isBlockedByThem: Boolean = false, // They blocked me
    val isLoading: Boolean = true,
    val actionMessage: String = "",
    val shouldNavigateBack: Boolean = false
)

@HiltViewModel
class FriendInfoViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendInfoUiState())
    val uiState: StateFlow<FriendInfoUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private var myUsername    = ""
    private var myDisplayName = ""

    fun load(friendUsername: String) {
        if (_uiState.value.friendUserId == friendUsername && !_uiState.value.isLoading) return
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            if (userId > 0 && !isGuest) {
                myUsername = userRepository.getUserById(userId)?.username ?: ""
            }

            _uiState.value = _uiState.value.copy(friendUserId = friendUsername, isLoading = true)

            // Load my display name
            if (myUsername.isNotEmpty()) {
                db.getReference("online_profiles").child(myUsername).get()
                    .addOnSuccessListener { snap ->
                        val nick  = snap.child("nickname").getValue(String::class.java) ?: ""
                        val uname = snap.child("username").getValue(String::class.java) ?: myUsername
                        myDisplayName = nick.ifBlank { uname }
                    }
            }

            // Load friend profile — keyed by username
            db.getReference("online_profiles").child(friendUsername).get()
                .addOnSuccessListener { snap ->
                    val username = snap.child("username").getValue(String::class.java) ?: friendUsername
                    val nickname = snap.child("nickname").getValue(String::class.java) ?: ""
                    val bio      = snap.child("bio").getValue(String::class.java) ?: ""
                    val rank     = snap.child("rank").getValue(String::class.java) ?: "Normal"
                    _uiState.value = _uiState.value.copy(
                        username = username, nickname = nickname,
                        bio = bio, rank = rank, isLoading = false
                    )
                }
                .addOnFailureListener { _uiState.value = _uiState.value.copy(isLoading = false) }

            // Check if I blocked them
            if (myUsername.isNotEmpty()) {
                db.getReference("block_list").child(myUsername).child(friendUsername).get()
                    .addOnSuccessListener { snap ->
                        _uiState.value = _uiState.value.copy(isBlocked = snap.exists())
                    }
            }

            // Check if they blocked me — check MY side of block_list for "blockerName"
            // When A blocks B: writes block_list/B/A with {blockerName: "A_display"}
            // So B checks block_list/B/A (= block_list/{myUsername}/{friendUsername})
            if (myUsername.isNotEmpty()) {
                db.getReference("block_list").child(myUsername).child(friendUsername).get()
                    .addOnSuccessListener { snap ->
                        // "blockerName" field = this entry was written by THEM (they blocked me)
                        // "username" field    = this entry was written by ME (I blocked them)
                        val theyBlockedMe = snap.exists() && snap.hasChild("blockerName")
                        _uiState.value = _uiState.value.copy(isBlockedByThem = theyBlockedMe)
                    }
            }
        }
    }

    fun blockUser() {
        if (myUsername.isEmpty()) return
        val friendUsername = _uiState.value.friendUserId
        val state          = _uiState.value
        val displayName    = state.nickname.ifBlank { state.username }
        db.getReference("block_list").child(myUsername).child(friendUsername)
            .setValue(mapOf("username" to displayName, "blockedAt" to System.currentTimeMillis()))
        db.getReference("block_list").child(friendUsername).child(myUsername)
            .setValue(mapOf("blockerName" to myDisplayName, "blockedAt" to System.currentTimeMillis()))
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(isBlocked = true, actionMessage = "$displayName has been blocked.")
            }
    }

    fun unblockUser() {
        if (myUsername.isEmpty()) return
        val friendUsername = _uiState.value.friendUserId
        // Delete BOTH nodes so the other person is fully unblocked:
        //   block_list/{me}/{friend}    — my "I blocked them" record
        //   block_list/{friend}/{me}    — their "blockerName" record written when I blocked them
        db.getReference("block_list").child(myUsername).child(friendUsername).removeValue()
        db.getReference("block_list").child(friendUsername).child(myUsername).removeValue()
            .addOnSuccessListener {
                _uiState.value = _uiState.value.copy(
                    isBlocked       = false,
                    isBlockedByThem = false,
                    actionMessage   = "User unblocked."
                )
            }
    }

    fun unfriend() {
        if (myUsername.isEmpty()) return
        val friendUsername = _uiState.value.friendUserId
        db.getReference("friends").child(myUsername).child(friendUsername).removeValue()
        db.getReference("friends").child(friendUsername).child(myUsername).removeValue()
        _uiState.value = _uiState.value.copy(actionMessage = "Unfriended.", shouldNavigateBack = true)
    }

    fun clearActionMessage()  { _uiState.value = _uiState.value.copy(actionMessage = "") }
    fun clearNavigateBack()   { _uiState.value = _uiState.value.copy(shouldNavigateBack = false) }
}
