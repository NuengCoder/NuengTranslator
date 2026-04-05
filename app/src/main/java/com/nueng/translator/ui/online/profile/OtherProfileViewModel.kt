package com.nueng.translator.ui.online.profile

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

enum class FriendStatus { LOADING, NONE, SENT, RECEIVED, FRIENDS }

data class OtherProfileUiState(
    val targetUserId: String = "",   // target's username
    val username: String = "",
    val nickname: String = "",
    val bio: String = "",
    val rank: String = "Normal",
    val friendStatus: FriendStatus = FriendStatus.LOADING,
    val isLoading: Boolean = true,
    val actionMessage: String = ""
)

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private val listeners = mutableMapOf<String, Pair<com.google.firebase.database.DatabaseReference, ValueEventListener>>()
    private var myUsername = ""

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            if (userId > 0 && !isGuest) {
                myUsername = userRepository.getUserById(userId)?.username ?: ""
            }
        }
    }

    fun loadProfile(targetUsername: String) {
        clearListeners()
        _uiState.value = OtherProfileUiState(targetUserId = targetUsername, isLoading = true, friendStatus = FriendStatus.LOADING)

        viewModelScope.launch {
            if (myUsername.isEmpty()) {
                val userId  = preferencesManager.loggedInUserId.first()
                val isGuest = preferencesManager.isGuest.first()
                if (userId > 0 && !isGuest) myUsername = userRepository.getUserById(userId)?.username ?: ""
            }

            // Load profile — keyed by username now
            db.getReference("online_profiles").child(targetUsername).get()
                .addOnSuccessListener { snapshot ->
                    val username = snapshot.child("username").getValue(String::class.java) ?: targetUsername
                    val nickname = snapshot.child("nickname").getValue(String::class.java) ?: ""
                    val bio      = snapshot.child("bio").getValue(String::class.java) ?: ""
                    val rank     = snapshot.child("rank").getValue(String::class.java) ?: "Normal"
                    _uiState.value = _uiState.value.copy(
                        username = username, nickname = nickname,
                        bio = bio, rank = rank, isLoading = false
                    )
                }
                .addOnFailureListener { _uiState.value = _uiState.value.copy(isLoading = false) }

            if (myUsername.isNotEmpty() && myUsername != targetUsername) {
                listenFriendStatus(targetUsername)
            } else {
                _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE)
            }
        }
    }

    private fun listenFriendStatus(targetUsername: String) {
        val me = myUsername

        addListener("friends",
            db.getReference("friends").child(me).child(targetUsername),
            object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    if (s.exists()) _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.FRIENDS)
                    else if (_uiState.value.friendStatus == FriendStatus.FRIENDS) recheckStatus(me, targetUsername)
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        addListener("outgoing",
            db.getReference("friend_requests").child(me).child("outgoing").child(targetUsername),
            object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val cur = _uiState.value.friendStatus
                    if (cur == FriendStatus.FRIENDS) return
                    if (s.exists()) _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.SENT)
                    else if (cur == FriendStatus.SENT) recheckStatus(me, targetUsername)
                }
                override fun onCancelled(e: DatabaseError) {}
            })

        addListener("incoming",
            db.getReference("friend_requests").child(me).child("incoming").child(targetUsername),
            object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    val cur = _uiState.value.friendStatus
                    if (cur == FriendStatus.FRIENDS || cur == FriendStatus.SENT) return
                    if (s.exists()) _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.RECEIVED)
                    else if (cur == FriendStatus.RECEIVED || cur == FriendStatus.LOADING) recheckStatus(me, targetUsername)
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    private fun recheckStatus(me: String, target: String) {
        db.getReference("friends").child(me).child(target).get()
            .addOnSuccessListener { s1 ->
                if (s1.exists()) { _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.FRIENDS); return@addOnSuccessListener }
                db.getReference("friend_requests").child(me).child("outgoing").child(target).get()
                    .addOnSuccessListener { s2 ->
                        if (s2.exists()) { _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.SENT); return@addOnSuccessListener }
                        db.getReference("friend_requests").child(me).child("incoming").child(target).get()
                            .addOnSuccessListener { s3 ->
                                _uiState.value = _uiState.value.copy(friendStatus = if (s3.exists()) FriendStatus.RECEIVED else FriendStatus.NONE)
                            }
                            .addOnFailureListener { _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE) }
                    }
                    .addOnFailureListener { _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE) }
            }
            .addOnFailureListener { _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE) }
    }

    private fun addListener(key: String, ref: com.google.firebase.database.DatabaseReference, listener: ValueEventListener) {
        listeners[key]?.let { (r, l) -> r.removeEventListener(l) }
        ref.addValueEventListener(listener)
        listeners[key] = Pair(ref, listener)
    }

    private fun clearListeners() {
        listeners.forEach { (_, pair) -> pair.first.removeEventListener(pair.second) }
        listeners.clear()
    }

    fun sendFriendRequest(targetUsername: String) {
        if (myUsername.isEmpty()) return
        val data = mapOf("from" to myUsername, "to" to targetUsername, "timestamp" to System.currentTimeMillis())
        db.getReference("friend_requests").child(myUsername).child("outgoing").child(targetUsername).setValue(data)
        db.getReference("friend_requests").child(targetUsername).child("incoming").child(myUsername).setValue(data)
        _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.SENT, actionMessage = "Friend request sent!")
    }

    fun cancelFriendRequest(targetUsername: String) {
        if (myUsername.isEmpty()) return
        db.getReference("friend_requests").child(myUsername).child("outgoing").child(targetUsername).removeValue()
        db.getReference("friend_requests").child(targetUsername).child("incoming").child(myUsername).removeValue()
        _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE, actionMessage = "Request cancelled.")
    }

    fun acceptFriendRequest(targetUsername: String) {
        if (myUsername.isEmpty()) return
        val data = mapOf("since" to System.currentTimeMillis())
        db.getReference("friends").child(myUsername).child(targetUsername).setValue(data)
        db.getReference("friends").child(targetUsername).child(myUsername).setValue(data)
        db.getReference("friend_requests").child(myUsername).child("incoming").child(targetUsername).removeValue()
        db.getReference("friend_requests").child(targetUsername).child("outgoing").child(myUsername).removeValue()
        _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.FRIENDS, actionMessage = "You are now friends!")
    }

    fun declineFriendRequest(targetUsername: String) {
        if (myUsername.isEmpty()) return
        db.getReference("friend_requests").child(myUsername).child("incoming").child(targetUsername).removeValue()
        db.getReference("friend_requests").child(targetUsername).child("outgoing").child(myUsername).removeValue()
        _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE, actionMessage = "Request declined.")
    }

    fun unfriend(targetUsername: String) {
        if (myUsername.isEmpty()) return
        db.getReference("friends").child(myUsername).child(targetUsername).removeValue()
        db.getReference("friends").child(targetUsername).child(myUsername).removeValue()
        _uiState.value = _uiState.value.copy(friendStatus = FriendStatus.NONE, actionMessage = "Unfriended.")
    }

    fun clearActionMessage() { _uiState.value = _uiState.value.copy(actionMessage = "") }

    override fun onCleared() { super.onCleared(); clearListeners() }
}
