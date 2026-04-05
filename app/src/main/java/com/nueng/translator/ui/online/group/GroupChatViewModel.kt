package com.nueng.translator.ui.online.group

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

data class GroupChatMsg(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarLetter: Char = '?',
    val timestamp: Long = 0L,
    val isOwn: Boolean = false,
    // dataType: "message" | "file" | "voice" | "image" | "gif"
    val dataType: String = "message",
    // Common
    val isDeleted: Boolean = false,
    val deletedBy: String = "",
    val isEdited: Boolean = false,
    val isForwarded: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val readBy: Map<String, String> = emptyMap(),
    // Reply
    val replyToId: String = "",
    val replyToSender: String = "",
    val replyToPreview: String = "",
    val replyToDataType: String = "",
    // message
    val text: String = "",
    // file
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
    val imageUrl: String = ""
)

data class GroupChatUiState(
    val groupId: String = "",
    val groupName: String = "",
    val groupAvatarLetter: Char = 'G',
    val groupAvatarUrl: String = "",
    val myUserId: String = "",
    val myDisplayName: String = "",
    val myRole: String = "member",
    val messages: List<GroupChatMsg> = emptyList(),
    val isConnected: Boolean = false,
    val isLoading: Boolean = true,
    val showDirectoryPicker: Boolean = false,
    val userDirectories: List<UserDirectory> = emptyList(),
    val selectedDirectory: UserDirectory? = null,
    val showSendConfirm: Boolean = false,
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val isDownloading: Boolean = false,
    val snackMessage: String = "",
    val replyingTo: GroupChatMsg? = null,
    val editingMsg: GroupChatMsg? = null,
    val isSelectMode: Boolean = false,
    val selectedMsgIds: Set<String> = emptySet(),
    val memberCount: Int = 0,
    val groupMembers: List<Pair<String,String>> = emptyList(),
    val myJoinedAt: Long = 0L,
    val highlightedMsgId: String = "",
    val favEmoji: String = "\uD83D\uDC4D",
    val isRecording: Boolean = false
)

@HiltViewModel
class GroupChatViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val userRepository: UserRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userDataRepository: UserDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupChatUiState())
    val uiState: StateFlow<GroupChatUiState> = _uiState.asStateFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
    )

    private var msgListener: ValueEventListener? = null
    private var msgRef: com.google.firebase.database.DatabaseReference? = null
    private var myRoomUserId = -1L

    fun init(groupId: String) {
        if (_uiState.value.groupId == groupId && !_uiState.value.isLoading) return
        removeListener()
        viewModelScope.launch {
            val userId = preferencesManager.loggedInUserId.first()
            val isGuest = preferencesManager.isGuest.first()
            val user = if (userId > 0 && !isGuest) userRepository.getUserById(userId) else null
            val myId = user?.username ?: ""
            myRoomUserId = userId

            _uiState.value = _uiState.value.copy(
                groupId = groupId, myUserId = myId,
                isLoading = true, messages = emptyList()
            )

            db.getReference("user_groups").child(myId).child(groupId).get()
                .addOnSuccessListener { snap ->
                    val name = snap.child("group_name").getValue(String::class.java) ?: "Group"
                    val role = snap.child("role").getValue(String::class.java) ?: "member"
                    val letter = name.firstOrNull()?.uppercaseChar() ?: 'G'
                    _uiState.value = _uiState.value.copy(
                        groupName = name, groupAvatarLetter = letter, myRole = role
                    )
                    db.getReference("group_chats").child(groupId).child("avatarUrl").get()
                        .addOnSuccessListener { avatarSnap ->
                            val url = avatarSnap.getValue(String::class.java) ?: ""
                            _uiState.value = _uiState.value.copy(groupAvatarUrl = url)
                        }
                }

            if (myId.isNotEmpty()) {
                db.getReference("online_profiles").child(myId).get()
                    .addOnSuccessListener { snap ->
                        val nick = snap.child("nickname").getValue(String::class.java) ?: ""
                        val uname = snap.child("username").getValue(String::class.java) ?: myId
                        _uiState.value = _uiState.value.copy(myDisplayName = nick.ifBlank { uname })
                    }
            }

            if (userId > 0 && !isGuest) {
                viewModelScope.launch {
                    userDirectoryRepository.getDirectoriesByUserId(userId).collect { dirs ->
                        _uiState.value = _uiState.value.copy(userDirectories = dirs)
                    }
                }
            }

            val savedFav = preferencesManager.favEmoji.first()
            _uiState.value = _uiState.value.copy(favEmoji = savedFav)

            if (groupId.isNotEmpty() && myId.isNotEmpty()) {
                db.getReference("group_members").child(groupId).child(myId).get()
                    .addOnSuccessListener { snap ->
                        val joinedAt = snap.child("joinedAt").getValue(Long::class.java)
                            ?: snap.child("joined_at").getValue(Long::class.java) ?: 0L
                        _uiState.value = _uiState.value.copy(myJoinedAt = joinedAt)
                        listenMessages(groupId, myId)
                    }
                    .addOnFailureListener { listenMessages(groupId, myId) }
                val ref = db.getReference("online_profiles").child(myId).child("lastOnline")
                ref.setValue("online")
                db.getReference("group_members").child(groupId).get()
                    .addOnSuccessListener { snap ->
                        val usernames = snap.children.mapNotNull { it.key }
                        if (usernames.isEmpty()) return@addOnSuccessListener
                        val members = mutableListOf<Pair<String, String>>()
                        var loaded = 0
                        for (uname in usernames) {
                            db.getReference("online_profiles").child(uname).get()
                                .addOnSuccessListener { pSnap ->
                                    val nick =
                                        pSnap.child("nickname").getValue(String::class.java) ?: ""
                                    val display = nick.ifBlank { uname }
                                    val letter =
                                        display.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                    members.add(Pair(uname, letter))
                                    loaded++
                                    if (loaded == usernames.size) {
                                        _uiState.value = _uiState.value.copy(
                                            groupMembers = members, memberCount = members.size
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    val letter =
                                        uname.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                                    members.add(Pair(uname, letter))
                                    loaded++
                                    if (loaded == usernames.size) {
                                        _uiState.value = _uiState.value.copy(
                                            groupMembers = members, memberCount = members.size
                                        )
                                    }
                                }
                        }
                    }
            }
        }
    }

    private fun listenMessages(groupId: String, myId: String) {
        msgRef = db.getReference("group_chats").child(groupId).child("messages")
        // Auto-wipe removed (requires Firebase index)
        val joinedAt = _uiState.value.myJoinedAt
        val query = if (joinedAt > 0L)
            msgRef!!.orderByChild("timestamp").startAt(joinedAt.toDouble()).limitToLast(50)
        else
            msgRef!!.orderByChild("timestamp").limitToLast(50)
        msgListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val msgs = mutableListOf<GroupChatMsg?>()
                for (child in snapshot.children) {
                    val senderId = child.child("senderId").getValue(String::class.java) ?: continue
                    val ts       = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    val sName    = child.child("senderName").getValue(String::class.java) ?: senderId
                    val letter   = sName.firstOrNull()?.uppercaseChar() ?: '?'
                    val dataType = child.child("dataType").getValue(String::class.java)
                        ?: when (child.child("msgType").getValue(String::class.java) ?: "text") {
                            "file" -> "file"; "voice" -> "voice"; else -> "message"
                        }
                    val isDeleted   = child.child("deleted").getValue(Boolean::class.java) ?: false
                    val deletedBy   = child.child("deletedBy").getValue(String::class.java) ?: ""
                    val isForwarded = child.child("isForwarded").getValue(Boolean::class.java) ?: false
                    val replyToId      = child.child("replyToId").getValue(String::class.java) ?: ""
                    val replyToSender  = child.child("replyToSender").getValue(String::class.java) ?: ""
                    val replyToPreview = child.child("replyToPreview").getValue(String::class.java)
                        ?: child.child("replyToText").getValue(String::class.java) ?: ""
                    val replyToDataType = child.child("replyToDataType").getValue(String::class.java) ?: ""
                    val reactions = mutableMapOf<String, List<String>>()
                    if (!isDeleted) {
                        for (emojiSnap in child.child("reactions").children) {
                            val emoji = emojiSnap.key ?: continue
                            val names = emojiSnap.children.mapNotNull { it.getValue(String::class.java) }.filter { it.isNotBlank() }
                            if (names.isNotEmpty()) reactions[emoji] = names
                        }
                    }
                    val msg = when (dataType) {
                        "voice" -> GroupChatMsg(
                            id = child.key ?: "", senderId = senderId, senderName = sName,
                            senderAvatarLetter = letter, timestamp = ts, isOwn = senderId == myId,
                            dataType = "voice", isDeleted = isDeleted, deletedBy = deletedBy,
                            isForwarded = isForwarded, reactions = reactions,
                            replyToId = replyToId, replyToSender = replyToSender,
                            replyToPreview = replyToPreview, replyToDataType = replyToDataType,
                            voiceData     = child.child("voiceData").getValue(String::class.java) ?: "",
                            voiceDuration = child.child("voiceDuration").getValue(Int::class.java) ?: 0
                        )
                        "file" -> GroupChatMsg(
                            id = child.key ?: "", senderId = senderId, senderName = sName,
                            senderAvatarLetter = letter, timestamp = ts, isOwn = senderId == myId,
                            dataType = "file", isDeleted = isDeleted, deletedBy = deletedBy,
                            isForwarded = isForwarded, reactions = reactions,
                            replyToId = replyToId, replyToSender = replyToSender,
                            replyToPreview = replyToPreview, replyToDataType = replyToDataType,
                            fileData = child.child("fileData").getValue(String::class.java) ?: "",
                            fileName = child.child("fileName").getValue(String::class.java) ?: "",
                            dirName  = child.child("dirName").getValue(String::class.java) ?: "",
                            fileSize = child.child("fileSize").getValue(Long::class.java) ?: 0L
                        )
                        "image" -> {
                            val readBy = mutableMapOf<String, String>()
                            for (rSnap in child.child("read_by").children) {
                                val uname = rSnap.key ?: continue
                                readBy[uname] = uname.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            }
                            GroupChatMsg(
                                id = child.key ?: "", senderId = senderId, senderName = sName,
                                senderAvatarLetter = letter, timestamp = ts, isOwn = senderId == myId,
                                dataType = "image", isDeleted = isDeleted, deletedBy = deletedBy,
                                isForwarded = isForwarded, reactions = reactions, readBy = readBy,
                                replyToId = replyToId, replyToSender = replyToSender,
                                replyToPreview = replyToPreview, replyToDataType = replyToDataType,
                                imageData   = child.child("imageData").getValue(String::class.java) ?: "",
                                imageWidth  = child.child("imageWidth").getValue(Int::class.java) ?: 0,
                                imageHeight = child.child("imageHeight").getValue(Int::class.java) ?: 0,
                                imageUrl    = child.child("imageUrl").getValue(String::class.java) ?: ""
                            )
                        }
                        else -> {
                            val text     = child.child("text").getValue(String::class.java) ?: ""
                            val isEdited = child.child("isEdited").getValue(Boolean::class.java) ?: false
                            val readBy   = mutableMapOf<String, String>()
                            for (rSnap in child.child("read_by").children) {
                                val uname = rSnap.key ?: continue
                                readBy[uname] = uname.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                            }
                            GroupChatMsg(
                                id = child.key ?: "", senderId = senderId, senderName = sName,
                                senderAvatarLetter = letter, text = text, timestamp = ts,
                                isOwn = senderId == myId, dataType = "message",
                                isDeleted = isDeleted, deletedBy = deletedBy,
                                isEdited = isEdited, isForwarded = isForwarded,
                                replyToId = replyToId, replyToSender = replyToSender,
                                replyToPreview = replyToPreview, replyToDataType = replyToDataType,
                                readBy = readBy, reactions = reactions
                            )
                        }
                    }
                    msgs.add(msg)
                }
                val sorted = msgs.filterNotNull().sortedBy { it.timestamp }
                _uiState.value = _uiState.value.copy(
                    messages = sorted, isConnected = true, isLoading = false
                )
                sorted.map { it.senderId }.distinct().forEach { uid ->
                    if (uid.isNotBlank()) {
                        db.getReference("online_profiles").child(uid).get()
                            .addOnSuccessListener { pSnap ->
                                val nick  = pSnap.child("nickname").getValue(String::class.java) ?: ""
                                val uname = pSnap.child("username").getValue(String::class.java) ?: uid
                                val fresh = nick.ifBlank { uname }
                                if (fresh.isNotBlank()) {
                                    val updated = _uiState.value.messages.map { m ->
                                        if (m.senderId == uid) m.copy(
                                            senderName = fresh,
                                            senderAvatarLetter = fresh.firstOrNull()?.uppercaseChar() ?: m.senderAvatarLetter
                                        ) else m
                                    }
                                    _uiState.value = _uiState.value.copy(messages = updated)
                                }
                            }
                    }
                }
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
        if (text.isBlank() || state.groupId.isEmpty() || state.myUserId.isEmpty()) return
        val editing = state.editingMsg
        if (editing != null) {
            db.getReference("group_chats").child(state.groupId).child("messages")
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
                    "voice" -> "\uD83C\uDFA4 Voice message (${reply.voiceDuration}s)"
                    "file"  -> "\uD83D\uDCC1 ${reply.fileName}"
                    "image" -> "\uD83D\uDDBC\uFE0F Image"
                    else    -> reply.text.take(80)
                }
            }
            payload["replyToSender"]   = reply.senderName
            payload["replyToDataType"] = reply.dataType
        }
        db.getReference("group_chats").child(state.groupId).child("messages").push().setValue(payload)
        db.getReference("group_chats").child(state.groupId).child("last_message")
            .updateChildren(mapOf("text" to trimmed, "timestamp" to ServerValue.TIMESTAMP, "senderId" to state.myUserId))
        db.getReference("group_members").child(state.groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    val preview = if (uid == state.myUserId) "You: $trimmed" else "${state.myDisplayName}: $trimmed"
                    db.getReference("user_groups").child(uid).child(state.groupId)
                        .updateChildren(mapOf("lastMsg" to preview, "lastMsgTime" to ServerValue.TIMESTAMP))
                }
            }
        if (reply != null) _uiState.value = _uiState.value.copy(replyingTo = null)
    }

    fun sendImage(base64Jpeg: String, width: Int, height: Int) {
        val state = _uiState.value
        if (base64Jpeg.isBlank() || state.myUserId.isEmpty()) return
        val preview = "\uD83D\uDDBC\uFE0F Image"
        viewModelScope.launch {
            val imgUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                uploadToImgBB(base64Jpeg)
            }
            val payload = mutableMapOf(
                "senderId" to state.myUserId,
                "senderName" to state.myDisplayName,
                "dataType" to "image",
                "imageWidth" to width,
                "imageHeight" to height,
                "text" to preview,
                "timestamp" to ServerValue.TIMESTAMP
            )
            if (imgUrl != null) payload["imageUrl"] = imgUrl else payload["imageData"] = base64Jpeg
            val reply = state.replyingTo
            if (reply != null) {
                payload["replyToId"] = reply.id
                payload["replyToPreview"] = reply.replyToPreview.ifBlank { reply.text.take(60) }
                payload["replyToDataType"] = reply.dataType
                payload["replyToSender"] = reply.senderName
                _uiState.value = _uiState.value.copy(replyingTo = null)
            }
            val msgRef =
                db.getReference("group_chats").child(state.groupId).child("messages").push()
            msgRef.setValue(payload).addOnSuccessListener {
                db.getReference("group_members").child(state.groupId).get()
                    .addOnSuccessListener { snap ->
                        val now = System.currentTimeMillis()
                        val updates = mutableMapOf<String, Any>()
                        for (child in snap.children) {
                            val uid = child.key ?: continue
                            val display = if (uid == state.myUserId) "You: $preview"
                            else "${state.myDisplayName}: $preview"
                            updates["user_groups/$uid/${state.groupId}/lastMsg"] = display
                            updates["user_groups/$uid/${state.groupId}/lastMsgTime"] = now
                        }
                        if (updates.isNotEmpty()) db.reference.updateChildren(updates)
                    }
            }
        }
    }
        private var onlineRef: com.google.firebase.database.DatabaseReference? = null
        private var onlineListener: ValueEventListener? = null

    fun setMyOnlineStatus(online: Boolean) {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        if (!online) {
            db.getReference("online_profiles").child(myId).child("lastOnline")
                .setValue(System.currentTimeMillis())
        }
    }

    fun markAllRead() {
        val state = _uiState.value
        val myId  = state.myUserId; if (myId.isEmpty()) return
        val base  = db.getReference("group_chats").child(state.groupId).child("messages")
        val myLetter = state.myDisplayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        for (msg in state.messages) {
            if (!msg.isOwn && myId !in msg.readBy && !msg.isDeleted) {
                base.child(msg.id).child("read_by").child(myId).setValue(myLetter)
            }
        }
    }

    fun detachOnline() {
        onlineListener?.let { onlineRef?.removeEventListener(it) }
        onlineListener = null
    }

    fun sendVoiceMessage(base64Data: String, durationSec: Int) {
        val state = _uiState.value
        if (base64Data.isBlank() || state.myUserId.isEmpty()) return
        val voicePreview = "\uD83C\uDF99 Voice message"
        val payload = mutableMapOf(
            "senderId"      to state.myUserId,
            "senderName"    to state.myDisplayName,
            "dataType"      to "voice",
            "voiceData"     to base64Data,
            "voiceDuration" to durationSec,
            "text"          to voicePreview,
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
        db.getReference("group_chats").child(state.groupId).child("messages").push().setValue(payload)
        db.getReference("group_chats").child(state.groupId).child("last_message")
            .updateChildren(mapOf("text" to voicePreview, "timestamp" to ServerValue.TIMESTAMP, "senderId" to state.myUserId))
        db.getReference("group_members").child(state.groupId).get()
            .addOnSuccessListener { snap ->
                for (child in snap.children) {
                    val uid = child.key ?: continue
                    val preview = if (uid == state.myUserId) "You: $voicePreview" else "${state.myDisplayName}: $voicePreview"
                    db.getReference("user_groups").child(uid).child(state.groupId)
                        .updateChildren(mapOf("lastMsg" to preview, "lastMsgTime" to ServerValue.TIMESTAMP))
                }
            }
    }

    fun highlightMsg(id: String) {
        _uiState.value = _uiState.value.copy(highlightedMsgId = id)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            _uiState.value = _uiState.value.copy(highlightedMsgId = "")
        }
    }

    fun setFavEmoji(emoji: String) {
        _uiState.value = _uiState.value.copy(favEmoji = emoji)
        viewModelScope.launch { preferencesManager.setFavEmoji(emoji) }
    }

    fun setReplyTo(msg: GroupChatMsg) {
        val preview = when (msg.dataType) {
            "voice" -> "\uD83C\uDFA4 Voice message (${msg.voiceDuration}s)"
            "file"  -> "\uD83D\uDCC1 ${msg.fileName}"
            "image" -> "\uD83D\uDDBC\uFE0F Image"
            else    -> msg.text.take(80)
        }
        _uiState.value = _uiState.value.copy(
            replyingTo = msg.copy(replyToPreview = preview, replyToDataType = msg.dataType),
            editingMsg = null
        )
    }
    fun clearReply()                   { _uiState.value = _uiState.value.copy(replyingTo = null) }
    fun startEdit(msg: GroupChatMsg)   { _uiState.value = _uiState.value.copy(editingMsg = msg, replyingTo = null) }
    fun cancelEdit()                   { _uiState.value = _uiState.value.copy(editingMsg = null) }
    fun canEdit(msg: GroupChatMsg): Boolean = (msg.isOwn || _uiState.value.myRole in listOf("admin","creator")) && !msg.isDeleted
    fun canDeleteForEveryone(msg: GroupChatMsg): Boolean = msg.isOwn || _uiState.value.myRole in listOf("admin","creator")

    fun deleteForMe(msg: GroupChatMsg) {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        db.getReference("group_chats").child(_uiState.value.groupId).child("messages")
            .child(msg.id).child("deleted_for").child(myId).setValue(true)
    }

    fun deleteForEveryone(msg: GroupChatMsg) {
        if (!canDeleteForEveryone(msg)) return
        val state = _uiState.value
        val label = if (msg.isOwn) "Deleted by ${state.myDisplayName}" else "Deleted by Admin"
        val ref = db.getReference("group_chats").child(state.groupId).child("messages").child(msg.id)
        ref.updateChildren(mapOf("deleted" to true, "deletedBy" to label, "text" to label))
        ref.child("reactions").removeValue()
        if (state.replyingTo?.id == msg.id) _uiState.value = _uiState.value.copy(replyingTo = null)
        if (state.editingMsg?.id == msg.id) _uiState.value = _uiState.value.copy(editingMsg = null)
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000L)
            ref.removeValue()
            db.getReference("group_chats").child(state.groupId).child("messages")
                .orderByChild("timestamp").limitToLast(1).get()
                .addOnSuccessListener { snap ->
                    val last    = snap.children.lastOrNull()
                    val newText = last?.child("text")?.getValue(String::class.java) ?: ""
                    val newTs   = last?.child("timestamp")?.getValue(Long::class.java) ?: 0L
                    db.getReference("group_chats").child(state.groupId).child("last_message")
                        .updateChildren(mapOf("text" to newText, "timestamp" to newTs))
                    db.getReference("group_members").child(state.groupId).get()
                        .addOnSuccessListener { mSnap ->
                            for (child in mSnap.children) {
                                val uid = child.key ?: continue
                                db.getReference("user_groups").child(uid).child(state.groupId)
                                    .updateChildren(mapOf("lastMsg" to newText, "lastMsgTime" to newTs))
                            }
                        }
                }
        }
    }

    fun toggleReaction(msg: GroupChatMsg, emoji: String) {
        val state = _uiState.value
        if (state.myUserId.isEmpty() || msg.isDeleted) return
        val ref = db.getReference("group_chats").child(state.groupId)
            .child("messages").child(msg.id).child("reactions").child(emoji).child(state.myUserId)
        val existing = msg.reactions[emoji]?.contains(state.myDisplayName) == true
        if (existing) ref.removeValue() else ref.setValue(state.myDisplayName)
    }

    fun enterSelectMode(msg: GroupChatMsg) {
        _uiState.value = _uiState.value.copy(
            isSelectMode = true, selectedMsgIds = setOf(msg.id),
            replyingTo = null, editingMsg = null)
    }
    fun toggleSelect(msg: GroupChatMsg) {
        val cur = _uiState.value.selectedMsgIds.toMutableSet()
        if (msg.id in cur) cur.remove(msg.id) else cur.add(msg.id)
        _uiState.value = _uiState.value.copy(selectedMsgIds = cur)
    }
    fun exitSelectMode() { _uiState.value = _uiState.value.copy(isSelectMode = false, selectedMsgIds = emptySet()) }
    fun getSelectedMessages(): List<GroupChatMsg> {
        val ids = _uiState.value.selectedMsgIds
        return _uiState.value.messages.filter { it.id in ids }
    }
    fun deleteSelectedForMe() {
        val myId = _uiState.value.myUserId; if (myId.isEmpty()) return
        val base = db.getReference("group_chats").child(_uiState.value.groupId).child("messages")
        for (id in _uiState.value.selectedMsgIds) base.child(id).child("deleted_for").child(myId).setValue(true)
        exitSelectMode()
    }
    fun deleteSelectedForEveryone() {
        val state = _uiState.value
        val base  = db.getReference("group_chats").child(state.groupId).child("messages")
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
            msg != null && (msg.isOwn || state.myRole != "member")
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
                db.getReference("group_chats").child(state.groupId).child("messages").push()
                    .setValue(payload)
                    .addOnSuccessListener {
                        val filePreview = "Sent a file: " + dir.name
                        db.getReference("group_chats").child(state.groupId).child("last_message")
                            .updateChildren(mapOf("text" to filePreview, "timestamp" to ServerValue.TIMESTAMP, "senderId" to state.myUserId))
                        db.getReference("group_members").child(state.groupId).get()
                            .addOnSuccessListener { mSnap ->
                                for (child in mSnap.children) {
                                    val uid = child.key ?: continue
                                    val preview = if (uid == state.myUserId) "You: $filePreview" else "${state.myDisplayName}: $filePreview"
                                    db.getReference("user_groups").child(uid).child(state.groupId)
                                        .updateChildren(mapOf("lastMsg" to preview, "lastMsgTime" to ServerValue.TIMESTAMP))
                                }
                            }
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

    fun downloadAndImport(msg: GroupChatMsg) {
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
        } catch (_: Exception) { null }
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