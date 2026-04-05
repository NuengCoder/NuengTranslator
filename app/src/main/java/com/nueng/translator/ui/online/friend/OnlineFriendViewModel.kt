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

data class FriendItem(
    val userId: String,
    val displayName: String,
    val avatarLetter: Char,
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val chatId: String = "",
    val unreadCount: Int = 0
)

data class GroupItem(
    val groupId: String,
    val groupName: String,
    val avatarLetter: Char,
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val memberCount: Int = 0,
    val unreadCount: Int = 0
)

data class OnlineFriendUiState(
    val myUserId: String = "",
    val myDisplayName: String = "",
    val friends: List<FriendItem> = emptyList(),
    val groups: List<GroupItem> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val pendingRequestCount: Int = 0,
    val globalUnreadCount: Int = 0,
    val friendUnreadTotal: Int = 0
)

@HiltViewModel
class OnlineFriendViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineFriendUiState())
    val uiState: StateFlow<OnlineFriendUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private var friendsListener: ValueEventListener? = null
    private var friendsRef: com.google.firebase.database.DatabaseReference? = null
    private var groupsListener: ValueEventListener? = null
    private var groupsRef: com.google.firebase.database.DatabaseReference? = null
    private var requestsListener: ValueEventListener? = null
    private var requestsRef: com.google.firebase.database.DatabaseReference? = null
    private var globalUnreadListener: ValueEventListener? = null
    private var globalUnreadRef: com.google.firebase.database.DatabaseReference? = null
    // Real-time listeners per chat so badges update without leaving the screen
    private val chatUnreadListeners = mutableMapOf<String, ValueEventListener>()
    private val chatUnreadRefs = mutableMapOf<String, com.google.firebase.database.Query>()

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            if (userId > 0 && !isGuest) {
                val username = userRepository.getUserById(userId)?.username ?: ""
                if (username.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(myUserId = username)
                    listenFriends(username)
                    listenGroups(username)
                    listenPendingRequests(username)
                    listenGlobalUnread(username)
                    // Load display name for self-chat card
                    val db = FirebaseDatabase.getInstance(
                        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
                    )
                    db.getReference("online_profiles").child(username).get()
                        .addOnSuccessListener { snap ->
                            val nick  = snap.child("nickname").getValue(String::class.java) ?: ""
                            val uname = snap.child("username").getValue(String::class.java) ?: username
                            _uiState.value = _uiState.value.copy(myDisplayName = nick.ifBlank { uname })
                        }
                    } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun listenFriends(myUsername: String) {
        friendsRef     = db.getReference("friends").child(myUsername)
        friendsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                _uiState.value = _uiState.value.copy(isConnected = true)
                val friends      = mutableListOf<FriendItem>()
                var needsProfile = 0

                for (child in snapshot.children) {
                    val friendUsername = child.key ?: continue
                    val displayName    = child.child("displayName").getValue(String::class.java)
                    val chatId         = child.child("chatId").getValue(String::class.java)
                        ?: buildChatId(myUsername, friendUsername)
                    val lastMsg  = child.child("lastMsg").getValue(String::class.java) ?: ""
                    val lastTime = child.child("lastMsgTime").getValue(Long::class.java) ?: 0L

                    if (displayName != null) {
                        val letter = displayName.firstOrNull()?.uppercaseChar() ?: '?'
                        friends.add(FriendItem(
                            userId = friendUsername, displayName = displayName, avatarLetter = letter,
                            lastMessage = lastMsg, lastMessageTime = lastTime, chatId = chatId
                        ))
                        // Real-time unread listener for this friend chat
                        val myUname = myUsername
                        val cId = chatId
                        if (cId !in chatUnreadListeners) {
                            val ref = db.getReference("friend_chats").child(cId).child("messages")
                                .orderByChild("timestamp").limitToLast(50)
                            val listener = object : ValueEventListener {
                                override fun onDataChange(msgSnap: DataSnapshot) {
                                    var unread = 0
                                    for (msgChild in msgSnap.children) {
                                        val deleted = msgChild.child("deleted").getValue(Boolean::class.java) ?: false
                                        if (deleted) continue
                                        val sender = msgChild.child("senderId").getValue(String::class.java) ?: ""
                                        if (sender == myUname) continue
                                        if (!msgChild.child("read_by").child(myUname).exists()) unread++
                                    }
                                    val updated = _uiState.value.friends.map { f ->
                                        if (f.chatId == cId) f.copy(unreadCount = unread) else f
                                    }
                                    val total = updated.sumOf { it.unreadCount } + _uiState.value.groups.sumOf { it.unreadCount }
                                    _uiState.value = _uiState.value.copy(friends = updated, friendUnreadTotal = total)
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            }
                            ref.addValueEventListener(listener)
                            chatUnreadListeners[cId] = listener
                            chatUnreadRefs[cId] = ref
                        }
                    } else {
                        needsProfile++
                        fetchAndDenormalizeFriend(myUsername, friendUsername, chatId)
                    }
                }

                if (friends.isNotEmpty() || needsProfile == 0) {
                    _uiState.value = _uiState.value.copy(
                        friends   = friends.sortedByDescending { it.lastMessageTime },
                        isLoading = false
                    )
                }
                if (snapshot.childrenCount == 0L) {
                    _uiState.value = _uiState.value.copy(friends = emptyList(), isLoading = false)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(isConnected = false, isLoading = false)
            }
        }
        friendsRef!!.addValueEventListener(friendsListener!!)
    }

    private fun fetchAndDenormalizeFriend(myUsername: String, friendUsername: String, chatId: String) {
        db.getReference("online_profiles").child(friendUsername).get()
            .addOnSuccessListener { snap ->
                val nick  = snap.child("nickname").getValue(String::class.java) ?: ""
                val uname = snap.child("username").getValue(String::class.java) ?: friendUsername
                val name  = nick.ifBlank { uname }
                db.getReference("friends").child(myUsername).child(friendUsername)
                    .updateChildren(mapOf("displayName" to name, "chatId" to chatId))
                val letter  = name.firstOrNull()?.uppercaseChar() ?: '?'
                val current = _uiState.value.friends.toMutableList()
                if (current.none { it.userId == friendUsername }) {
                    current.add(FriendItem(
                        userId = friendUsername, displayName = name, avatarLetter = letter,
                        lastMessage = "", lastMessageTime = 0L, chatId = chatId
                    ))
                    _uiState.value = _uiState.value.copy(
                        friends   = current.sortedByDescending { it.lastMessageTime },
                        isLoading = false
                    )
                }
            }
    }

    private fun listenGroups(myUsername: String) {
        groupsRef     = db.getReference("user_groups").child(myUsername)
        groupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groups = mutableListOf<GroupItem>()
                for (child in snapshot.children) {
                    val groupId   = child.key ?: continue
                    val groupName = child.child("group_name").getValue(String::class.java)
                    val lastMsg   = child.child("lastMsg").getValue(String::class.java) ?: ""
                    val lastTime  = child.child("lastMsgTime").getValue(Long::class.java) ?: 0L
                    val count     = child.child("memberCount").getValue(Int::class.java) ?: 0
                    if (groupName != null) {
                        val letter = groupName.firstOrNull()?.uppercaseChar() ?: 'G'
                        // avatarUrl lives in group_chats, fetch it
                        groups.add(GroupItem(
                            groupId = groupId, groupName = groupName, avatarLetter = letter,
                            lastMessage = lastMsg, lastMessageTime = lastTime, memberCount = count
                        ))
                        db.getReference("group_chats").child(groupId).child("avatarUrl").get()
                            .addOnSuccessListener { avatarSnap ->
                                val url = avatarSnap.getValue(String::class.java) ?: ""
                                if (url.isNotBlank()) {
                                    val updated = _uiState.value.groups.map {
                                        if (it.groupId == groupId) it.copy(avatarUrl = url) else it
                                    }
                                    _uiState.value = _uiState.value.copy(groups = updated)
                                }
                            }
                        if (count == 0) fetchAndDenormalizeGroup(myUsername, groupId)
                        // Real-time unread listener for this group
                        val myUname = myUsername
                        val gId = groupId
                        if (gId !in chatUnreadListeners) {
                            val ref = db.getReference("group_chats").child(gId).child("messages")
                                .orderByChild("timestamp").limitToLast(50)
                            val listener = object : ValueEventListener {
                                override fun onDataChange(msgSnap: DataSnapshot) {
                                    var unread = 0
                                    for (msgChild in msgSnap.children) {
                                        val deleted = msgChild.child("deleted").getValue(Boolean::class.java) ?: false
                                        if (deleted) continue
                                        val sender = msgChild.child("senderId").getValue(String::class.java) ?: ""
                                        if (sender == myUname) continue
                                        if (!msgChild.child("read_by").child(myUname).exists()) unread++
                                    }
                                    val updated = _uiState.value.groups.map { g ->
                                        if (g.groupId == gId) g.copy(unreadCount = unread) else g
                                    }
                                    val total = _uiState.value.friends.sumOf { it.unreadCount } + updated.sumOf { it.unreadCount }
                                    _uiState.value = _uiState.value.copy(groups = updated, friendUnreadTotal = total)
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            }
                            ref.addValueEventListener(listener)
                            chatUnreadListeners[gId] = listener
                            chatUnreadRefs[gId] = ref
                        }
                    } else {
                        fetchAndDenormalizeGroup(myUsername, groupId)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    groups    = groups.sortedByDescending { it.lastMessageTime },
                    isLoading = false
                )
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
        groupsRef!!.addValueEventListener(groupsListener!!)
    }

    private fun fetchAndDenormalizeGroup(myUsername: String, groupId: String) {
        db.getReference("group_chats").child(groupId).get()
            .addOnSuccessListener { snap ->
                val name     = snap.child("name").getValue(String::class.java) ?: "Group"
                val count    = snap.child("member_count").getValue(Int::class.java) ?: 0
                val lastMsg  = snap.child("last_message").child("text").getValue(String::class.java) ?: ""
                val lastTime = snap.child("last_message").child("timestamp").getValue(Long::class.java) ?: 0L
                db.getReference("user_groups").child(myUsername).child(groupId)
                    .updateChildren(mapOf(
                        "group_name"  to name,
                        "memberCount" to count,
                        "lastMsg"     to lastMsg,
                        "lastMsgTime" to lastTime
                    ))
                val letter  = name.firstOrNull()?.uppercaseChar() ?: 'G'
                val current = _uiState.value.groups.toMutableList()
                if (current.none { it.groupId == groupId }) {
                    current.add(GroupItem(
                        groupId = groupId, groupName = name, avatarLetter = letter,
                        lastMessage = lastMsg, lastMessageTime = lastTime, memberCount = count
                    ))
                    _uiState.value = _uiState.value.copy(groups = current)
                }
            }
    }

    private fun listenPendingRequests(myUsername: String) {
        // Requests TO me are stored at friend_requests/{myUsername}/incoming
        requestsRef      = db.getReference("friend_requests").child(myUsername).child("incoming")
        requestsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Each child key is the sender's username — just count them
                val count = snapshot.childrenCount.toInt()
                _uiState.value = _uiState.value.copy(pendingRequestCount = count)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        requestsRef!!.addValueEventListener(requestsListener!!)
    }

    private fun listenGlobalUnread(myUsername: String) {
        globalUnreadRef = db.getReference("global_chat").child("messages")
        globalUnreadListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var count = 0
                for (child in snapshot.children) {
                    val isDeleted = child.child("deleted").getValue(Boolean::class.java) ?: false
                    if (isDeleted) continue
                    val sender = child.child("senderId").getValue(String::class.java) ?: ""
                    if (sender == myUsername) continue
                    if (!child.child("read_by").child(myUsername).exists()) count++
                }
                _uiState.value = _uiState.value.copy(globalUnreadCount = count)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        globalUnreadRef!!.orderByChild("timestamp").limitToLast(100)
            .addValueEventListener(globalUnreadListener!!)
    }

    fun buildChatId(a: String, b: String): String =
        if (a < b) "${a}_${b}" else "${b}_${a}"

    override fun onCleared() {
        super.onCleared()
        friendsListener?.let      { friendsRef?.removeEventListener(it) }
        groupsListener?.let       { groupsRef?.removeEventListener(it) }
        requestsListener?.let     { requestsRef?.removeEventListener(it) }
        globalUnreadListener?.let { globalUnreadRef?.removeEventListener(it) }
        chatUnreadListeners.forEach { (key, listener) ->
            chatUnreadRefs[key]?.removeEventListener(listener)
        }
        
        chatUnreadListeners.clear()
        chatUnreadRefs.clear()
    }
}
