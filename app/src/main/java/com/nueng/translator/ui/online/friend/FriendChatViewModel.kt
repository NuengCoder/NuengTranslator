package com.nueng.translator.ui.online.friend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.nueng.translator.data.local.PreferencesManager
import com.nueng.translator.data.local.entity.UserDirectory
import com.nueng.translator.data.repository.UserDataRepository
import com.nueng.translator.data.repository.UserDirectoryRepository
import com.nueng.translator.data.repository.UserRepository
import com.nueng.translator.util.NtfExporter
import com.nueng.translator.util.NtfImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMsg(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarLetter: Char = '?',
    val timestamp: Long = 0L,
    val isOwn: Boolean = false,
    // dataType: "message" | "file" | "voice" | "image" | "gif" | "group_invite"
    val dataType: String = "message",
    // Common
    val isDeleted: Boolean = false,
    val deletedBy: String = "",
    val isEdited: Boolean = false,
    val isForwarded: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val readBy: Set<String> = emptySet(),
    // Reply
    val replyToId: String = "",
    val replyToSender: String = "",
    val replyToPreview: String = "",
    val replyToDataType: String = "",
    // message
    val text: String = "",
    // file (.ntf)
    val fileData: String = "",
    val fileName: String = "",
    val dirName: String = "",
    val fileSize: Long = 0L,
    // voice
    val voiceData: String = "",
    val voiceDuration: Int = 0,
    // image
    val imageData: String = "",
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val imageUrl: String = "",
    // group_invite
    val groupId: String = "",
    val groupName: String = "",
    val invitedBy: String = "",
    val invitedByRole: String = "",
    val autoAdd: Boolean = false
)

data class FriendChatUiState(
    val chatId: String = "",
    val friendUserId: String = "",
    val friendDisplayName: String = "",
    val friendAvatarLetter: Char = '?',
    val myUserId: String = "",
    val myDisplayName: String = "",
    val isAdmin: Boolean = false,
    val messages: List<ChatMsg> = emptyList(),
    val isConnected: Boolean = false,
    val isBlocked: Boolean = false,
    val isBlockedByFriend: Boolean = false,
    val blockedByName: String = "",
    val showDirectoryPicker: Boolean = false,
    val userDirectories: List<UserDirectory> = emptyList(),
    val selectedDirectory: UserDirectory? = null,
    val showSendConfirm: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val isDownloading: Boolean = false,
    val snackMessage: String = "",
    val replyingTo: ChatMsg? = null,
    val editingMsg: ChatMsg? = null,
    val isSelectMode: Boolean = false,
    val selectedMsgIds: Set<String> = emptySet(),
    val friendIsOnline: Boolean = false,
    val isRecording: Boolean = false,
    val highlightedMsgId: String = "",
    val favEmoji: String = "\uD83D\uDC4D"
)

@HiltViewModel
class FriendChatViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userDataRepository: UserDataRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendChatUiState())
    val uiState: StateFlow<FriendChatUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private var msgListener: ValueEventListener? = null
    private var msgRef: com.google.firebase.database.DatabaseReference? = null
    private var myRoomUserId = -1L

    fun init(chatIdParam: String, friendUserId: String) {
        removeListener()
        viewModelScope.launch {
            val savedEmoji = preferencesManager.favEmoji.first()
            _uiState.value = _uiState.value.copy(favEmoji = savedEmoji)
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val myId = user?.username ?: ""
            myRoomUserId = userId
            val isAdmin = user?.role == "admin"
            val chatId = if (chatIdParam == "resolve" || chatIdParam.isBlank())
                buildChatId(myId, friendUserId) else chatIdParam
            _uiState.value = _uiState.value.copy(
                chatId = chatId, friendUserId = friendUserId,
                myUserId = myId, messages = emptyList(), isAdmin = isAdmin
            )
            db.getReference("online_profiles").child(friendUserId).get()
                .addOnSuccessListener { pSnap ->
                    val nick = pSnap.child("nickname").getValue(String::class.java) ?: ""
                    val uname = pSnap.child("username").getValue(String::class.java) ?: friendUserId
                    val name = nick.ifBlank { uname }
                    _uiState.value = _uiState.value.copy(
                        friendDisplayName = name,
                        friendAvatarLetter = name.firstOrNull()?.uppercaseChar() ?: '?'
                    )
                    db.getReference("friends").child(myId).child(friendUserId)
                        .updateChildren(mapOf("displayName" to name))
                }
            db.getReference("friends").child(myId).child(friendUserId).get()
                .addOnSuccessListener { snap ->
                    val dName = snap.child("displayName").getValue(String::class.java)
                    if (dName != null && _uiState.value.friendDisplayName.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            friendDisplayName = dName,
                            friendAvatarLetter = dName.firstOrNull()?.uppercaseChar() ?: '?'
                        )
                    }
                    if (myId.isNotEmpty()) {
                        db.getReference("online_profiles").child(myId).get()
                            .addOnSuccessListener { snap ->
                                val nick = snap.child("nickname").getValue(String::class.java) ?: ""
                                val uname = snap.child("username").getValue(String::class.java) ?: myId
                                _uiState.value = _uiState.value.copy(myDisplayName = nick.ifBlank { uname })
                            }
                    }
                    if (myId.isNotEmpty()) {
                        db.getReference("block_list").child(myId).child(friendUserId).get()
                            .addOnSuccessListener { snap ->
                                val blockerName = snap.child("blockerName").getValue(String::class.java)
                                if (blockerName != null) {
                                    _uiState.value = _uiState.value.copy(
                                        isBlockedByFriend = true, blockedByName = blockerName, isBlocked = false
                                    )
                                } else if (snap.exists()) {
                                    _uiState.value = _uiState.value.copy(isBlocked = true, isBlockedByFriend = false)
                                }
                            }
                    }
                    if (userId > 0 && !isGuest) {
                        viewModelScope.launch {
                            userDirectoryRepository.getDirectoriesByUserId(userId).collect { dirs ->
                                _uiState.value = _uiState.value.copy(userDirectories = dirs)
                            }
                        }
                    }
                    if (chatId.isNotEmpty() && myId.isNotEmpty()) {
                        listenMessages(chatId, myId)
                        val ref = db.getReference("online_profiles").child(myId).child("lastOnline")
                        ref.setValue("online")
                    }
                }
        }
    }

    fun buildChatId(a: String, b: String): String =
        if (a < b) a + "_" + b else b + "_" + a

    private var onlineListener: ValueEventListener? = null
    private var onlineRef: com.google.firebase.database.DatabaseReference? = null

    fun listenFriendOnline(friendUserId: String) {
        onlineRef = db.getReference("online_profiles").child(friendUserId).child("lastOnline")
        onlineListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val isOnline = try {
                    when {
                        !snap.exists() -> false
                        snap.getValue(String::class.java) == "online" -> true
                        else -> {
                            val ts = snap.getValue(Long::class.java) ?: 0L
                            ts > 0L && (System.currentTimeMillis() - ts) < 2 * 60 * 1000L
                        }
                    }
                } catch (_: Exception) { false }
                _uiState.value = _uiState.value.copy(friendIsOnline = isOnline)
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        onlineRef!!.addValueEventListener(onlineListener!!)
    }

    fun setMyOnlineStatus(online: Boolean) {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        val ref = db.getReference("online_profiles").child(myId).child("lastOnline")
        if (online) ref.setValue("online") else ref.setValue(System.currentTimeMillis())
    }

    fun markAllRead() {
        val state = _uiState.value
        val myId  = state.myUserId; if (myId.isEmpty()) return
        val base  = db.getReference("friend_chats").child(state.chatId).child("messages")
        for (msg in state.messages) {
            if (!msg.isOwn && myId !in msg.readBy && !msg.isDeleted) {
                base.child(msg.id).child("read_by").child(myId).setValue(System.currentTimeMillis())
            }
        }
    }

    fun detachOnline() {
        onlineListener?.let { onlineRef?.removeEventListener(it) }
        onlineListener = null
    }

    private fun listenMessages(chatId: String, myId: String) {
        msgRef = db.getReference("friend_chats").child(chatId).child("messages")
        // Auto-wipe removed (requires Firebase index)
        val query = msgRef!!.orderByChild("timestamp").limitToLast(50)
        msgListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = mutableListOf<ChatMsg>()
                for (child in snapshot.children) {
                    msgs.add(parseMsg(child, myId) ?: continue)
                }
                _uiState.value = _uiState.value.copy(
                    messages    = msgs.sortedBy { it.timestamp },
                    isConnected = true
                )
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(isConnected = false)
            }
        }
        query.addValueEventListener(msgListener!!)
        _uiState.value = _uiState.value.copy(isConnected = true)
    }

    private fun parseMsg(child: DataSnapshot, myId: String): ChatMsg? {
        val senderId = child.child("senderId").getValue(String::class.java) ?: return null
        val msgType  = child.child("msgType").getValue(String::class.java) ?: "text"
        val dataType = child.child("dataType").getValue(String::class.java)
            ?: when (msgType) { "file" -> "file"; "voice" -> "voice"; "group_invite" -> "group_invite"; else -> "message" }
        val ts       = child.child("timestamp").getValue(Long::class.java) ?: 0L
        val sName    = child.child("senderName").getValue(String::class.java) ?: senderId
        val letter   = sName.firstOrNull()?.uppercaseChar() ?: '?'
        if (msgType == "group_invite") {
            val text   = child.child("text").getValue(String::class.java) ?: "Group invite"
            val ts2    = child.child("timestamp").getValue(Long::class.java) ?: 0L
            val sName2 = child.child("senderName").getValue(String::class.java) ?: senderId
            val letter2 = sName2.firstOrNull()?.uppercaseChar() ?: '?'
            return ChatMsg(
                id = child.key ?: "", senderId = senderId, senderName = sName2,
                senderAvatarLetter = letter2, text = text, timestamp = ts2,
                isOwn = senderId == myId, dataType = "group_invite",
                groupId       = child.child("groupId").getValue(String::class.java) ?: "",
                groupName     = child.child("groupName").getValue(String::class.java) ?: "",
                invitedBy     = child.child("invitedBy").getValue(String::class.java) ?: "",
                invitedByRole = child.child("invitedByRole").getValue(String::class.java) ?: "",
                autoAdd       = child.child("autoAdd").getValue(Boolean::class.java) ?: false
            )
        }
        if (dataType == "voice") {
            val vDeleted   = child.child("deleted").getValue(Boolean::class.java) ?: false
            val vDeletedBy = child.child("deletedBy").getValue(String::class.java) ?: ""
            val vForwarded = child.child("isForwarded").getValue(Boolean::class.java) ?: false
            val vReactions = mutableMapOf<String, List<String>>()
            if (!vDeleted) {
                for (emojiSnap in child.child("reactions").children) {
                    val emoji = emojiSnap.key ?: continue
                    val names = emojiSnap.children.mapNotNull { it.getValue(String::class.java) }.filter { it.isNotBlank() }
                    if (names.isNotEmpty()) vReactions[emoji] = names
                }
            }
            val vReadBy = child.child("read_by").children.mapNotNull { it.key }.toSet()
            return ChatMsg(
                id = child.key ?: "", senderId = senderId, senderName = sName,
                senderAvatarLetter = letter, timestamp = ts,
                isOwn = senderId == myId, dataType = "voice",
                isDeleted = vDeleted, deletedBy = vDeletedBy,
                isForwarded = vForwarded, reactions = vReactions, readBy = vReadBy,
                voiceData     = child.child("voiceData").getValue(String::class.java) ?: "",
                voiceDuration = child.child("voiceDuration").getValue(Int::class.java) ?: 0,
                replyToId     = child.child("replyToId").getValue(String::class.java) ?: "",
                replyToPreview = child.child("replyToPreview").getValue(String::class.java)
                    ?: child.child("replyToText").getValue(String::class.java) ?: "",
                replyToSender = child.child("replyToSender").getValue(String::class.java) ?: "",
                replyToDataType = child.child("replyToDataType").getValue(String::class.java) ?: ""
            )
        }
        val fDeleted   = child.child("deleted").getValue(Boolean::class.java) ?: false
        val fDeletedBy = child.child("deletedBy").getValue(String::class.java) ?: ""
        val fForwarded = child.child("isForwarded").getValue(Boolean::class.java) ?: false
        val fReactions = mutableMapOf<String, List<String>>()
        if (!fDeleted) {
            for (emojiSnap in child.child("reactions").children) {
                val emoji = emojiSnap.key ?: continue
                val names = emojiSnap.children.mapNotNull { it.getValue(String::class.java) }.filter { it.isNotBlank() }
                if (names.isNotEmpty()) fReactions[emoji] = names
            }
        }
        val fReadBy = child.child("read_by").children.mapNotNull { it.key }.toSet()
        if (dataType == "file") {
            return ChatMsg(
                id = child.key ?: "", senderId = senderId, senderName = sName,
                senderAvatarLetter = letter, timestamp = ts,
                isOwn = senderId == myId, dataType = "file",
                isDeleted = fDeleted, deletedBy = fDeletedBy,
                isForwarded = fForwarded, reactions = fReactions, readBy = fReadBy,
                replyToId = child.child("replyToId").getValue(String::class.java) ?: "",
                replyToPreview = child.child("replyToPreview").getValue(String::class.java)
                    ?: child.child("replyToText").getValue(String::class.java) ?: "",
                replyToSender = child.child("replyToSender").getValue(String::class.java) ?: "",
                replyToDataType = child.child("replyToDataType").getValue(String::class.java) ?: "",
                fileData = child.child("fileData").getValue(String::class.java) ?: "",
                fileName = child.child("fileName").getValue(String::class.java) ?: "",
                dirName  = child.child("dirName").getValue(String::class.java) ?: "",
                fileSize = child.child("fileSize").getValue(Long::class.java) ?: 0L
            )
        }
        if (dataType == "image") {
            val readBy = mutableSetOf<String>()
            for (rSnap in child.child("read_by").children) { rSnap.key?.let { readBy.add(it) } }
            val reactions = mutableMapOf<String, List<String>>()
            if (!fDeleted) {
                for (emojiSnap in child.child("reactions").children) {
                    val emoji = emojiSnap.key ?: continue
                    val names = emojiSnap.children.mapNotNull { it.getValue(String::class.java) }.filter { it.isNotBlank() }
                    if (names.isNotEmpty()) reactions[emoji] = names
                }
            }
            return ChatMsg(
                id = child.key ?: "", senderId = senderId, senderName = sName,
                senderAvatarLetter = letter, timestamp = ts,
                isOwn = senderId == myId, dataType = "image",
                imageData = child.child("imageData").getValue(String::class.java) ?: "",
                imageWidth = child.child("imageWidth").getValue(Int::class.java) ?: 0,
                imageHeight = child.child("imageHeight").getValue(Int::class.java) ?: 0,
                imageUrl = child.child("imageUrl").getValue(String::class.java) ?: "",
                isDeleted = fDeleted, deletedBy = fDeletedBy,
                replyToId = child.child("replyToId").getValue(String::class.java) ?: "",
                replyToPreview = child.child("replyToPreview").getValue(String::class.java) ?: "",
                replyToSender = child.child("replyToSender").getValue(String::class.java) ?: "",
                replyToDataType = child.child("replyToDataType").getValue(String::class.java) ?: "",
                readBy = readBy, reactions = reactions
            )
        }
        val text        = child.child("text").getValue(String::class.java) ?: return null
        val isDeleted   = child.child("deleted").getValue(Boolean::class.java) ?: false
        val deletedBy   = child.child("deletedBy").getValue(String::class.java) ?: ""
        val isEdited    = child.child("isEdited").getValue(Boolean::class.java) ?: false
        val isForwarded = child.child("isForwarded").getValue(Boolean::class.java) ?: false
        val replyToId      = child.child("replyToId").getValue(String::class.java) ?: ""
        val replyToPreview = child.child("replyToPreview").getValue(String::class.java)
            ?: child.child("replyToText").getValue(String::class.java) ?: ""
        val replyToSender  = child.child("replyToSender").getValue(String::class.java) ?: ""
        val replyToDataType = child.child("replyToDataType").getValue(String::class.java) ?: ""
        val readBy = mutableSetOf<String>()
        for (rSnap in child.child("read_by").children) { rSnap.key?.let { readBy.add(it) } }
        val reactions = mutableMapOf<String, List<String>>()
        if (!isDeleted) {
            for (emojiSnap in child.child("reactions").children) {
                val emoji = emojiSnap.key ?: continue
                val names = emojiSnap.children.mapNotNull { it.getValue(String::class.java) }.filter { it.isNotBlank() }
                if (names.isNotEmpty()) reactions[emoji] = names
            }
        }
        return ChatMsg(
            id = child.key ?: "", senderId = senderId, senderName = sName,
            senderAvatarLetter = letter, text = text, timestamp = ts,
            isOwn = senderId == myId, isDeleted = isDeleted, deletedBy = deletedBy,
            isEdited = isEdited, isForwarded = isForwarded, readBy = readBy, replyToId = replyToId,
            replyToPreview = replyToPreview, replyToSender = replyToSender,
            replyToDataType = replyToDataType, reactions = reactions
        )
    }

    fun sendMessage(text: String) {
        val state = _uiState.value
        if (text.isBlank() || state.chatId.isEmpty() || state.myUserId.isEmpty()) return
        if (state.isBlockedByFriend) return
        if (state.isBlocked) {
            db.getReference("block_list").child(state.myUserId).child(state.friendUserId).removeValue()
            db.getReference("block_list").child(state.friendUserId).child(state.myUserId).removeValue()
            _uiState.value = _uiState.value.copy(isBlocked = false, isBlockedByFriend = false, blockedByName = "")
        }
        val editing = state.editingMsg
        if (editing != null) {
            db.getReference("friend_chats").child(state.chatId).child("messages")
                .child(editing.id).updateChildren(mapOf("text" to text.trim(), "isEdited" to true))
            _uiState.value = _uiState.value.copy(editingMsg = null)
            return
        }
        val trimmed = text.trim()
        val payload = mutableMapOf(
            "senderId"   to state.myUserId,
            "senderName" to state.myDisplayName,
            "dataType"   to "message",
            "text"       to trimmed,
            "timestamp"  to ServerValue.TIMESTAMP
        )
        val reply = state.replyingTo
        if (reply != null) {
            payload["replyToId"]      = reply.id
            payload["replyToPreview"] = reply.replyToPreview.ifBlank {
                when (reply.dataType) {
                    "voice" -> "\uD83C\uDF99 Voice message (${reply.voiceDuration}s)"
                    "file"  -> "\uD83D\uDCC1 ${reply.fileName}"
                    "image" -> "\uD83D\uDDBC\uFE0F Image"
                    else    -> reply.text.take(80)
                }
            }
            payload["replyToSender"]   = reply.senderName
            payload["replyToDataType"] = reply.dataType
        }
        db.getReference("friend_chats").child(state.chatId).child("messages").push().setValue(payload)
        val myPreview    = "You: $trimmed"
        val theirPreview = state.myDisplayName + ": " + trimmed
        db.reference.updateChildren(mapOf(
            "friend_chats/" + state.chatId + "/last_message/text"              to trimmed,
            "friend_chats/" + state.chatId + "/last_message/timestamp"         to ServerValue.TIMESTAMP,
            "friend_chats/" + state.chatId + "/last_message/senderId"          to state.myUserId,
            "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsg"     to myPreview,
            "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsgTime" to ServerValue.TIMESTAMP,
            "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsg"     to theirPreview,
            "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsgTime" to ServerValue.TIMESTAMP
        ))
        if (reply != null) _uiState.value = _uiState.value.copy(replyingTo = null)
    }

    fun sendImage(base64Jpeg: String, width: Int, height: Int) {
        val state = _uiState.value
        android.util.Log.d("SENDIMG", "sendImage called b64len=${base64Jpeg.length} userId=${state.myUserId} chatId=${state.chatId}")
        if (base64Jpeg.isBlank() || state.myUserId.isEmpty()) return
        val preview = "\uD83D\uDDBC\uFE0F Image"
        viewModelScope.launch {
            val imgUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { uploadToImgBB(base64Jpeg) }
            android.util.Log.d("SENDIMG", "imgUrl=$imgUrl")
            val payload = mutableMapOf(
                "senderId"    to state.myUserId,
                "senderName"  to state.myDisplayName,
                "dataType"    to "image",
                "imageWidth"  to width,
                "imageHeight" to height,
                "text"        to preview,
                "timestamp"   to ServerValue.TIMESTAMP
            )
            if (imgUrl != null) payload["imageUrl"] = imgUrl else payload["imageData"] = base64Jpeg
            val reply = state.replyingTo
            if (reply != null) {
                payload["replyToId"]       = reply.id
                payload["replyToPreview"]  = reply.replyToPreview.ifBlank { reply.text.take(60) }
                payload["replyToDataType"] = reply.dataType
                payload["replyToSender"]   = reply.senderName
                _uiState.value = _uiState.value.copy(replyingTo = null)
            }
            db.getReference("friend_chats").child(state.chatId).child("messages").push().setValue(payload)
            val now = System.currentTimeMillis()
            db.getReference("friends").child(state.myUserId).child(state.friendUserId)
                .updateChildren(mapOf("lastMsg" to "You: $preview", "lastMsgTime" to now))
            db.getReference("friends").child(state.friendUserId).child(state.myUserId)
                .updateChildren(mapOf("lastMsg" to "${state.myDisplayName}: $preview", "lastMsgTime" to now))
        }
    }

    fun acceptGroupInvite(msg: ChatMsg) {
        val state = _uiState.value
        val myId  = state.myUserId; if (myId.isEmpty()) return
        if (msg.autoAdd) {
            val memberData = mapOf("role" to "member", "joinedAt" to ServerValue.TIMESTAMP)
            db.getReference("group_members").child(msg.groupId).child(myId).setValue(memberData)
            db.getReference("group_chats").child(msg.groupId).child("member_count").get()
                .addOnSuccessListener { snap ->
                    val newCount = (snap.getValue(Int::class.java) ?: 0) + 1
                    db.getReference("group_chats").child(msg.groupId).child("member_count").setValue(newCount)
                    db.getReference("group_members").child(msg.groupId).get()
                        .addOnSuccessListener { mSnap ->
                            for (child in mSnap.children) {
                                val uid = child.key ?: continue
                                db.getReference("user_groups").child(uid).child(msg.groupId)
                                    .child("memberCount").setValue(newCount)
                            }
                        }
                }
            db.getReference("user_groups").child(myId).child(msg.groupId).setValue(
                mapOf("group_name" to msg.groupName, "role" to "member"))
        } else {
            db.getReference("group_requests").child(msg.groupId).child(myId).setValue(mapOf(
                "from" to msg.invitedBy, "fromRole" to msg.invitedByRole, "timestamp" to ServerValue.TIMESTAMP
            ))
        }
        db.getReference("friend_chats").child(state.chatId).child("messages").child(msg.id).removeValue()
    }

    fun declineGroupInvite(msg: ChatMsg) {
        val state = _uiState.value
        db.getReference("friend_chats").child(state.chatId).child("messages").child(msg.id).removeValue()
    }

    fun sendVoiceMessage(base64Data: String, durationSec: Int) {
        val state = _uiState.value
        if (base64Data.isBlank() || state.myUserId.isEmpty()) return
        val now = System.currentTimeMillis()
        val payload = mutableMapOf(
            "senderId"      to state.myUserId,
            "senderName"    to state.myDisplayName,
            "dataType"      to "voice",
            "voiceData"     to base64Data,
            "voiceDuration" to durationSec,
            "text"          to "\uD83C\uDF99 Voice message",
            "timestamp"     to ServerValue.TIMESTAMP
        )
        val reply = state.replyingTo
        if (reply != null) {
            payload["replyToId"]       = reply.id
            payload["replyToPreview"]  = reply.replyToPreview.ifBlank { reply.text.take(60) }
            payload["replyToDataType"] = reply.dataType
            payload["replyToSender"]   = reply.senderName
            _uiState.value = _uiState.value.copy(replyingTo = null)
        }
        db.getReference("friend_chats").child(state.chatId).child("messages").push().setValue(payload)
        val lastMsgUpdate = mapOf("lastMsg" to "\uD83C\uDF99 Voice message", "lastMsgTime" to now)
        db.getReference("friends").child(state.myUserId).child(state.friendUserId).updateChildren(lastMsgUpdate)
        db.getReference("friends").child(state.friendUserId).child(state.myUserId).updateChildren(lastMsgUpdate)
    }

    fun highlightMsg(id: String) {
        _uiState.value = _uiState.value.copy(highlightedMsgId = id)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2100L)
            _uiState.value = _uiState.value.copy(highlightedMsgId = "")
        }
    }

    fun setFavEmoji(emoji: String) {
        _uiState.value = _uiState.value.copy(favEmoji = emoji)
        viewModelScope.launch { preferencesManager.setFavEmoji(emoji) }
    }

    fun setReplyTo(msg: ChatMsg) {
        val preview = when (msg.dataType) {
            "voice" -> "\uD83C\uDF99 Voice message (${msg.voiceDuration}s)"
            "file"  -> "\uD83D\uDCC1 ${msg.fileName}"
            "image" -> "\uD83D\uDDBC\uFE0F Image"
            else    -> msg.text.take(80)
        }
        _uiState.value = _uiState.value.copy(
            replyingTo = msg.copy(replyToPreview = preview, replyToDataType = msg.dataType),
            editingMsg = null
        )
    }
    fun clearReply()              { _uiState.value = _uiState.value.copy(replyingTo = null) }
    fun startEdit(msg: ChatMsg)   { _uiState.value = _uiState.value.copy(editingMsg = msg, replyingTo = null) }
    fun cancelEdit()              { _uiState.value = _uiState.value.copy(editingMsg = null) }
    fun canEdit(msg: ChatMsg): Boolean = (msg.isOwn || _uiState.value.isAdmin) && !msg.isDeleted
    fun canDeleteForEveryone(msg: ChatMsg): Boolean = msg.isOwn || _uiState.value.isAdmin

    fun deleteForMe(msg: ChatMsg) {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        val chatId = _uiState.value.chatId
        db.getReference("friend_chats").child(chatId).child("messages")
            .child(msg.id).child("deleted_for").child(myId).setValue(true)
    }

    fun deleteForEveryone(msg: ChatMsg) {
        if (!canDeleteForEveryone(msg)) return
        val state = _uiState.value
        val label = if (msg.isOwn) "Deleted by ${state.myDisplayName}" else "Deleted by Admin"
        val msgRef = db.getReference("friend_chats").child(state.chatId).child("messages").child(msg.id)
        msgRef.updateChildren(mapOf("deleted" to true, "deletedBy" to label, "text" to label))
        msgRef.child("reactions").removeValue()
        if (state.replyingTo?.id == msg.id) _uiState.value = _uiState.value.copy(replyingTo = null)
        if (state.editingMsg?.id == msg.id) _uiState.value = _uiState.value.copy(editingMsg = null)
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000L)
            msgRef.removeValue()
            db.getReference("friend_chats").child(state.chatId).child("messages")
                .orderByChild("timestamp").limitToLast(1).get()
                .addOnSuccessListener { snap ->
                    val last    = snap.children.lastOrNull()
                    val newText = last?.child("text")?.getValue(String::class.java) ?: ""
                    val newTs   = last?.child("timestamp")?.getValue(Long::class.java) ?: 0L
                    db.reference.updateChildren(mapOf(
                        "friend_chats/" + state.chatId + "/last_message/text"                   to newText,
                        "friend_chats/" + state.chatId + "/last_message/timestamp"              to newTs,
                        "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsg"     to newText,
                        "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsgTime" to newTs,
                        "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsg"     to newText,
                        "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsgTime" to newTs
                    ))
                }
        }
    }

    fun toggleReaction(msg: ChatMsg, emoji: String) {
        val state = _uiState.value
        if (state.myUserId.isEmpty() || msg.isDeleted) return
        val ref = db.getReference("friend_chats").child(state.chatId)
            .child("messages").child(msg.id).child("reactions").child(emoji).child(state.myUserId)
        val existing = msg.reactions[emoji]?.contains(state.myDisplayName) == true
        if (existing) ref.removeValue() else ref.setValue(state.myDisplayName)
    }

    fun enterSelectMode(msg: ChatMsg) {
        _uiState.value = _uiState.value.copy(
            isSelectMode = true, selectedMsgIds = setOf(msg.id),
            replyingTo = null, editingMsg = null)
    }
    fun toggleSelect(msg: ChatMsg) {
        val cur = _uiState.value.selectedMsgIds.toMutableSet()
        if (msg.id in cur) cur.remove(msg.id) else cur.add(msg.id)
        _uiState.value = _uiState.value.copy(selectedMsgIds = cur)
    }
    fun exitSelectMode() { _uiState.value = _uiState.value.copy(isSelectMode = false, selectedMsgIds = emptySet()) }
    fun getSelectedMessages(): List<ChatMsg> {
        val ids = _uiState.value.selectedMsgIds
        return _uiState.value.messages.filter { it.id in ids }
    }
    fun deleteSelectedForMe() {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        val chatId = _uiState.value.chatId
        val base = db.getReference("friend_chats").child(chatId).child("messages")
        for (id in _uiState.value.selectedMsgIds)
            base.child(id).child("deleted_for").child(myId).setValue(true)
        exitSelectMode()
    }
    fun deleteSelectedForEveryone() {
        val state = _uiState.value
        val base  = db.getReference("friend_chats").child(state.chatId).child("messages")
        for (id in state.selectedMsgIds) {
            val msg   = state.messages.find { it.id == id } ?: continue
            val label = if (msg.isOwn) "Deleted by ${state.myDisplayName}" else "Deleted by Admin"
            base.child(id).updateChildren(mapOf("deleted" to true, "deletedBy" to label, "text" to label))
            base.child(id).child("reactions").removeValue()
        }
        exitSelectMode()
    }
    fun canDeleteSelectedForEveryone(): Boolean {
        val state = _uiState.value
        return state.selectedMsgIds.all { id ->
            val msg = state.messages.find { it.id == id }
            msg != null && (msg.isOwn || state.isAdmin)
        }
    }

    fun onAttachTapped()           { _uiState.value = _uiState.value.copy(showDirectoryPicker = true) }
    fun onDismissDirectoryPicker() { _uiState.value = _uiState.value.copy(showDirectoryPicker = false) }
    fun onDismissSendConfirm()     { _uiState.value = _uiState.value.copy(showSendConfirm = false, selectedDirectory = null) }

    fun onDirectorySelected(dir: UserDirectory) {
        _uiState.value = _uiState.value.copy(
            showDirectoryPicker = false, selectedDirectory = dir, showSendConfirm = true
        )
    }

    fun confirmSendDirectory() {
        val state = _uiState.value
        val dir   = state.selectedDirectory ?: return
        _uiState.value = _uiState.value.copy(showSendConfirm = false, selectedDirectory = null, isUploading = true)
        viewModelScope.launch {
            try {
                val words    = userDataRepository.getNotesByDirectory(myRoomUserId, dir.id).first()
                val json     = NtfExporter.export(dir, words)
                val fileSize = json.toByteArray(Charsets.UTF_8).size.toLong()
                val fileName = dir.name.replace(" ", "_") + ".ntf"
                val preview  = "Sent a file: " + dir.name
                val payload  = mapOf(
                    "senderId"   to state.myUserId,
                    "senderName" to state.myDisplayName,
                    "dataType"   to "file",
                    "fileData"   to json,
                    "fileName"   to fileName,
                    "dirName"    to dir.name,
                    "fileSize"   to fileSize,
                    "text"       to ("[File: " + dir.name + "]"),
                    "timestamp"  to ServerValue.TIMESTAMP
                )
                db.getReference("friend_chats").child(state.chatId).child("messages").push()
                    .setValue(payload)
                    .addOnSuccessListener {
                        db.reference.updateChildren(mapOf(
                            "friend_chats/" + state.chatId + "/last_message/text"              to preview,
                            "friend_chats/" + state.chatId + "/last_message/timestamp"         to ServerValue.TIMESTAMP,
                            "friend_chats/" + state.chatId + "/last_message/senderId"          to state.myUserId,
                            "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsg"     to ("You: $preview"),
                            "friends/" + state.myUserId + "/" + state.friendUserId + "/lastMsgTime" to ServerValue.TIMESTAMP,
                            "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsg"     to (state.myDisplayName + ": " + preview),
                            "friends/" + state.friendUserId + "/" + state.myUserId + "/lastMsgTime" to ServerValue.TIMESTAMP
                        ))
                        _uiState.value = _uiState.value.copy(
                            isUploading = false, uploadProgress = 0, snackMessage = dir.name + " sent!"
                        )
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isUploading = false, snackMessage = "Send failed: " + (e.message ?: "unknown error")
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading = false, snackMessage = "Export failed: " + (e.message ?: "unknown error")
                )
            }
        }
    }

    fun downloadAndImport(msg: ChatMsg) {
        if (msg.fileData.isBlank() || myRoomUserId <= 0) return
        _uiState.value = _uiState.value.copy(isDownloading = true)
        viewModelScope.launch {
            try {
                val result = NtfImporter.import(
                    json = msg.fileData, userId = myRoomUserId,
                    directoryRepo = userDirectoryRepository, userDataRepo = userDataRepository
                )
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    snackMessage  = if (result.success)
                        "Imported " + result.directoryName + " (" + result.wordCount + " words) into My Note!"
                    else "Import failed: " + result.error
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false, snackMessage = "Import failed: " + (e.message ?: "unknown error")
                )
            }
        }
    }

    private fun uploadToImgBB(base64Jpeg: String): String? {
        return try {
            val apiKey = com.nueng.translator.BuildConfig.IMGBB_API_KEY
            val url = java.net.URL("https://api.imgbb.com/1/upload")
            val boundary = "----FormBoundary${System.currentTimeMillis()}"
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.connectTimeout = 30000
            conn.readTimeout    = 30000
            val os = conn.outputStream
            val nl = "\r\n".toByteArray(Charsets.UTF_8)
            val dd = "--".toByteArray(Charsets.UTF_8)
            val b  = boundary.toByteArray(Charsets.UTF_8)
            // key field
            os.write(dd); os.write(b); os.write(nl)
            os.write("Content-Disposition: form-data; name=\"key\"".toByteArray(Charsets.UTF_8))
            os.write(nl); os.write(nl)
            os.write(apiKey.toByteArray(Charsets.UTF_8)); os.write(nl)
            // image field
            os.write(dd); os.write(b); os.write(nl)
            os.write("Content-Disposition: form-data; name=\"image\"".toByteArray(Charsets.UTF_8))
            os.write(nl); os.write(nl)
            os.write(base64Jpeg.toByteArray(Charsets.UTF_8)); os.write(nl)
            // closing boundary
            os.write(dd); os.write(b); os.write(dd); os.write(nl)
            os.flush(); os.close()
            val resp = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val unescaped = resp.replace("\\/", "/")
            val match = Regex("\"display_url\":\"([^\"]+)\"").find(unescaped)
            match?.groupValues?.get(1)
        } catch (e: Exception) { android.util.Log.e("IMGBB", "Upload failed", e); null }
    }

    fun clearSnackMessage() { _uiState.value = _uiState.value.copy(snackMessage = "") }
    fun detach() { removeListener() }

    private fun removeListener() {
        msgListener?.let { msgRef?.removeEventListener(it) }
        msgListener = null
        msgRef      = null
    }

    override fun onCleared() {
        super.onCleared()
        removeListener()
    }
}