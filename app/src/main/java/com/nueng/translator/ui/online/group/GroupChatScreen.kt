package com.nueng.translator.ui.online.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.friend.DirectoryPickerSheet
import com.nueng.translator.ui.online.settings.AvatarCircle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.core.graphics.scale

private val GC_QUICK_EMOJIS = listOf("\u2764\uFE0F","\uD83D\uDE02","\uD83D\uDE2E","\uD83D\uDE22","\uD83D\uDE21","\uD83D\uDC4D")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToGroupInfo: (groupId: String) -> Unit = {},
    onNavigateToForward: (text: String) -> Unit = {},
    viewModel: GroupChatViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val listState     = rememberLazyListState()
    val snackbarState = remember { SnackbarHostState() }
    val clipboard     = LocalClipboardManager.current
    var inputText     by remember { mutableStateOf(TextFieldValue("")) }
    var msgToDownload by remember { mutableStateOf<GroupChatMsg?>(null) }

    var actionMsg              by remember { mutableStateOf<GroupChatMsg?>(null) }
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var showAttachMenu         by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream   = context.contentResolver.openInputStream(uri) ?: return@let
                val original = BitmapFactory.decodeStream(stream)
                stream.close()
                val maxSize  = 800
                val scale    = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height, 1f)
                val scaled   = if (scale < 1f) original.scale((original.width * scale).toInt(),
                    (original.height * scale).toInt()
                ) else original
                val out = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
                viewModel.sendImage(b64, scaled.width, scaled.height)
            } catch (_: Exception) {}
        }
    }
    var deleteForEveryone      by remember { mutableStateOf(false) }
    var showEmojiPicker        by remember { mutableStateOf(false) }
    var showSelectDeleteDialog by remember { mutableStateOf(false) }
    var selectDeleteForAll     by remember { mutableStateOf(false) }
    var showTableBuilder       by remember { mutableStateOf(false) }
    var tableCols              by remember { mutableIntStateOf(0) }
    var tableRows              by remember { mutableIntStateOf(0) }
    var tableCells             by remember { mutableStateOf(listOf<List<String>>()) }
    var showHelpDialog         by remember { mutableStateOf(false) }
    var showFunDialog          by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }
    DisposableEffect(groupId) {
        onDispose {
            viewModel.setMyOnlineStatus(false)
            viewModel.detachOnline()
            viewModel.detach()
        }
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
            viewModel.markAllRead()
        }
    }
    LaunchedEffect(uiState.snackMessage) {
        if (uiState.snackMessage.isNotEmpty()) { snackbarState.showSnackbar(uiState.snackMessage); viewModel.clearSnackMessage() }
    }
    LaunchedEffect(uiState.highlightedMsgId) {
        val hid = uiState.highlightedMsgId
        if (hid.isNotBlank()) {
            val idx = uiState.messages.indexOfFirst { it.id == hid }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }
    LaunchedEffect(uiState.editingMsg?.id) {
        val e = uiState.editingMsg
        if (e != null) inputText = TextFieldValue(e.text, TextRange(e.text.length))
    }

    if (uiState.showDirectoryPicker) {
        DirectoryPickerSheet(directories = uiState.userDirectories,
            onSelect = { viewModel.onDirectorySelected(it) }, onDismiss = { viewModel.onDismissDirectoryPicker() })
    }
    uiState.selectedDirectory?.let { dir ->
        if (uiState.showSendConfirm) {
            AlertDialog(onDismissRequest = { viewModel.onDismissSendConfirm() },
                title = { Text("Send Directory?") },
                text  = { Text("Send ${dir.name} to ${uiState.groupName}? This file will be stored in the group's File Storage.") },
                confirmButton = { TextButton(onClick = { viewModel.confirmSendDirectory() }) { Text("Send") } },
                dismissButton = { TextButton(onClick = { viewModel.onDismissSendConfirm() }) { Text("Cancel") } })
        }
    }
    msgToDownload?.let { msg ->
        AlertDialog(onDismissRequest = { msgToDownload = null },
            title = { Text("Download File?") },
            text  = { Text("Download ${msg.dirName}? This will be added to your My Note.") },
            confirmButton = { TextButton(onClick = { viewModel.downloadAndImport(msg); msgToDownload = null }) { Text("Download") } },
            dismissButton = { TextButton(onClick = { msgToDownload = null }) { Text("Cancel") } })
    }
    if (showDeleteDialog && actionMsg != null) {
        val msg = actionMsg!!
        AlertDialog(onDismissRequest = { showDeleteDialog = false; actionMsg = null },
            title = { Text(if (deleteForEveryone) "Delete for everyone?" else "Delete for you?") },
            text  = { Text(if (deleteForEveryone) "This message will be deleted for all." else "This message will be hidden from your view.") },
            confirmButton = { TextButton(onClick = { if (deleteForEveryone) viewModel.deleteForEveryone(msg) else viewModel.deleteForMe(msg); showDeleteDialog = false; actionMsg = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; actionMsg = null }) { Text("Cancel") } })
    }
    if (showSelectDeleteDialog) {
        val count = uiState.selectedMsgIds.size
        AlertDialog(onDismissRequest = { showSelectDeleteDialog = false },
            title = { Text(if (selectDeleteForAll) "Delete for everyone?" else "Delete for you?") },
            text  = { Text(if (selectDeleteForAll) "$count message(s) deleted for all." else "$count message(s) hidden from your view.") },
            confirmButton = { TextButton(onClick = { if (selectDeleteForAll) viewModel.deleteSelectedForEveryone() else viewModel.deleteSelectedForMe(); showSelectDeleteDialog = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showSelectDeleteDialog = false }) { Text("Cancel") } })
    }
    if (showEmojiPicker && actionMsg != null) {
        val msg = actionMsg!!
        AlertDialog(onDismissRequest = { showEmojiPicker = false; actionMsg = null },
            title = { Text("React", fontSize = 15.sp) },
            text = { Column {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    GC_QUICK_EMOJIS.forEach { emoji -> Text(emoji, fontSize = 26.sp, modifier = Modifier.clickable { viewModel.toggleReaction(msg, emoji); showEmojiPicker = false; actionMsg = null }) }
                }
                LazyRow(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    items(GC_EMOJI_LIST.chunked(6)) { col ->
                        Column { col.forEach { emoji -> Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(3.dp).clickable { viewModel.toggleReaction(msg, emoji); showEmojiPicker = false; actionMsg = null }) } }
                    }
                }
            } }, confirmButton = {}, dismissButton = { TextButton(onClick = { showEmojiPicker = false; actionMsg = null }) { Text("Cancel") } })
    }
    if (showTableBuilder && tableCols > 0 && tableRows > 0) {
        AlertDialog(onDismissRequest = { showTableBuilder = false },
            title = { Text("Table ($tableCols x $tableRows)", fontWeight = FontWeight.Bold) },
            text = { Column {
                Text("Row 1 = header.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    val hs = rememberScrollState(); val vs = rememberScrollState()
                    Column(modifier = Modifier.verticalScroll(vs)) {
                        for (r in 0 until tableRows) {
                            Row(modifier = Modifier.horizontalScroll(hs)) {
                                for (c in 0 until tableCols) {
                                    val ih = r == 0
                                    OutlinedTextField(value = tableCells.getOrNull(r)?.getOrNull(c) ?: "",
                                        onValueChange = { nv -> tableCells = tableCells.mapIndexed { ri, row -> row.mapIndexed { ci, cell -> if (ri == r && ci == c) nv else cell } } },
                                        modifier = Modifier.width(90.dp).padding(2.dp), singleLine = true,
                                        placeholder = { Text(if (ih) "H${c+1}" else "r${r}c${c+1}", fontSize = 10.sp) },
                                        label = if (ih) ({ Text("Header", fontSize = 9.sp) }) else null,
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = if (ih) FontWeight.Bold else FontWeight.Normal),
                                        shape = RoundedCornerShape(4.dp))
                                }
                            }
                        }
                    }
                }
            } },
            confirmButton = { TextButton(onClick = {
                val jr = JSONArray(); for (r in 0 until tableRows) { val row = JSONArray(); for (c in 0 until tableCols) row.put(tableCells.getOrNull(r)?.getOrNull(c) ?: ""); jr.put(row) }
                val jo = JSONObject(); jo.put("cols", tableCols); jo.put("rows", jr)
                viewModel.sendMessage("[TABLE]$jo"); showTableBuilder = false; inputText = TextFieldValue("")
            }) { Text("Send Table", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { showTableBuilder = false }) { Text("Cancel") } })
    }
    if (showHelpDialog) {
        AlertDialog(onDismissRequest = { showHelpDialog = false },
            title = { Text("NuengChat Help", fontWeight = FontWeight.Bold) },
            text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GCHelpSection("Chat Features", "Tap bubble -> action bar (Reply, Edit, Delete, Emoji, Copy, Forward, Select).\nSwipe left/right -> quick reply.\nAttach -> send .ntf file.")
                GCHelpSection("Print Commands", "  table(col,row) -> visual table\n  loop(n,msg) -> repeat (1-20)\n  fun() -> commands list\n  help() -> this guide")
                GCHelpSection("Auto-List", "  1.  / -  / *  / a.  / A.  + space -> auto-list\nEnter on empty item -> cancel.")
            } }, confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it!") } })
    }
    if (showFunDialog) {
        AlertDialog(onDismissRequest = { showFunDialog = false },
            title = { Text("Print Commands", fontWeight = FontWeight.Bold) },
            text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GCHelpSection("table(col,row) or table(n)", "table(3,4) -> 3 cols, 4 rows\ntable(3) -> 3x3. Max 8 cols, 20 rows.")
                GCHelpSection("loop(n, msg) or repeat(n, msg)", "Sends msg n times (1-20). Out of range -> plain text.\nloop(3, Hello!) -> 3 lines")
                GCHelpSection("Auto-List", "  1.  -  *  a.  A.  + space -> list\nEnter empty item -> cancel.")
                GCHelpSection("Meta", "  help() -> guide\n  fun() -> this list")
            } }, confirmButton = { TextButton(onClick = { showFunDialog = false }) { Text("Got it!") } })
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectMode) {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { viewModel.exitSelectMode() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel") } },
                    title = { Text("${uiState.selectedMsgIds.size} selected", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = { val j = viewModel.getSelectedMessages().joinToString("\n") { "${it.senderName} : ${it.text}" }; clipboard.setText(AnnotatedString(j)); viewModel.exitSelectMode() }) { Icon(Icons.Default.ContentCopy, "Copy") }
                        IconButton(onClick = { val j = viewModel.getSelectedMessages().joinToString("\n") { "${it.senderName} : ${it.text}" }; onNavigateToForward(j); viewModel.exitSelectMode() }) { Icon(
                            Icons.AutoMirrored.Filled.Forward, "Forward") }
                        IconButton(onClick = { selectDeleteForAll = viewModel.canDeleteSelectedForEveryone(); showSelectDeleteDialog = true }, enabled = uiState.selectedMsgIds.isNotEmpty()) {
                            Icon(Icons.Default.Delete, "Delete", tint = if (uiState.selectedMsgIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(letter = uiState.groupAvatarLetter, sizeDp = 34)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(uiState.groupName.ifBlank { "..." }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = if (uiState.isConnected) Icons.Default.Cloud else Icons.Default.CloudOff, contentDescription = null,
                                        tint = if (uiState.isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350), modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (uiState.isConnected) "Online" else "Offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = { IconButton(onClick = { onNavigateToGroupInfo(groupId) }) { Icon(Icons.Default.MoreVert, "Group Info", tint = MaterialTheme.colorScheme.primary) } }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isUploading) { LinearProgressIndicator(progress = { uiState.uploadProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary) }
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            } else if (uiState.messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)) {
                        Text("No messages yet", fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.myJoinedAt > 0L) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "You joined on " + SimpleDateFormat(
                                    "MMM dd, yyyy", Locale.getDefault()
                                ).format(Date(uiState.myJoinedAt)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Messages before you joined are not shown.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            } else {
                // Compute last-read message per member (Instagram style) — outside LazyColumn
                // Include ALL members (including self — sender always read their own message)
                val lastReadPerMember = remember(uiState.messages, uiState.myUserId) {
                    val result = mutableMapOf<String, String>() // username -> msgId
                    for (m in uiState.messages) {
                        // Everyone who read this message
                        for (uname in m.readBy.keys) {
                            result[uname] = m.id
                        }
                        // Sender always counts as having read their own message
                        if (m.senderId.isNotBlank()) {
                            result[m.senderId] = m.id
                        }
                    }
                    result
                }
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(items = uiState.messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { msg ->
                        // Who has THIS message as their last-read? (include self)
                        val readersHere = uiState.groupMembers.filter { (uname, _) ->
                            lastReadPerMember[uname] == msg.id
                        }
                        when (msg.dataType) {
                            "voice" -> {
                                GCVoiceFileBubbleWrapper(
                                    msg=msg, isHighlighted=actionMsg?.id==msg.id,
                                    isReplied=uiState.highlightedMsgId==msg.id,
                                    isSelected=msg.id in uiState.selectedMsgIds, isSelectMode=uiState.isSelectMode,
                                    onTap={ if(uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg=if(actionMsg?.id==msg.id)null else msg },
                                    onReply={ viewModel.setReplyTo(msg); actionMsg=null },
                                    onDelete={ actionMsg=msg; deleteForEveryone=false; showDeleteDialog=true },
                                    onDeleteAll={ actionMsg=msg; deleteForEveryone=true; showDeleteDialog=true },
                                    onForward={ onNavigateToForward("__VOICE__${msg.id}"); actionMsg=null },
                                    onSelect={ viewModel.enterSelectMode(msg); actionMsg=null },
                                    onEmoji={ actionMsg=msg; showEmojiPicker=true },
                                    canDeleteAll=viewModel.canDeleteForEveryone(msg)
                                ) {
                                    com.nueng.translator.ui.online.voice.VoiceChatBubble(
                                        msgId=msg.id, voiceData=msg.voiceData, voiceDuration=msg.voiceDuration,
                                        senderName=msg.senderName, senderLetter=msg.senderAvatarLetter,
                                        senderId=msg.senderId,
                                        isOwn=msg.isOwn, timestamp=msg.timestamp,
                                        isDeleted=msg.isDeleted, deletedBy=msg.deletedBy,
                                        reactions=msg.reactions,
                                        onReactionTap   = { emoji -> viewModel.toggleReaction(msg, emoji) },
                                        replyToSender   = msg.replyToSender,
                                        replyToPreview  = msg.replyToPreview,
                                        replyToDataType = msg.replyToDataType,
                                        replyToId       = msg.replyToId,
                                        onReplyTap      = { id -> viewModel.highlightMsg(id) }
                                    )
                                }
                            }
                            "image" -> {
                                GCVoiceFileBubbleWrapper(
                                    msg=msg, isHighlighted=actionMsg?.id==msg.id,
                                    isReplied=uiState.highlightedMsgId==msg.id,
                                    isSelected=msg.id in uiState.selectedMsgIds,
                                    isSelectMode=uiState.isSelectMode,
                                    onTap={if(uiState.isSelectMode)viewModel.toggleSelect(msg) else actionMsg=if(actionMsg?.id==msg.id)null else msg},
                                    onReply={viewModel.setReplyTo(msg);actionMsg=null},
                                    onDelete={actionMsg=msg;deleteForEveryone=false;showDeleteDialog=true},
                                    onDeleteAll={actionMsg=msg;deleteForEveryone=true;showDeleteDialog=true},
                                    onForward={ onNavigateToForward("__IMAGE__${msg.id}"); actionMsg=null },
                                    onSelect={viewModel.enterSelectMode(msg);actionMsg=null},
                                    onEmoji={actionMsg=msg;showEmojiPicker=true},
                                    canDeleteAll=viewModel.canDeleteForEveryone(msg)
                                ) {
                                    com.nueng.translator.ui.online.friend.ImageChatBubble(
                                        imageData=msg.imageData, isOwn=msg.isOwn,
                                        senderName=msg.senderName, senderLetter=msg.senderAvatarLetter,
                                        senderId=msg.senderId, timestamp=msg.timestamp,
                                        isDeleted=msg.isDeleted, deletedBy=msg.deletedBy
                                    )
                                }
                            }
                            "file" -> {
                                GCVoiceFileBubbleWrapper(
                                    msg=msg, isHighlighted=actionMsg?.id==msg.id,
                                    isReplied=uiState.highlightedMsgId==msg.id,
                                    isSelected=msg.id in uiState.selectedMsgIds, isSelectMode=uiState.isSelectMode,
                                    onTap={ if(uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg=if(actionMsg?.id==msg.id)null else msg },
                                    onReply={ viewModel.setReplyTo(msg); actionMsg=null },
                                    onDelete={ actionMsg=msg; deleteForEveryone=false; showDeleteDialog=true },
                                    onDeleteAll={ actionMsg=msg; deleteForEveryone=true; showDeleteDialog=true },
                                    onForward={ onNavigateToForward("__FILE__${msg.id}"); actionMsg=null },
                                    onSelect={ viewModel.enterSelectMode(msg); actionMsg=null },
                                    onEmoji={ actionMsg=msg; showEmojiPicker=true },
                                    canDeleteAll=viewModel.canDeleteForEveryone(msg)
                                ) {
                                    GCFileBubble(msg=msg, onDownload={ viewModel.downloadAndImport(msg) })
                                }
                            }
                            else -> {
                                GCTextBubble(
                                    msg             = msg,
                                    isHighlighted   = actionMsg?.id == msg.id || uiState.editingMsg?.id == msg.id || uiState.highlightedMsgId == msg.id,
                                    isSelected      = msg.id in uiState.selectedMsgIds,
                                    isSelectMode    = uiState.isSelectMode,
                                    canEdit         = viewModel.canEdit(msg),
                                    canDeleteForAll = viewModel.canDeleteForEveryone(msg),
                                    readersHere     = readersHere,
                                    onTap           = { if (uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg = if (actionMsg?.id == msg.id) null else msg },
                                    onSwipeReply    = { if (!uiState.isSelectMode) viewModel.setReplyTo(msg) },
                                    onReply         = { viewModel.setReplyTo(msg); actionMsg = null },
                                    onEdit          = { viewModel.startEdit(msg); actionMsg = null },
                                    onDeleteForMe   = { deleteForEveryone = false; showDeleteDialog = true },
                                    onDeleteForAll  = { deleteForEveryone = true; showDeleteDialog = true },
                                    onEmoji         = { showEmojiPicker = true },
                                    onCopy          = { clipboard.setText(AnnotatedString(msg.text)); actionMsg = null },
                                    onForward       = { if (!msg.isDeleted) { onNavigateToForward(msg.text); actionMsg = null } },
                                    onSelect        = { viewModel.enterSelectMode(msg); actionMsg = null },
                                    onReactionTap   = { emoji -> viewModel.toggleReaction(msg, emoji) },
                                    onReplyTap      = { id -> viewModel.highlightMsg(id) },
                                    isReplied       = uiState.highlightedMsgId == msg.id
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            // Reply bar
            AnimatedVisibility(visible = uiState.replyingTo != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                uiState.replyingTo?.let { reply ->
                    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Reply, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reply.senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            val replyPreview = when (reply.dataType) {
                                "voice" -> "🎤 Voice message (${reply.voiceDuration}s)"
                                "file"  -> "📁 ${reply.fileName} (${reply.dirName})"
                                else    -> reply.text
                            }
                            Text(replyPreview, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.clearReply() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            // Edit banner
            AnimatedVisibility(visible = uiState.editingMsg != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editing message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.cancelEdit(); inputText = TextFieldValue("") }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary) }
                }
            }

            // Input bar
            if (!uiState.isSelectMode) {
                val gcCtx = androidx.compose.ui.platform.LocalContext.current
                val gcRec = remember { com.nueng.translator.util.VoiceRecorder(gcCtx) }
                var gcIsRec  by remember { mutableStateOf(false) }
                var gcVData  by remember { mutableStateOf<String?>(null) }
                var gcVSec   by remember { mutableIntStateOf(0) }
                var gcPPlay  by remember { mutableStateOf(false) }
                var gcPMp    by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
                var gcEmoji  by remember { mutableStateOf(false) }
                var gcSelFav by remember { mutableStateOf(false) }
                val gcKb = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
                val gcScp = rememberCoroutineScope()
                var gcPerm by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(gcCtx, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) }
                val gcAP = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { gcPerm = it }
                when {
                    gcIsRec -> GCVoiceRecordingBar(
                        onRelease = { gcIsRec=false; val r=gcRec.stopRecording(); if(r!=null&&r.first.isNotBlank()&&r.second>=1){gcVData=r.first;gcVSec=r.second} },
                        onCancel  = { gcIsRec=false; gcRec.cancelRecording() }
                    )
                    gcVData != null -> GCVoicePreviewBar(
                        durationSec=gcVSec, isPlaying=gcPPlay,
                        onPlay = { if(gcPPlay){gcPMp?.pause();gcPPlay=false} else { val f=java.io.File(gcCtx.cacheDir,"gc_prev.3gp"); try{ f.writeBytes(Base64.decode(gcVData!!,Base64.DEFAULT)); val mp=android.media.MediaPlayer(); mp.setDataSource(f.absolutePath); mp.prepare(); mp.setOnCompletionListener{gcPPlay=false}; mp.start(); gcPMp=mp; gcPPlay=true }catch(_:Exception){gcPPlay=false} } },
                        onDiscard = { gcPMp?.stop();gcPMp?.release();gcPMp=null;gcPPlay=false;gcVData=null;gcVSec=0 },
                        onSend    = { gcPMp?.stop();gcPMp?.release();gcPMp=null;gcPPlay=false; viewModel.sendVoiceMessage(gcVData!!,gcVSec); gcVData=null;gcVSec=0 }
                    )
                    else -> Column {
                        AnimatedVisibility(visible=gcEmoji, enter=slideInVertically{it}+fadeIn(), exit=slideOutVertically{it}+fadeOut()) {
                            Column(modifier=Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState()).padding(4.dp)) {
                                if(gcSelFav) Text("Tap to set as favourite", fontSize=11.sp, color=MaterialTheme.colorScheme.primary, modifier=Modifier.padding(start=8.dp,bottom=4.dp))
                                GC_EMOJI_LIST.chunked(8).forEach { row ->
                                    Row(modifier=Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceEvenly) {
                                        row.forEach { emoji ->
                                            Box(modifier=Modifier.size(40.dp).combinedClickable(
                                                onClick={ if(gcSelFav){viewModel.setFavEmoji(emoji);gcSelFav=false;gcEmoji=false} else inputText=TextFieldValue(inputText.text+emoji,TextRange(inputText.text.length+emoji.length)) },
                                                onLongClick={ viewModel.setFavEmoji(emoji);gcSelFav=false;gcEmoji=false }
                                            ), contentAlignment=Alignment.Center){ Text(emoji,fontSize=22.sp) }
                                        }
                                    }
                                }
                            }
                        }
                        Row(modifier=Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal=8.dp,vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
                            Box {
                                IconButton(onClick={showAttachMenu=!showAttachMenu}, enabled=!uiState.isUploading, modifier=Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AddCircleOutline,"Attach",tint=if(!uiState.isUploading)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded=showAttachMenu,
                                    onDismissRequest={showAttachMenu=false}
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text={Text("File (.ntf)")},
                                        onClick={showAttachMenu=false; viewModel.onAttachTapped()}
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text={Text("Image")},
                                        onClick={showAttachMenu=false; imageLauncher.launch("image/*")}
                                    )
                                }
                            }
                            OutlinedTextField(value=inputText, onValueChange={nv->val ar=gcAutoList(inputText,nv);inputText=if(ar.text!=nv.text)ar else nv}, modifier=Modifier.weight(1f), placeholder={Text(if(uiState.editingMsg!=null)"Edit..." else "Message...",fontSize=14.sp)}, singleLine=false, maxLines=5, shape=RoundedCornerShape(16.dp))
                            IconButton(onClick={if(gcEmoji){gcEmoji=false;gcSelFav=false;gcScp.launch{kotlinx.coroutines.delay(100);gcKb?.show()}}else{gcEmoji=true;gcKb?.hide()}}, modifier=Modifier.size(36.dp)){ Icon(if(gcEmoji)Icons.Default.Keyboard else Icons.Default.EmojiEmotions,null,tint=if(gcEmoji)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp)) }
                            Box(modifier=Modifier.size(36.dp).clip(CircleShape).combinedClickable(onClick={viewModel.sendMessage(uiState.favEmoji)},onLongClick={gcSelFav=true;gcEmoji=true;gcKb?.hide()}),contentAlignment=Alignment.Center){ Text(uiState.favEmoji,fontSize=20.sp) }
                            if(inputText.text.isBlank()&&uiState.editingMsg==null){
                                Box(modifier=Modifier.size(36.dp).clip(CircleShape).pointerInput(Unit){ detectTapGestures(onPress={_->
                                    if(!gcPerm){gcAP.launch(android.Manifest.permission.RECORD_AUDIO);return@detectTapGestures}
                                    try{gcRec.startRecording();}catch(_:Exception){return@detectTapGestures}
                                    gcIsRec=true;val t=System.currentTimeMillis();tryAwaitRelease();val h=System.currentTimeMillis()-t;gcIsRec=false
                                    if(h<500L){gcRec.cancelRecording()}else{val r=gcRec.stopRecording();if(r!=null&&r.first.isNotBlank()){gcVData=r.first;gcVSec=r.second}}
                                })},contentAlignment=Alignment.Center){Icon(Icons.Default.Mic,"Hold to record",tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))}
                            } else {
                                Surface(modifier=Modifier.size(40.dp).clip(CircleShape),color=if(inputText.text.isNotBlank()||uiState.editingMsg!=null)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    onClick={
                                        val raw=inputText.text.trim()
                                        val tm2=Regex("^[Tt][Aa][Bb][Ll][Ee][(](\\d+),(\\d+)[)]$").matchEntire(raw)
                                        val tm1=Regex("^[Tt][Aa][Bb][Ll][Ee][(](\\d+)[)]$").matchEntire(raw)
                                        if(tm2!=null||tm1!=null){val c:Int;val r:Int;if(tm2!=null){c=tm2.groupValues[1].toIntOrNull()?.coerceIn(1,8)?:2;r=tm2.groupValues[2].toIntOrNull()?.coerceIn(1,20)?:2}else{val n=tm1!!.groupValues[1].toIntOrNull()?.coerceIn(1,8)?:2;c=n;r=n};tableCols=c;tableRows=r;tableCells=List(r){List(c){""}};showTableBuilder=true;inputText=TextFieldValue("")}
                                        else if(raw.equals("help()",ignoreCase=true)){showHelpDialog=true;inputText=TextFieldValue("")}
                                        else if(raw.equals("fun()",ignoreCase=true)){showFunDialog=true;inputText=TextFieldValue("")}
                                        else{val lm=Regex("^(?:loop|repeat)[(]\\s*(\\d+)\\s*,\\s*(.+)[)]$",RegexOption.IGNORE_CASE).matchEntire(raw);if(lm!=null){val times=lm.groupValues[1].toIntOrNull()?:0;val msg=lm.groupValues[2].trim();if(times in 1..20)viewModel.sendMessage((1..times).joinToString("\n"){msg})else viewModel.sendMessage(raw);inputText=TextFieldValue("")}else if(raw.isNotBlank()){viewModel.sendMessage(raw);inputText=TextFieldValue("")}}
                                    }){
                                    Box(contentAlignment=Alignment.Center){Icon(if(uiState.editingMsg!=null)Icons.Default.Edit else Icons.AutoMirrored.Filled.Send,"Send",tint=if(inputText.text.isNotBlank()||uiState.editingMsg!=null)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(20.dp))}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun gcAutoList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val nt = new.text; val ot = old.text
    if (!nt.endsWith("\n") || nt.length <= ot.length) return new
    val tbf = nt.dropLast(1); val ll = tbf.substringAfterLast("\n", tbf)
    val di = ll.indexOf(". ")
    if (di > 0) {
        val mn = ll.substring(0, di)
        if (mn.isNotEmpty() && mn.all { it.isDigit() }) {
            val ct = ll.substring(di+2)
            if (ct.isEmpty()) { val r=tbf.dropLast(ll.length); return TextFieldValue(r,TextRange(r.length)) }
            val nx=(mn.toIntOrNull()?:0)+1; val r= "$nt$nx. "; return TextFieldValue(r,TextRange(r.length))
        }
    }
    if (ll=="- "||ll=="* ") { val r=tbf.dropLast(ll.length); return TextFieldValue(r,TextRange(r.length)) }
    if (ll.startsWith("- ")&&ll.length>2) { val r= "$nt- "; return TextFieldValue(r,TextRange(r.length)) }
    if (ll.startsWith("* ")&&ll.length>2) { val r= "$nt* "; return TextFieldValue(r,TextRange(r.length)) }
    val le=Regex("""^[a-zA-Z]\. $"""); val la=Regex("""^([a-zA-Z])\. (.+)$""")
    if (le.matches(ll)) { val r=tbf.dropLast(ll.length); return TextFieldValue(r,TextRange(r.length)) }
    val lm=la.matchEntire(ll)
    if (lm!=null) { val ch=lm.groupValues[1].first(); val nx= when (ch) {
        'z' -> 'z'
        'Z' -> 'Z'
        else -> ch+1
    }
        val r= "$nt$nx. "; return TextFieldValue(r,TextRange(r.length)) }
    return TextFieldValue(nt,TextRange(nt.length))
}

private fun gcParseTable(jt: String): Pair<Int,List<List<String>>>? {
    return try {
        val j=JSONObject(jt); val c=j.getInt("cols"); val jr=j.getJSONArray("rows")
        val rows=(0 until jr.length()).map { r -> val row=jr.getJSONArray(r); (0 until c).map { ci -> row.optString(ci,"") } }
        if (c>0&&rows.isNotEmpty()) Pair(c,rows) else null
    } catch (_: Exception) { null }
}

@Composable
private fun GCTableBubble(jt: String, isOwn: Boolean) {
    val p=remember(jt){gcParseTable(jt)}
    if(p==null){Text(jt,fontSize=12.sp,modifier=Modifier.padding(8.dp));return}
    val(cols,rows)=p
    val hBg=if(isOwn)MaterialTheme.colorScheme.primary.copy(0.9f)else MaterialTheme.colorScheme.primary
    val hFg=MaterialTheme.colorScheme.onPrimary
    val cBg=if(isOwn)MaterialTheme.colorScheme.primary.copy(0.7f)else MaterialTheme.colorScheme.surfaceVariant
    val cBgA=if(isOwn)MaterialTheme.colorScheme.primary.copy(0.6f)else MaterialTheme.colorScheme.surface
    val cFg=if(isOwn)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bdr=if(isOwn)MaterialTheme.colorScheme.onPrimary.copy(0.3f)else MaterialTheme.colorScheme.outline.copy(0.4f)
    val hs=rememberScrollState()
    Column(modifier=Modifier.horizontalScroll(hs).padding(4.dp)){
        for(r in 0 until rows.size){val row=rows[r];val ih=r==0
            Row{for(c in 0 until cols){val cell=row.getOrElse(c){""}
                Box(modifier=Modifier.border(0.5.dp,bdr).background(when{ih->hBg;r%2==1->cBg;else->cBgA}).padding(horizontal=8.dp,vertical=5.dp).widthIn(min=48.dp,max=120.dp)){
                    Text(cell,fontSize=12.sp,fontWeight=if(ih)FontWeight.Bold else FontWeight.Normal,color=if(ih)hFg else cFg,maxLines=3,overflow=TextOverflow.Ellipsis)
                }
            }}
        }
    }
}

@Composable private fun GCHelpSection(title: String, body: String) {
    Column(modifier=Modifier.padding(bottom=12.dp)){
        Text(title,fontSize=13.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(body,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,lineHeight=18.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class,ExperimentalLayoutApi::class)
@Composable
private fun GCTextBubble(
    msg: GroupChatMsg, isHighlighted: Boolean, isSelected: Boolean, isSelectMode: Boolean,
    canEdit: Boolean, canDeleteForAll: Boolean,
    readersHere: List<Pair<String,String>> = emptyList(), // members whose last-read = this msg
    onTap:()->Unit, onSwipeReply:()->Unit, onReply:()->Unit, onEdit:()->Unit,
    onDeleteForMe:()->Unit, onDeleteForAll:()->Unit, onEmoji:()->Unit, onCopy:()->Unit,
    onForward:()->Unit, onSelect:()->Unit, onReactionTap:(String)->Unit,
    onReplyTap:(String)->Unit = {},
    isReplied: Boolean = false
) {
    val tf=SimpleDateFormat("HH:mm",Locale.getDefault()); val scope=rememberCoroutineScope()
    val offX=remember{Animatable(0f)}; var tapped by remember{mutableStateOf(false)}
    LaunchedEffect(tapped){if(tapped){kotlinx.coroutines.delay(160);tapped=false}}
    val bg=when{msg.isDeleted->MaterialTheme.colorScheme.surfaceVariant.copy(0.4f);tapped->if(msg.isOwn)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(0.18f);msg.isOwn->MaterialTheme.colorScheme.primary.copy(0.85f);else->MaterialTheme.colorScheme.surfaceVariant}
    val sh=RoundedCornerShape(topStart=if(msg.replyToId.isNotBlank()&&!msg.isDeleted)4.dp else 16.dp,topEnd=if(msg.replyToId.isNotBlank()&&!msg.isDeleted)4.dp else 16.dp,bottomStart=if(msg.isOwn)16.dp else 4.dp,bottomEnd=if(msg.isOwn)4.dp else 16.dp)
    val goldColor = Color(0xFFFFD700)
    val gcPulse = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            repeat(4) {
                gcPulse.animateTo(1f, tween(250))
                gcPulse.animateTo(0f, tween(250))
            }
        } else { gcPulse.snapTo(0f) }
    }
    Column(modifier=Modifier.fillMaxWidth()){
        if(isHighlighted&&!isReplied&&!msg.isDeleted&&!isSelectMode){
            Row(modifier=Modifier.then(if(msg.isOwn)Modifier.align(Alignment.End)else Modifier.align(Alignment.Start)).padding(horizontal=8.dp,vertical=2.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(24.dp)).padding(horizontal=2.dp,vertical=2.dp),horizontalArrangement=Arrangement.spacedBy(0.dp),verticalAlignment=Alignment.CenterVertically){
                GCActionIcon(Icons.AutoMirrored.Filled.Reply,"Reply",onClick=onReply)
                if(canEdit) GCActionIcon(Icons.Default.Edit,"Edit",onClick=onEdit)
                GCActionIcon(Icons.Default.Delete,"Delete",tint=MaterialTheme.colorScheme.error,onClick={if(canDeleteForAll)onDeleteForAll()else onDeleteForMe()})
                GCActionIcon(Icons.Default.EmojiEmotions,"Emoji",onClick=onEmoji)
                GCActionIcon(Icons.Default.ContentCopy,"Copy",onClick=onCopy)
                GCActionIcon(Icons.AutoMirrored.Filled.Forward,"Forward",onClick={if(!msg.isDeleted)onForward()})
                GCActionIcon(Icons.Default.CheckBox,"Select",onClick=onClick@{onSelect();return@onClick})
            }
        }
        Row(modifier=Modifier.fillMaxWidth().padding(vertical=2.dp).then(when{isSelected->Modifier.background(MaterialTheme.colorScheme.primary.copy(0.18f));isHighlighted->Modifier.background(MaterialTheme.colorScheme.primary.copy(0.07f),RoundedCornerShape(8.dp));else->Modifier}),
            horizontalArrangement=if(msg.isOwn)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Bottom){
            if(!msg.isOwn){Box{AvatarCircle(letter=msg.senderAvatarLetter,sizeDp=28,username=msg.senderId);if(isSelectMode)Icon(if(isSelected)Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(14.dp).align(Alignment.BottomEnd))};Spacer(Modifier.width(6.dp))}
            Column(horizontalAlignment=if(msg.isOwn)Alignment.End else Alignment.Start){
                if(!msg.isOwn) Text(msg.senderName,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=4.dp,bottom=2.dp))
                if(msg.isForwarded&&!msg.isDeleted) Text("Forwarded",fontSize=10.sp,fontStyle=FontStyle.Italic,color=MaterialTheme.colorScheme.primary.copy(0.7f),modifier=Modifier.padding(start=4.dp,bottom=2.dp))
                if(msg.replyToId.isNotBlank()&&!msg.isDeleted){
                    Surface(modifier=Modifier.widthIn(max=260.dp).padding(bottom=2.dp).clickable{ onReplyTap(msg.replyToId) },color=if(msg.isOwn)MaterialTheme.colorScheme.primary.copy(0.55f)else MaterialTheme.colorScheme.surfaceVariant.copy(0.7f),shape=RoundedCornerShape(topStart=12.dp,topEnd=12.dp,bottomStart=4.dp,bottomEnd=4.dp)){
                        Column(modifier=Modifier.padding(horizontal=10.dp,vertical=6.dp)){
                            Text(msg.replyToSender,fontSize=11.sp,fontWeight=FontWeight.Bold,color=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            Text(msg.replyToPreview,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis,color=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary.copy(0.7f)else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Box(modifier=Modifier.then(if(msg.text.startsWith("[TABLE]"))Modifier.wrapContentWidth()else Modifier.widthIn(max=260.dp)).offset{IntOffset(offX.value.roundToInt(),0)}.pointerInput(msg.id){detectHorizontalDragGestures(onDragEnd={scope.launch{if(abs(offX.value)>60f)onSwipeReply();offX.animateTo(0f,spring(dampingRatio=Spring.DampingRatioMediumBouncy))}},onHorizontalDrag={_,d->scope.launch{offX.snapTo((offX.value+d).coerceIn(-80f,80f))}})}){
                    Surface(modifier=Modifier.then(if(msg.text.startsWith("[TABLE]"))Modifier.wrapContentWidth()else Modifier.widthIn(max=260.dp)).then(if(isReplied&&gcPulse.value>0f)Modifier.border(2.dp,goldColor.copy(alpha=gcPulse.value),sh)else Modifier).clickable{tapped=true;onTap()},color=bg,shape=sh){
                        if(msg.isDeleted) Text(msg.deletedBy.ifBlank{"Deleted"},fontSize=13.sp,fontStyle=FontStyle.Italic,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp))
                        else if(msg.text.startsWith("[TABLE]")) GCTableBubble(msg.text.removePrefix("[TABLE]"),msg.isOwn)
                        else Text(msg.text,fontSize=14.sp,color=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp))
                    }
                }
                if(!msg.isDeleted&&msg.reactions.isNotEmpty()){FlowRow(modifier=Modifier.padding(top=2.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){msg.reactions.forEach{(emoji,names)->Surface(modifier=Modifier.clickable{onReactionTap(emoji)},shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceVariant){Row(modifier=Modifier.padding(horizontal=6.dp,vertical=2.dp),verticalAlignment=Alignment.CenterVertically){Text(emoji,fontSize=14.sp);Spacer(Modifier.width(2.dp));Text(names.size.toString(),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}}
                Row(verticalAlignment=Alignment.CenterVertically){
                    if(msg.isEdited&&!msg.isDeleted) Text("edited",fontSize=9.sp,fontStyle=FontStyle.Italic,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),modifier=Modifier.padding(start=4.dp,end=2.dp))
                    if(msg.timestamp>0) Text(tf.format(Date(msg.timestamp)),fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),modifier=Modifier.padding(horizontal=4.dp,vertical=2.dp))
                    // Own message: gray tick = sent, aqua double = read by anyone
                    if (msg.isOwn && !msg.isDeleted) {
                        val anyRead = msg.readBy.keys.any { it != msg.senderId }
                        Text(
                            text     = if (anyRead) "\u2713\u2713" else "\u2713",
                            fontSize = 10.sp,
                            color    = if (anyRead) Color(0xFF00BCD4)
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                // Instagram-style: avatars always right-aligned, one step below timestamp
                if (readersHere.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 1.dp, end = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (readersHere.size > 6) {
                            Text(
                                text     = "+${readersHere.size - 6}",
                                fontSize = 8.sp,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        readersHere.takeLast(6).forEach { (uname, letter) ->
                            val avatarLetter = letter.firstOrNull() ?: '?'
                            AvatarCircle(
                                letter   = avatarLetter,
                                sizeDp   = 18,
                                username = uname,
                                modifier = Modifier.padding(start = 3.dp)
                            )
                        }
                    }
                }
            }
            if(msg.isOwn){if(isSelectMode)Icon(if(isSelected)Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(16.dp).padding(start=2.dp));Spacer(Modifier.width(6.dp))}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GCVoiceFileBubbleWrapper(
    msg: GroupChatMsg, isHighlighted: Boolean, isReplied: Boolean = false,
    isSelected: Boolean, isSelectMode: Boolean,
    onTap: () -> Unit, onReply: () -> Unit, onDelete: () -> Unit, onDeleteAll: () -> Unit,
    onForward: () -> Unit, onSelect: () -> Unit, onEmoji: () -> Unit, canDeleteAll: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offX  = remember { Animatable(0f) }
    val goldColor = Color(0xFFFFD700)
    val wrapPulse = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isReplied) {
        if (isReplied) {
            repeat(4) {
                wrapPulse.animateTo(1f, tween(250))
                wrapPulse.animateTo(0f, tween(250))
            }
        } else { wrapPulse.snapTo(0f) }
    }
    val bg = when {
        isSelected    -> MaterialTheme.colorScheme.primary.copy(0.15f)
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(0.07f)
        else          -> Color.Transparent
    }
    Column(modifier = Modifier.fillMaxWidth().background(bg)) {
        if (isHighlighted && !isReplied && !isSelectMode) {
            Row(modifier = Modifier.then(if(msg.isOwn)Modifier.align(Alignment.End)else Modifier.align(Alignment.Start))
                .padding(horizontal=8.dp,vertical=2.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(24.dp)).padding(horizontal=2.dp,vertical=2.dp),
                horizontalArrangement=Arrangement.spacedBy(0.dp), verticalAlignment=Alignment.CenterVertically) {
                GCActionIcon(Icons.AutoMirrored.Filled.Reply,"Reply",onClick=onReply)
                GCActionIcon(Icons.Default.EmojiEmotions,"Emoji",onClick=onEmoji)
                GCActionIcon(Icons.AutoMirrored.Filled.Forward,"Forward",onClick=onForward)
                if(canDeleteAll) GCActionIcon(Icons.Default.Delete,"Delete",tint=MaterialTheme.colorScheme.error,onClick=onDeleteAll)
                else GCActionIcon(Icons.Default.Delete,"Delete for me",onClick=onDelete)
                GCActionIcon(Icons.Default.CheckBox,"Select",onClick=onSelect)
            }
        }
        Box(modifier=Modifier.offset{IntOffset(offX.value.roundToInt(),0)}
            .then(if (isReplied && wrapPulse.value > 0f) Modifier.border(2.dp, goldColor.copy(alpha = wrapPulse.value), RoundedCornerShape(12.dp)) else Modifier)
            .pointerInput(msg.id){detectHorizontalDragGestures(onDragEnd={scope.launch{if(abs(offX.value)>60f)onReply();offX.animateTo(0f,spring(dampingRatio=Spring.DampingRatioMediumBouncy))}},onHorizontalDrag={_,d->scope.launch{offX.snapTo((offX.value+d).coerceIn(-80f,80f))}})}
            .clickable{onTap()}) { content() }
        val gcTf = SimpleDateFormat("HH:mm", Locale.getDefault())
        Row(
            modifier = Modifier
                .then(if (msg.isOwn) Modifier.align(Alignment.End) else Modifier.align(Alignment.Start))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (msg.timestamp > 0) Text(
                gcTf.format(Date(msg.timestamp)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                modifier = Modifier.padding(end = 2.dp)
            )
            if (msg.isOwn && !msg.isDeleted) {
                val isRead = msg.readBy.keys.any { it != msg.senderId }
                Text(
                    if (isRead) "✓✓" else "✓",
                    fontSize = 10.sp,
                    color = if (isRead) Color(0xFF00BCD4)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
        }
    }
}

@Composable private fun GCActionIcon(icon: ImageVector, label: String, tint: Color = Color.Unspecified, onClick: () -> Unit) {
    IconButton(onClick=onClick,modifier=Modifier.size(34.dp)){Icon(icon,label,modifier=Modifier.size(17.dp),tint=if(tint==Color.Unspecified)MaterialTheme.colorScheme.onSurfaceVariant else tint)}
}

@Composable
private fun GCFileBubble(msg: GroupChatMsg, onDownload: () -> Unit) {
    if (msg.isDeleted) {
        Row(modifier=Modifier.fillMaxWidth().padding(vertical=2.dp),
            horizontalArrangement=if(msg.isOwn)Arrangement.End else Arrangement.Start) {
            if (!msg.isOwn) { AvatarCircle(letter=msg.senderAvatarLetter,sizeDp=28,username=msg.senderId); Spacer(Modifier.width(6.dp)) }
            Surface(color=MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), shape=RoundedCornerShape(12.dp)) {
                Text(msg.deletedBy.ifBlank{"Deleted"}, fontSize=13.sp, fontStyle=FontStyle.Italic,
                    color=MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier=Modifier.padding(horizontal=12.dp,vertical=8.dp))
            }
        }
        return
    }
    Row(modifier=Modifier.fillMaxWidth().padding(vertical=2.dp),horizontalArrangement=if(msg.isOwn)Arrangement.End else Arrangement.Start,verticalAlignment=Alignment.Bottom){
        if(!msg.isOwn){AvatarCircle(letter=msg.senderAvatarLetter,sizeDp=28,username=msg.senderId);Spacer(Modifier.width(6.dp))}
        Column(horizontalAlignment=if(msg.isOwn)Alignment.End else Alignment.Start){
            if(!msg.isOwn) Text(msg.senderName,fontSize=11.sp,fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(start=4.dp,bottom=2.dp))
            Surface(modifier=Modifier.widthIn(max=260.dp),color=if(msg.isOwn)MaterialTheme.colorScheme.primary.copy(0.85f)else MaterialTheme.colorScheme.surfaceVariant,
                shape=RoundedCornerShape(topStart=16.dp,topEnd=16.dp,bottomStart=if(msg.isOwn)16.dp else 4.dp,bottomEnd=if(msg.isOwn)4.dp else 16.dp)){
                Column(modifier=Modifier.padding(12.dp)){
                    Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Folder,null,tint=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,modifier=Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(msg.dirName,fontSize=14.sp,fontWeight=FontWeight.SemiBold,color=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)}
                    Spacer(Modifier.height(4.dp))
                    Text(msg.fileName,fontSize=11.sp,color=if(msg.isOwn)MaterialTheme.colorScheme.onPrimary.copy(0.7f)else MaterialTheme.colorScheme.onSurfaceVariant)
                    if(!msg.isOwn){Spacer(Modifier.height(8.dp));Surface(onClick=onDownload,color=MaterialTheme.colorScheme.primary,shape=RoundedCornerShape(8.dp)){Row(modifier=Modifier.padding(horizontal=12.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Download,"Download",tint=MaterialTheme.colorScheme.onPrimary,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Download",fontSize=12.sp,color=MaterialTheme.colorScheme.onPrimary)}}}
                }
            }

        }
        if(msg.isOwn) Spacer(Modifier.width(6.dp))
    }
}
