package com.nueng.translator.ui.online.global

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class GlobalMsg(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarLetter: Char = '?',
    val text: String = "",
    val timestamp: Long = 0L,
    val isOwn: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedBy: String = "",
    val isEdited: Boolean = false,
    val isForwarded: Boolean = false,
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val reactions: Map<String, List<String>> = emptyMap()
)

data class GlobalChatUiState(
    val myUserId: String = "",
    val myDisplayName: String = "",
    val isAdmin: Boolean = false,
    val messages: List<GlobalMsg> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val replyingTo: GlobalMsg? = null,
    val highlightedMsgId: String = "",
    val editingMsg: GlobalMsg? = null,
    val isSelectMode: Boolean = false,
    val selectedMsgIds: Set<String> = emptySet(),
    val favEmoji: String = "👍"
)

@HiltViewModel
class GlobalChatViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalChatUiState())
    val uiState: StateFlow<GlobalChatUiState> = _uiState.asStateFlow()

    private val db      = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
    private val chatRef = db.getReference("global_chat")
    private val msgsRef = chatRef.child("messages")
    private val wipeRef = chatRef.child("last_wipe")
    private var msgListener: ValueEventListener? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        const val MAX_MSG_LENGTH = 500
        const val DELETE_DELAY_MS = 3000L
        val QUICK_EMOJIS = listOf("\u2764\uFE0F","\uD83D\uDE02","\uD83D\uDE2E","\uD83D\uDE22","\uD83D\uDE21","\uD83D\uDC4D")
    }

    init {
        viewModelScope.launch {
            val userId  = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user    = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val myId    = user?.username ?: ""
            val isAdmin = user?.role == "admin"
            _uiState.value = _uiState.value.copy(myUserId = myId, isAdmin = isAdmin)
            // Load shared fav emoji from DataStore
            val savedFav = preferencesManager.favEmoji.first()
            _uiState.value = _uiState.value.copy(favEmoji = savedFav)

            if (myId.isNotEmpty()) {
                db.getReference("online_profiles").child(myId).get()
                    .addOnSuccessListener { snap ->
                        val nick  = snap.child("nickname").getValue(String::class.java) ?: ""
                        val uname = snap.child("username").getValue(String::class.java) ?: myId
                        _uiState.value = _uiState.value.copy(myDisplayName = nick.ifBlank { uname })
                    }
            }
            checkDailyWipe(myId)
        }
    }

    private fun checkDailyWipe(myId: String) {
        wipeRef.get()
            .addOnSuccessListener { snap ->
                val lastWipe  = snap.getValue(Long::class.java) ?: 0L
                val now       = System.currentTimeMillis()
                val today7am  = getTodaySevenAm()
                // Only wipe if:
                //   1. Current time is past 7AM today, AND
                //   2. Last wipe was before today's 7AM (hasn't been wiped today yet)
                val shouldWipe = now >= today7am && lastWipe < today7am
                if (shouldWipe) {
                    msgsRef.removeValue().addOnCompleteListener {
                        wipeRef.setValue(now)
                        listenMessages(myId)
                    }
                } else {
                    listenMessages(myId)
                }
            }
            .addOnFailureListener { listenMessages(myId) }
    }

    private fun getTodaySevenAm(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun listenMessages(myId: String) {
        val query = msgsRef.orderByChild("timestamp").limitToLast(100)
        msgListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = mutableListOf<GlobalMsg>()
                for (child in snapshot.children) {
                    val senderId = child.child("senderId").getValue(String::class.java) ?: continue
                    val text     = child.child("text").getValue(String::class.java) ?: continue
                    val ts       = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val sName    = child.child("senderName").getValue(String::class.java) ?: senderId
                    val letter   = sName.firstOrNull()?.uppercaseChar() ?: '?'
                    if (child.child("deleted_for").child(myId).exists()) continue
                    val isDeleted = child.child("deleted").getValue(Boolean::class.java) ?: false
                    val deletedBy = child.child("deletedBy").getValue(String::class.java) ?: ""
                    val isEdited     = child.child("isEdited").getValue(Boolean::class.java) ?: false
                    val isForwarded  = child.child("isForwarded").getValue(Boolean::class.java) ?: false
                    val replyToId     = child.child("replyToId").getValue(String::class.java) ?: ""
                    val replyToText   = child.child("replyToText").getValue(String::class.java) ?: ""
                    val replyToSender = child.child("replyToSender").getValue(String::class.java) ?: ""
                    val reactions = mutableMapOf<String, List<String>>()
                    if (!isDeleted) {
                        for (emojiSnap in child.child("reactions").children) {
                            val emoji = emojiSnap.key ?: continue
                            val names = emojiSnap.children
                                .mapNotNull { it.getValue(String::class.java) }
                                .filter { it.isNotBlank() }
                            if (names.isNotEmpty()) reactions[emoji] = names
                        }
                    }
                    msgs.add(GlobalMsg(
                        id = child.key ?: "", senderId = senderId, senderName = sName,
                        senderAvatarLetter = letter, text = text, timestamp = ts,
                        isOwn = senderId == myId, isDeleted = isDeleted, deletedBy = deletedBy,
                        isEdited = isEdited, isForwarded = isForwarded, replyToId = replyToId,
                        replyToText = replyToText, replyToSender = replyToSender, reactions = reactions
                    ))
                }
                _uiState.value = _uiState.value.copy(
                    messages = msgs.sortedBy { it.timestamp }, isConnected = true, isLoading = false
                )
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(isConnected = false, isLoading = false)
            }
        }
        query.addValueEventListener(msgListener!!)
        _uiState.value = _uiState.value.copy(isConnected = true)
    }

    fun sendMessage(text: String) {
        val state = _uiState.value
        if (text.isBlank() || state.myUserId.isEmpty()) return
        // Edit mode
        val editing = state.editingMsg
        if (editing != null) {
            msgsRef.child(editing.id).updateChildren(mapOf("text" to text.trim().take(MAX_MSG_LENGTH), "isEdited" to true))
            _uiState.value = _uiState.value.copy(editingMsg = null)
            return
        }
        val trimmed = text.trim().take(MAX_MSG_LENGTH)
        val payload = mutableMapOf(
            "senderId"   to state.myUserId,
            "senderName" to state.myDisplayName,
            "text"       to trimmed,
            "timestamp"  to ServerValue.TIMESTAMP
        )
        val reply = state.replyingTo
        if (reply != null) {
            payload["replyToId"]     = reply.id
            payload["replyToText"]   = reply.text.take(100)
            payload["replyToSender"] = reply.senderName
        }
        msgsRef.push().setValue(payload)
        if (reply != null) _uiState.value = _uiState.value.copy(replyingTo = null)
    }

    fun startEdit(msg: GlobalMsg) { _uiState.value = _uiState.value.copy(editingMsg = msg, replyingTo = null) }
    fun cancelEdit()              { _uiState.value = _uiState.value.copy(editingMsg = null) }

    // ── SelectView ────────────────────────────────────────────────────────
    fun enterSelectMode(msg: GlobalMsg) {
        _uiState.value = _uiState.value.copy(
            isSelectMode    = true,
            selectedMsgIds  = setOf(msg.id),
            replyingTo      = null,
            editingMsg      = null
        )
    }

    fun toggleSelect(msg: GlobalMsg) {
        val current = _uiState.value.selectedMsgIds.toMutableSet()
        if (msg.id in current) current.remove(msg.id) else current.add(msg.id)
        _uiState.value = _uiState.value.copy(selectedMsgIds = current)
    }

    fun exitSelectMode() {
        _uiState.value = _uiState.value.copy(isSelectMode = false, selectedMsgIds = emptySet())
    }

    fun getSelectedMessages(): List<GlobalMsg> {
        val ids = _uiState.value.selectedMsgIds
        return _uiState.value.messages.filter { it.id in ids }
    }

    fun deleteSelectedForMe() {
        val myId = _uiState.value.myUserId
        if (myId.isEmpty()) return
        val ids = _uiState.value.selectedMsgIds.toList()
        for (id in ids) {
            msgsRef.child(id).child("deleted_for").child(myId).setValue(true)
        }
        exitSelectMode()
    }

    fun deleteSelectedForEveryone() {
        val state = _uiState.value
        val ids   = state.selectedMsgIds.toList()
        for (id in ids) {
            val msg   = state.messages.find { it.id == id } ?: continue
            val label = if (msg.isOwn) "Deleted by ${state.myDisplayName}" else "Deleted by Admin"
            msgsRef.child(id).updateChildren(mapOf("deleted" to true, "deletedBy" to label, "text" to label))
            msgsRef.child(id).child("reactions").removeValue()
        }
        handler.postDelayed({ for (id in ids) msgsRef.child(id).removeValue() }, DELETE_DELAY_MS)
        exitSelectMode()
    }

    fun canDeleteSelectedForEveryone(): Boolean {
        val state = _uiState.value
        return state.selectedMsgIds.all { id ->
            val msg = state.messages.find { it.id == id }
            msg != null && (msg.isOwn || state.isAdmin)
        }
    }
    fun highlightMsg(id: String) {
        _uiState.value = _uiState.value.copy(highlightedMsgId = id)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2100L)
            _uiState.value = _uiState.value.copy(highlightedMsgId = "")
        }
    }

    fun setReplyTo(msg: GlobalMsg){ _uiState.value = _uiState.value.copy(replyingTo = msg, editingMsg = null) }
    fun setFavEmoji(emoji: String) {
        _uiState.value = _uiState.value.copy(favEmoji = emoji)
        viewModelScope.launch { preferencesManager.setFavEmoji(emoji) }
    }

    fun clearReply()              { _uiState.value = _uiState.value.copy(replyingTo = null) }

    fun deleteForMe(msg: GlobalMsg) {
        val myId = _uiState.value.myUserId
        if (myId.isEmpty()) return
        msgsRef.child(msg.id).child("deleted_for").child(myId).setValue(true)
    }

    fun canDeleteForEveryone(msg: GlobalMsg): Boolean = msg.isOwn || _uiState.value.isAdmin
    fun canEdit(msg: GlobalMsg): Boolean = (msg.isOwn || _uiState.value.isAdmin) && !msg.isDeleted

    fun deleteForEveryone(msg: GlobalMsg) {
        if (!canDeleteForEveryone(msg)) return
        val state = _uiState.value
        val label = if (msg.isOwn) "Deleted by ${state.myDisplayName}" else "Deleted by Admin"
        val ids = mutableListOf(msg.id)
        ids.addAll(state.messages.filter { it.replyToId == msg.id }.map { it.id })
        for (id in ids) {
            msgsRef.child(id).updateChildren(mapOf("deleted" to true, "deletedBy" to label, "text" to label))
            msgsRef.child(id).child("reactions").removeValue()
        }
        handler.postDelayed({ for (id in ids) msgsRef.child(id).removeValue() }, DELETE_DELAY_MS)
        if (state.replyingTo?.id == msg.id) _uiState.value = _uiState.value.copy(replyingTo = null)
        if (state.editingMsg?.id == msg.id) _uiState.value = _uiState.value.copy(editingMsg = null)
    }

    fun toggleReaction(msg: GlobalMsg, emoji: String) {
        val state = _uiState.value
        if (state.myUserId.isEmpty() || msg.isDeleted) return
        val ref = msgsRef.child(msg.id).child("reactions").child(emoji).child(state.myUserId)
        val existing = msg.reactions[emoji]?.contains(state.myDisplayName) == true
        if (existing) ref.removeValue() else ref.setValue(state.myDisplayName)
    }

    fun markAllRead() {
        val state = _uiState.value
        val myId  = state.myUserId
        if (myId.isEmpty()) return
        for (msg in state.messages) {
            if (!msg.isOwn && !msg.isDeleted) {
                msgsRef.child(msg.id).child("read_by").child(myId).setValue(true)
            }
        }
    }

    fun reattach() {
        if (msgListener != null) return
        val myId = _uiState.value.myUserId
        if (myId.isNotEmpty()) listenMessages(myId)
    }

    fun detach() { msgListener?.let { msgsRef.removeEventListener(it) }; msgListener = null }

    override fun onCleared() { super.onCleared(); detach(); handler.removeCallbacksAndMessages(null) }
}
