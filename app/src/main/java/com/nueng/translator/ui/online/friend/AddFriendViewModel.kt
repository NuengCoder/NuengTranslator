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

data class FriendRequestItem(
    val fromUserId: String,       // username string
    val fromDisplayName: String,
    val fromAvatarLetter: Char,
    val timestamp: Long = 0L
)

data class AddFriendUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val errorMessage: String = "",
    val foundUserId: String? = null,       // username string
    val navigateToProfile: Boolean = false,
    val myUserId: String = "",             // my username
    val incomingRequests: List<FriendRequestItem> = emptyList()
)

@HiltViewModel
class AddFriendViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private var requestsListener: ValueEventListener? = null
    private var requestsRef: com.google.firebase.database.DatabaseReference? = null

    init {
        viewModelScope.launch {
            val username = getMyUsername()
            _uiState.value = _uiState.value.copy(myUserId = username)
            if (username.isNotEmpty()) listenIncomingRequests(username)
        }
    }

    private suspend fun getMyUsername(): String {
        val userId  = preferencesManager.loggedInUserId.first()
        val isGuest = preferencesManager.isGuest.first()
        if (userId <= 0 || isGuest) return ""
        return userRepository.getUserById(userId)?.username ?: ""
    }

    private fun listenIncomingRequests(myUsername: String) {
        requestsRef     = db.getReference("friend_requests").child(myUsername).child("incoming")
        requestsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.map { it.key ?: "" }.filter { it.isNotEmpty() }
                if (ids.isEmpty()) { _uiState.value = _uiState.value.copy(incomingRequests = emptyList()); return }
                loadRequestProfiles(ids, snapshot)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        requestsRef!!.addValueEventListener(requestsListener!!)
    }

    private fun loadRequestProfiles(ids: List<String>, snapshot: DataSnapshot) {
        val result = mutableListOf<FriendRequestItem>()
        var loaded = 0
        for (fromUsername in ids) {
            val timestamp = snapshot.child(fromUsername).child("timestamp").getValue(Long::class.java) ?: 0L
            // online_profiles keyed by username now
            db.getReference("online_profiles").child(fromUsername).get()
                .addOnSuccessListener { profSnap ->
                    val nickname    = profSnap.child("nickname").getValue(String::class.java) ?: ""
                    val username    = profSnap.child("username").getValue(String::class.java) ?: fromUsername
                    val displayName = nickname.ifBlank { username }
                    val letter      = if (displayName.isNotEmpty()) displayName.first().uppercaseChar() else '?'
                    result.add(FriendRequestItem(fromUsername, displayName, letter, timestamp))
                    loaded++
                    if (loaded == ids.size) {
                        _uiState.value = _uiState.value.copy(
                            incomingRequests = result.sortedByDescending { it.timestamp }
                        )
                    }
                }
                .addOnFailureListener {
                    loaded++
                    if (loaded == ids.size) _uiState.value = _uiState.value.copy(incomingRequests = result)
                }
        }
    }

    fun onQueryChange(value: String) {
        _uiState.value = _uiState.value.copy(query = value, errorMessage = "", foundUserId = null)
    }

    // Search by USERNAME (user shares their username as their ID)
    fun searchUser() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) { _uiState.value = _uiState.value.copy(errorMessage = "Please enter a username"); return }
        if (query == _uiState.value.myUserId) { _uiState.value = _uiState.value.copy(errorMessage = "That's your own username!"); return }

        _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = "", foundUserId = null)

        db.getReference("online_profiles").child(query).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    _uiState.value = _uiState.value.copy(isSearching = false, foundUserId = query, navigateToProfile = true)
                } else {
                    _uiState.value = _uiState.value.copy(isSearching = false, errorMessage = "Username not found. Make sure they have opened NuengChat at least once.")
                }
            }
            .addOnFailureListener { e ->
                _uiState.value = _uiState.value.copy(isSearching = false, errorMessage = "Search failed: ${e.message ?: "unknown"}")
            }
    }

    fun acceptRequest(fromUsername: String) {
        val myUsername = _uiState.value.myUserId
        if (myUsername.isBlank()) return
        val data = mapOf("since" to System.currentTimeMillis())
        db.getReference("friends").child(myUsername).child(fromUsername).setValue(data)
        db.getReference("friends").child(fromUsername).child(myUsername).setValue(data)
        db.getReference("friend_requests").child(myUsername).child("incoming").child(fromUsername).removeValue()
        db.getReference("friend_requests").child(fromUsername).child("outgoing").child(myUsername).removeValue()
        val updated = _uiState.value.incomingRequests.filter { it.fromUserId != fromUsername }
        _uiState.value = _uiState.value.copy(incomingRequests = updated)
    }

    fun declineRequest(fromUsername: String) {
        val myUsername = _uiState.value.myUserId
        if (myUsername.isBlank()) return
        db.getReference("friend_requests").child(myUsername).child("incoming").child(fromUsername).removeValue()
        db.getReference("friend_requests").child(fromUsername).child("outgoing").child(myUsername).removeValue()
        val updated = _uiState.value.incomingRequests.filter { it.fromUserId != fromUsername }
        _uiState.value = _uiState.value.copy(incomingRequests = updated)
    }

    fun onNavigatedToProfile() {
        _uiState.value = _uiState.value.copy(navigateToProfile = false, foundUserId = null)
    }

    override fun onCleared() {
        super.onCleared()
        requestsListener?.let { requestsRef?.removeEventListener(it) }
    }
}
