package com.nueng.translator.ui.online.friend

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle
import com.nueng.translator.ui.online.voice.VoiceChatBubble
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val FC_EMOJI_LIST = listOf(
    "\uD83D\uDE00","\uD83D\uDE02","\uD83D\uDE04","\uD83D\uDE06","\uD83D\uDE09","\uD83D\uDE0A",
    "\uD83D\uDE0D","\uD83D\uDE0E","\uD83D\uDE1B","\uD83D\uDE1C","\uD83D\uDE1D","\uD83D\uDE21",
    "\uD83D\uDE22","\uD83D\uDE28","\uD83D\uDE2D","\uD83D\uDE31","\uD83D\uDE33","\uD83D\uDE35",
    "\uD83D\uDC4D","\uD83D\uDC4E","\uD83D\uDC4F","\uD83D\uDC4A","\uD83D\uDC4B","\uD83D\uDC4C",
    "\u2764\uFE0F","\uD83D\uDC95","\uD83D\uDC99","\uD83D\uDC9A","\uD83D\uDC9B","\uD83D\uDCA5",
    "\uD83D\uDE80","\u2708\uFE0F","\uD83C\uDF89","\uD83C\uDF81","\uD83C\uDF55","\uD83C\uDF54",
    "\uD83D\uDC31","\uD83D\uDC36","\uD83D\uDC3C","\uD83D\uDCA9","\uD83E\uDD16","\uD83D\uDC7B",
    "\u26BD","\uD83C\uDFC0","\uD83C\uDF1F","\u2B50","\uD83C\uDF08","\u26A1"
)

private val FC_QUICK_EMOJIS = listOf("\u2764\uFE0F","\uD83D\uDE02","\uD83D\uDE2E","\uD83D\uDE22","\uD83D\uDE21","\uD83D\uDC4D")

@SuppressLint("UseKtx")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FriendChatScreen(
    chatId: String,
    friendUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToFriendInfo: (friendUserId: String) -> Unit = {},
    onNavigateToForward: (text: String) -> Unit = {},
    onNavigateToGroupChat: (groupId: String) -> Unit = {},
    viewModel: FriendChatViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val listState      = rememberLazyListState()
    val snackbarState  = remember { SnackbarHostState() }
    @Suppress("DEPRECATION")
    val clipboard      = LocalClipboardManager.current
    var inputText      by remember { mutableStateOf(TextFieldValue("")) }
    var msgToDownload  by remember { mutableStateOf<ChatMsg?>(null) }
    val context        = androidx.compose.ui.platform.LocalContext.current
    val recorder       = remember { com.nueng.translator.util.VoiceRecorder(context) }
    var isRecording      by remember { mutableStateOf(false) }
    var showEmojiPanel   by remember { mutableStateOf(false) }
    // fav emoji loaded from DataStore via ViewModel
    val favEmoji = uiState.favEmoji
    var selectingFavEmoji by remember { mutableStateOf(false) }
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    // Voice preview state: non-null means we have a recorded audio ready to review
    var voicePreviewData by remember { mutableStateOf<String?>(null) }
    var voicePreviewSec  by remember { mutableIntStateOf(0) }
    var previewPlayer    by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var previewPlaying   by remember { mutableStateOf(false) }
    var hasAudioPerm   by remember { mutableStateOf(
        androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    )}
    val audioPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPerm = granted }

    // Action menu state
    var actionMsg              by remember { mutableStateOf<ChatMsg?>(null) }
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var showAttachMenu         by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream   = context.contentResolver.openInputStream(uri) ?: return@let
                val original = BitmapFactory.decodeStream(stream)
                stream.close()
                // Scale to max 1920px for upload (visually lossless, avoids EOFException)
                val maxSize = 1920
                val scale = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height, 1f)
                val upload = if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(
                    original, (original.width * scale).toInt(), (original.height * scale).toInt(), true
                ) else original
                val out = java.io.ByteArrayOutputStream()
                upload.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
                viewModel.sendImage(b64, upload.width, upload.height)
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

    LaunchedEffect(chatId, friendUserId) {
        viewModel.init(chatId, friendUserId)
        viewModel.listenFriendOnline(friendUserId)
    }
    DisposableEffect(chatId, friendUserId) {
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
    // Scroll to highlighted message when reply preview tapped
    LaunchedEffect(uiState.highlightedMsgId) {
        val hid = uiState.highlightedMsgId
        if (hid.isNotBlank()) {
            val idx = uiState.messages.indexOfFirst { it.id == hid }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    LaunchedEffect(uiState.editingMsg?.id) {
        val editing = uiState.editingMsg
        if (editing != null) inputText = TextFieldValue(editing.text, TextRange(editing.text.length))
    }

    val showInputBar = !(uiState.isBlockedByFriend && !uiState.isAdmin)

    // Directory picker
    if (uiState.showDirectoryPicker) {
        DirectoryPickerSheet(
            directories = uiState.userDirectories,
            onSelect    = { viewModel.onDirectorySelected(it) },
            onDismiss   = { viewModel.onDismissDirectoryPicker() }
        )
    }
    uiState.selectedDirectory?.let { dir ->
        if (uiState.showSendConfirm) {
            AlertDialog(
                onDismissRequest = { viewModel.onDismissSendConfirm() },
                title = { Text("Send Directory?") },
                text  = { Text("Send " + dir.name + " to this chat? This file will be stored in " + uiState.friendDisplayName + "'s File Storage.") },
                confirmButton = { TextButton(onClick = { viewModel.confirmSendDirectory() }) { Text("Send") } },
                dismissButton = { TextButton(onClick = { viewModel.onDismissSendConfirm() }) { Text("Cancel") } }
            )
        }
    }
    msgToDownload?.let { msg ->
        AlertDialog(
            onDismissRequest = { msgToDownload = null },
            title = { Text("Download File?") },
            text  = { Text("Download " + msg.dirName + "? This will be added directly to your My Note.") },
            confirmButton = { TextButton(onClick = { viewModel.downloadAndImport(msg); msgToDownload = null }) { Text("Download") } },
            dismissButton = { TextButton(onClick = { msgToDownload = null }) { Text("Cancel") } }
        )
    }

    // Delete dialog
    if (showDeleteDialog && actionMsg != null) {
        val msg = actionMsg!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; actionMsg = null },
            title = { Text(if (deleteForEveryone) "Delete for everyone?" else "Delete for you?") },
            text  = { Text(if (deleteForEveryone) "This message will be deleted for all users." else "This message will be hidden from your view only.") },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteForEveryone) viewModel.deleteForEveryone(msg) else viewModel.deleteForMe(msg)
                    showDeleteDialog = false; actionMsg = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; actionMsg = null }) { Text("Cancel") } }
        )
    }

    // SelectView delete dialog
    if (showSelectDeleteDialog) {
        val count = uiState.selectedMsgIds.size
        AlertDialog(
            onDismissRequest = { showSelectDeleteDialog = false },
            title = { Text(if (selectDeleteForAll) "Delete for everyone?" else "Delete for you?") },
            text  = { Text(if (selectDeleteForAll) "$count message(s) deleted for all." else "$count message(s) hidden from your view.") },
            confirmButton = {
                TextButton(onClick = {
                    if (selectDeleteForAll) viewModel.deleteSelectedForEveryone() else viewModel.deleteSelectedForMe()
                    showSelectDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showSelectDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    // Emoji picker
    if (showEmojiPicker && actionMsg != null) {
        val msg = actionMsg!!
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false; actionMsg = null },
            title = { Text("React", fontSize = 15.sp) },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FC_QUICK_EMOJIS.forEach { emoji ->
                            Text(emoji, fontSize = 26.sp, modifier = Modifier.clickable {
                                viewModel.toggleReaction(msg, emoji); showEmojiPicker = false; actionMsg = null
                            })
                        }
                    }
                    LazyRow(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        items(FC_EMOJI_LIST.chunked(6)) { col ->
                            Column {
                                col.forEach { emoji ->
                                    Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(3.dp).clickable {
                                        viewModel.toggleReaction(msg, emoji); showEmojiPicker = false; actionMsg = null
                                    })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showEmojiPicker = false; actionMsg = null }) { Text("Cancel") } }
        )
    }

    // Help dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("NuengChat Help", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FCHelpSection("Chat Features",
                        "Tap bubble -> action bar (Reply, Edit, Delete, Emoji, Copy, Forward, Select).\n" +
                        "Swipe bubble left/right -> quick reply.\n" +
                        "Tap Select -> multi-select messages.\n" +
                        "Attach icon -> send .ntf word list file."
                    )
                    FCHelpSection("Print Commands",
                        "  table(col,row) or table(n) -> build a visual table\n" +
                        "  loop(n,msg) or repeat(n,msg) -> repeat msg n times (1-20)\n" +
                        "  fun() -> show all print commands\n" +
                        "  help() -> show this guide"
                    )
                    FCHelpSection("Auto-List Formatting",
                        "Start a line with a prefix + space:\n" +
                        "  1.  -> numbered list\n" +
                        "  -   -> dash bullet\n" +
                        "  *   -> star bullet\n" +
                        "  a.  -> lowercase letters\n" +
                        "  A.  -> uppercase letters\n" +
                        "Enter on empty item -> cancel list."
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("Got it!") } }
        )
    }

    // Fun dialog
    if (showFunDialog) {
        AlertDialog(
            onDismissRequest = { showFunDialog = false },
            title = { Text("Print Commands", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    FCHelpSection("table(col,row) or table(n)",
                        "Opens table builder. Row 1 = header.\n" +
                        "  table(3,4) -> 3 cols, 4 rows\n" +
                        "  table(3)   -> 3x3 square\n" +
                        "Max: 8 columns, 20 rows."
                    )
                    FCHelpSection("loop(n, msg) or repeat(n, msg)",
                        "Sends msg repeated n times (1-20).\n" +
                        "Out of range -> sent as plain text.\n" +
                        "  loop(3, Hello!) -> 3 lines"
                    )
                    FCHelpSection("Auto-List",
                        "  1.  -> numbered | -  -> dash | *  -> star\n" +
                        "  a.  -> lowercase | A. -> uppercase\n" +
                        "Enter on empty item -> cancel."
                    )
                    FCHelpSection("Meta",
                        "  help() -> full guide\n" +
                        "  fun()  -> this list"
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showFunDialog = false }) { Text("Got it!") } }
        )
    }

    // Table builder
    if (showTableBuilder && tableCols > 0 && tableRows > 0) {
        AlertDialog(
            onDismissRequest = { showTableBuilder = false },
            title = { Text("Table ($tableCols x $tableRows)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Row 1 = header.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        val hScroll = rememberScrollState(); val vScroll = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(vScroll)) {
                            for (r in 0 until tableRows) {
                                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                                    for (c in 0 until tableCols) {
                                        val isHeader = r == 0
                                        OutlinedTextField(
                                            value = tableCells.getOrNull(r)?.getOrNull(c) ?: "",
                                            onValueChange = { nv ->
                                                tableCells = tableCells.mapIndexed { ri, row -> row.mapIndexed { ci, cell -> if (ri == r && ci == c) nv else cell } }
                                            },
                                            modifier = Modifier.width(90.dp).padding(2.dp),
                                            singleLine = true,
                                            placeholder = { Text(if (isHeader) "H${c+1}" else "r${r}c${c+1}", fontSize = 10.sp) },
                                            label = if (isHeader) ({ Text("Header", fontSize = 9.sp) }) else null,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val jr = JSONArray()
                    for (r in 0 until tableRows) { val row = JSONArray(); for (c in 0 until tableCols) row.put(tableCells.getOrNull(r)?.getOrNull(c) ?: ""); jr.put(row) }
                    val jo = JSONObject(); jo.put("cols", tableCols); jo.put("rows", jr)
                    viewModel.sendMessage("[TABLE]$jo"); showTableBuilder = false; inputText = TextFieldValue("")
                }) { Text("Send Table", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = { TextButton(onClick = { showTableBuilder = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectMode) {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { viewModel.exitSelectMode() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel") } },
                    title = { Text("${uiState.selectedMsgIds.size} selected", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = {
                            val joined = viewModel.getSelectedMessages().joinToString("\n") { "${it.senderName} : ${it.text}" }
                            clipboard.setText(AnnotatedString(joined)); viewModel.exitSelectMode()
                        }) { Icon(Icons.Default.ContentCopy, "Copy") }
                        IconButton(onClick = {
                            val joined = viewModel.getSelectedMessages().joinToString("\n") { "${it.senderName} : ${it.text}" }
                            onNavigateToForward(joined); viewModel.exitSelectMode()
                        }) { Icon(Icons.AutoMirrored.Filled.Forward, "Forward") }
                        IconButton(
                            onClick = { selectDeleteForAll = viewModel.canDeleteSelectedForEveryone(); showSelectDeleteDialog = true },
                            enabled = uiState.selectedMsgIds.isNotEmpty()
                        ) { Icon(Icons.Default.Delete, "Delete", tint = if (uiState.selectedMsgIds.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarCircle(letter = uiState.friendAvatarLetter, sizeDp = 34, username = uiState.friendUserId)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(uiState.friendDisplayName.ifBlank { "..." }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = if (uiState.friendIsOnline) Icons.Default.Cloud else Icons.Default.CloudOff, contentDescription = null,
                                        tint = if (uiState.friendIsOnline) Color(0xFF4CAF50) else Color(0xFFEF5350), modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.friendIsOnline) "Online" else "Offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                    actions = { IconButton(onClick = { onNavigateToFriendInfo(friendUserId) }) { Icon(Icons.Default.Info, "Friend Info", tint = MaterialTheme.colorScheme.primary) } }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isUploading) { LinearProgressIndicator(progress = { uiState.uploadProgress / 100f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary) }

            if (uiState.isBlocked) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("You have blocked this user.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else if (uiState.isBlockedByFriend) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("You have been blocked by", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Text(uiState.blockedByName.ifBlank { uiState.friendDisplayName }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        if (uiState.isAdmin) { Spacer(Modifier.height(12.dp)); Text("Admin: you can still send messages.", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center) }
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(items = uiState.messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { msg ->
                        when (msg.dataType) {
                            "group_invite" -> {
                                GroupInviteCard(
                                    msg       = msg,
                                    onAccept  = {
                                        viewModel.acceptGroupInvite(msg)
                                        if (msg.autoAdd) onNavigateToGroupChat(msg.groupId)
                                    },
                                    onDecline = { viewModel.declineGroupInvite(msg) }
                                )
                            }
                            "voice" -> {
                                VoiceFileBubbleWrapper(
                                    msg          = msg,
                                    isHighlighted = actionMsg?.id == msg.id,
                                    isReplied    = uiState.highlightedMsgId == msg.id,
                                    isSelected   = msg.id in uiState.selectedMsgIds,
                                    isSelectMode = uiState.isSelectMode,
                                    onTap        = { if (uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg = if (actionMsg?.id == msg.id) null else msg },
                                    onReply      = { viewModel.setReplyTo(msg); actionMsg = null },
                                    onDelete     = { actionMsg = msg; deleteForEveryone = false; showDeleteDialog = true },
                                    onDeleteAll  = { actionMsg = msg; deleteForEveryone = true; showDeleteDialog = true },
                                    onForward    = { onNavigateToForward("__VOICE__${msg.id}"); actionMsg = null },
                                    onSelect     = { viewModel.enterSelectMode(msg); actionMsg = null },
                                    onEmoji      = { actionMsg = msg; showEmojiPicker = true },
                                    canDeleteAll = viewModel.canDeleteForEveryone(msg)
                                ) {
                                    VoiceChatBubble(
                                        msgId           = msg.id,
                                        voiceData       = msg.voiceData,
                                        voiceDuration   = msg.voiceDuration,
                                        senderName      = msg.senderName,
                                        senderLetter    = msg.senderAvatarLetter,
                                        senderId        = msg.senderId,
                                        isOwn           = msg.isOwn,
                                        timestamp       = msg.timestamp,
                                        isDeleted       = msg.isDeleted,
                                        deletedBy       = msg.deletedBy,
                                        reactions       = msg.reactions,
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
                                VoiceFileBubbleWrapper(
                                    msg           = msg,
                                    isHighlighted = actionMsg?.id == msg.id,
                                    isReplied     = uiState.highlightedMsgId == msg.id,
                                    isSelected    = msg.id in uiState.selectedMsgIds,
                                    isSelectMode  = uiState.isSelectMode,
                                    onTap         = { if (uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg = if (actionMsg?.id == msg.id) null else msg },
                                    onReply       = { viewModel.setReplyTo(msg); actionMsg = null },
                                    onDelete      = { actionMsg = msg; deleteForEveryone = false; showDeleteDialog = true },
                                    onDeleteAll   = { actionMsg = msg; deleteForEveryone = true; showDeleteDialog = true },
                                    onForward     = { onNavigateToForward("__IMAGE__${msg.id}"); actionMsg = null },
                                    onSelect      = { viewModel.enterSelectMode(msg); actionMsg = null },
                                    onEmoji       = { actionMsg = msg; showEmojiPicker = true },
                                    canDeleteAll  = viewModel.canDeleteForEveryone(msg)
                                ) {
                                    ImageChatBubble(
                                        imageData    = msg.imageData,
                                        imageUrl     = msg.imageUrl,
                                        isOwn        = msg.isOwn,
                                        senderName   = msg.senderName,
                                        senderLetter = msg.senderAvatarLetter,
                                        senderId     = msg.senderId,
                                        timestamp    = msg.timestamp,
                                        isDeleted    = msg.isDeleted,
                                        deletedBy    = msg.deletedBy
                                    )
                                }
                            }
                            "file" -> {
                                VoiceFileBubbleWrapper(
                                    msg          = msg,
                                    isHighlighted = actionMsg?.id == msg.id,
                                    isReplied    = uiState.highlightedMsgId == msg.id,
                                    isSelected   = msg.id in uiState.selectedMsgIds,
                                    isSelectMode = uiState.isSelectMode,
                                    onTap        = { if (uiState.isSelectMode) viewModel.toggleSelect(msg) else actionMsg = if (actionMsg?.id == msg.id) null else msg },
                                    onReply      = { viewModel.setReplyTo(msg); actionMsg = null },
                                    onDelete     = { actionMsg = msg; deleteForEveryone = false; showDeleteDialog = true },
                                    onDeleteAll  = { actionMsg = msg; deleteForEveryone = true; showDeleteDialog = true },
                                    onForward    = { onNavigateToForward("__FILE__${msg.id}"); actionMsg = null },
                                    onSelect     = { viewModel.enterSelectMode(msg); actionMsg = null },
                                    onEmoji      = { actionMsg = msg; showEmojiPicker = true },
                                    canDeleteAll = viewModel.canDeleteForEveryone(msg)
                                ) {
                                    FileChatBubble(msg = msg, isOwn = msg.isOwn, onDownload = { msgToDownload = msg }, onReactionTap = { emoji -> viewModel.toggleReaction(msg, emoji) })
                                }
                            }
                            else -> {
                                FriendChatBubble(
                                    msg             = msg,
                                    isHighlighted   = actionMsg?.id == msg.id || uiState.editingMsg?.id == msg.id,
                                    isReplied       = uiState.highlightedMsgId == msg.id,
                                    isSelected      = msg.id in uiState.selectedMsgIds,
                                    isSelectMode    = uiState.isSelectMode,
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
                                    canEdit         = viewModel.canEdit(msg),
                                    canDeleteForAll = viewModel.canDeleteForEveryone(msg)
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
                            val fcReplyPreview = when (reply.dataType) {
                                "voice" -> "🎤 Voice message (${reply.voiceDuration}s)"
                                "file"  -> "📁 ${reply.fileName}"
                                else    -> reply.text
                            }
                            Text(fcReplyPreview, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.clearReply() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Edit banner
            AnimatedVisibility(visible = uiState.editingMsg != null, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editing message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.cancelEdit(); inputText = TextFieldValue("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            // Input bar
            if (showInputBar && !uiState.isSelectMode) {
                if (isRecording) {
                    // ── Voice Recording Stage ──────────────────────────────
                    VoiceRecordingBar(
                        onRelease = {
                            isRecording = false
                            val result = recorder.stopRecording()
                            if (result != null && result.first.isNotBlank() && result.second >= 1) {
                                voicePreviewData = result.first
                                voicePreviewSec  = result.second
                            }
                        },
                        onCancel = {
                            isRecording = false
                            recorder.cancelRecording()
                        }
                    )
                } else if (voicePreviewData != null) {
                    // ── Voice Preview Stage ────────────────────────────────
                    VoicePreviewBar(
                        durationSec  = voicePreviewSec,
                        isPlaying    = previewPlaying,
                        onPlay       = {
                            if (previewPlaying) {
                                previewPlayer?.pause()
                                previewPlaying = false
                            } else {
                                val cacheFile = java.io.File(context.cacheDir, "voice_preview_tmp.3gp")
                                try {
                                    val bytes = Base64.decode(voicePreviewData!!, Base64.DEFAULT)
                                    cacheFile.writeBytes(bytes)
                                    val mp = android.media.MediaPlayer()
                                    mp.setDataSource(cacheFile.absolutePath)
                                    mp.prepare()
                                    mp.setOnCompletionListener { previewPlaying = false }
                                    mp.start()
                                    previewPlayer = mp
                                    previewPlaying = true
                                } catch (_: Exception) { previewPlaying = false }
                            }
                        },
                        onDiscard = {
                            previewPlayer?.stop(); previewPlayer?.release(); previewPlayer = null
                            previewPlaying = false
                            voicePreviewData = null
                            voicePreviewSec  = 0
                        },
                        onSend = {
                            previewPlayer?.stop(); previewPlayer?.release(); previewPlayer = null
                            previewPlaying = false
                            viewModel.sendVoiceMessage(voicePreviewData!!, voicePreviewSec)
                            voicePreviewData = null
                            voicePreviewSec  = 0
                        }
                    )
                } else {
                // Emoji Panel — sits above input row, pushes content up
                AnimatedVisibility(
                    visible = showEmojiPanel,
                    enter   = slideInVertically { it } + fadeIn(),
                    exit    = slideOutVertically { it } + fadeOut()
                ) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        if (selectingFavEmoji) {
                            Text("Tap to set as favourite ⭐",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                        }
                        val rows = FC_EMOJI_LIST.chunked(8)
                        rows.forEach { rowEmojis ->
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly) {
                                rowEmojis.forEach { emoji ->
                                    Box(modifier = Modifier.size(40.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (selectingFavEmoji) {
                                                    // Single tap sets as fav
                                                    viewModel.setFavEmoji(emoji)
                                                    selectingFavEmoji = false
                                                    showEmojiPanel = false
                                                } else {
                                                    // Normal tap inserts emoji into text
                                                    inputText = TextFieldValue(
                                                        inputText.text + emoji,
                                                        TextRange(inputText.text.length + emoji.length))
                                                }
                                            },
                                            onLongClick = {
                                                // Long press always sets as fav
                                                viewModel.setFavEmoji(emoji)
                                                selectingFavEmoji = false
                                                showEmojiPanel = false
                                            }
                                        ),
                                        contentAlignment = Alignment.Center) {
                                        Text(emoji, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Attach
                    Box {
                        IconButton(onClick = { showAttachMenu = !showAttachMenu }, enabled = !uiState.isUploading, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.AddCircleOutline, "Attach", tint = if (!uiState.isUploading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded         = showAttachMenu,
                            onDismissRequest = { showAttachMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text    = { Text("File (.ntf)") },
                                onClick = { showAttachMenu = false; viewModel.onAttachTapped() }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text    = { Text("Image") },
                                onClick = { showAttachMenu = false; imageLauncher.launch("image/*") }
                            )
                        }
                    }
                    // Text field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { nv ->
                            val ar = fcApplyAutoList(inputText, nv)
                            inputText = if (ar.text != nv.text) ar else nv
                        },
                        modifier    = Modifier.weight(1f),
                        placeholder = { Text(if (uiState.editingMsg != null) "Edit..." else "Message...", fontSize = 14.sp) },
                        singleLine  = false,
                        maxLines    = 5,
                        shape       = RoundedCornerShape(16.dp)
                    )
                    // Emoji/Keyboard toggle
                    val micScope = rememberCoroutineScope()
                    val textFieldFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                    IconButton(onClick = {
                        if (showEmojiPanel) {
                            showEmojiPanel = false
                            selectingFavEmoji = false
                            // Request focus then show keyboard
                            micScope.launch {
                                kotlinx.coroutines.delay(100)
                                try { textFieldFocusRequester.requestFocus() } catch (_: Exception) {}
                                keyboardController?.show()
                            }
                        } else {
                            showEmojiPanel = true
                            keyboardController?.hide()
                            // Don't clear focus — let TextField keep focus for typing
                        }
                    }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (showEmojiPanel) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                            if (showEmojiPanel) "Keyboard" else "Emoji",
                            tint = if (showEmojiPanel) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Fav emoji — tap to send instantly
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                        .combinedClickable(
                            onClick     = {
                                if (selectingFavEmoji) {
                                    // Already in selection mode — close it
                                    selectingFavEmoji = false
                                    showEmojiPanel = false
                                } else {
                                    viewModel.sendMessage(favEmoji)
                                }
                            },
                            onLongClick = {
                                // Enter fav emoji selection mode
                                selectingFavEmoji = true
                                showEmojiPanel = true
                                keyboardController?.hide()
                            }
                        ),
                        contentAlignment = Alignment.Center) {
                        // Show star overlay when in selection mode
                        Box(contentAlignment = Alignment.TopEnd) {
                            Text(favEmoji, fontSize = 20.sp)
                            if (selectingFavEmoji) {
                                Text("⭐", fontSize = 8.sp,
                                    modifier = Modifier.offset(x = 2.dp, y = (-2).dp))
                            }
                        }
                    }
                    // Hold to record mic button (only when text empty) OR send button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { _ ->
                                        if (!hasAudioPerm) {
                                            audioPerm.launch(android.Manifest.permission.RECORD_AUDIO)
                                            return@detectTapGestures
                                        }
                                        // Start recording on main thread via micScope
                                        try {
                                            recorder.startRecording()
                                        } catch (_: Exception) {
                                            micScope.launch {
                                                snackbarState.showSnackbar("Cannot record: check microphone permission")
                                            }
                                            return@detectTapGestures
                                        }
                                        isRecording = true
                                        val holdStart = System.currentTimeMillis()
                                        // Wait for user to release finger
                                        tryAwaitRelease()
                                        val held = System.currentTimeMillis() - holdStart
                                        isRecording = false
                                            if (held < 500L) {
                                                // Too short — discard
                                                recorder.cancelRecording()
                                                micScope.launch {
                                                    snackbarState.showSnackbar("Hold longer to record")
                                                }
                                            } else {
                                                // Good recording — go to preview
                                                val result = recorder.stopRecording()
                                                if (result != null && result.first.isNotBlank()) {
                                                    voicePreviewData = result.first
                                                    voicePreviewSec  = result.second
                                                }
                                            }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, "Hold to record",
                            tint = if (isRecording) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp))
                    }
                    // Show mic when empty, send when typing
                    if (inputText.text.isBlank() && uiState.editingMsg == null) {
                    // Mic shown below via the existing mic Box
                    } else {
                    Surface(
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                        color    = if (inputText.text.isNotBlank() || uiState.editingMsg != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        onClick  = {
                            val raw = inputText.text.trim()
                            val tm2 = Regex("^[Tt][Aa][Bb][Ll][Ee]\\((\\d+),(\\d+)\\)$").matchEntire(raw)
                            val tm1 = Regex("^[Tt][Aa][Bb][Ll][Ee]\\((\\d+)\\)$").matchEntire(raw)
                            if (tm2 != null || tm1 != null) {
                                val c: Int; val r: Int
                                if (tm2 != null) { c = tm2.groupValues[1].toIntOrNull()?.coerceIn(1,8)?:2; r = tm2.groupValues[2].toIntOrNull()?.coerceIn(1,20)?:2 }
                                else { val n = tm1!!.groupValues[1].toIntOrNull()?.coerceIn(1,8)?:2; c=n; r=n }
                                tableCols=c; tableRows=r; tableCells=List(r){List(c){""}};showTableBuilder=true; inputText=TextFieldValue("")
                            } else {
                                if (raw.equals("help()", ignoreCase = true)) {
                                    showHelpDialog = true; inputText = TextFieldValue("")
                                } else if (raw.equals("fun()", ignoreCase = true)) {
                                    showFunDialog = true; inputText = TextFieldValue("")
                                } else {
                                val lm = Regex("^(?:loop|repeat)\\(\\s*(\\d+)\\s*,\\s*(.+)\\)$", RegexOption.IGNORE_CASE).matchEntire(raw)
                                if (lm != null) {
                                    val times = lm.groupValues[1].toIntOrNull()?:0
                                    val msg   = lm.groupValues[2].trim()
                                    if (times in 1..20) viewModel.sendMessage((1..times).joinToString("\n"){msg}) else viewModel.sendMessage(raw)
                                    inputText = TextFieldValue("")
                                } else if (raw.isNotBlank()) {
                                    viewModel.sendMessage(raw); inputText = TextFieldValue("")
                                }
                                } // end help/fun else
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(if (uiState.editingMsg != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Send, "Send",
                                tint = if (inputText.text.isNotBlank() || uiState.editingMsg != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                    } // end send button visible
                }
            }
        }
    }
}
}

@Composable
private fun VoicePreviewBar(
    durationSec: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDiscard: () -> Unit,
    onSend: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "prevWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )
    val m = durationSec / 60; val s = durationSec % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Discard
        IconButton(onClick = onDiscard, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Discard",
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
        // Play/Pause preview
        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(0.12f))
            .clickable { onPlay() },
            contentAlignment = Alignment.Center) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (isPlaying) "Pause" else "Play preview",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        // Duration
        Text("%d:%02d".format(m, s), fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp))
        // Waveform (static when not playing, animated when playing)
        Row(modifier = Modifier.weight(1f).height(28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until 24) {
                val h = if (isPlaying)
                    (0.25f + 0.75f * abs(kotlin.math.sin(phase + i * 0.45f))) * 24
                else
                    (0.2f + 0.7f * abs(kotlin.math.sin(Math.PI * i / 24).toFloat())) * 24
                Box(modifier = Modifier.width(3.dp).height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(
                        if (isPlaying) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                    ))
            }
        }
        // Send
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onSend() },
            contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send voice",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun VoiceRecordingBar(
    onRelease: () -> Unit,
    onCancel: () -> Unit
) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSec++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "recWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    val m = elapsedSec / 60; val s = elapsedSec % 60
    val timeStr = "%d:%02d".format(m, s)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel swipe hint
        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Cancel",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(6.dp))

        // Pulsing red dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
        )

        Spacer(Modifier.width(8.dp))

        // Timer
        Text(
            text      = timeStr,
            fontSize  = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.error,
            modifier  = Modifier.width(42.dp)
        )

        Spacer(Modifier.width(8.dp))

        // Live waveform (28 animated bars)
        Row(
            modifier = Modifier.weight(1f).height(32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            for (i in 0 until 28) {
                val h = (0.25f + 0.75f * abs(
                    kotlin.math.sin(phase + i * 0.45f)
                ) * 28).dp
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Release to send hint + hold button
        Text("Release", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(0.15f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            tryAwaitRelease()
                            onRelease()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, "Recording",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun GroupInviteCard(
    msg: ChatMsg,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isSender  = msg.isOwn  // sender is already in group, only sees Cancel
    val roleColor = when (msg.invitedByRole) {
        "creator" -> Color(0xFFFFA000)
        "admin"   -> Color(0xFF00BCD4)
        else      -> MaterialTheme.colorScheme.primary
    }
    val roleLabel = when (msg.invitedByRole) {
        "creator" -> "Creator"; "admin" -> "Admin"; else -> "Member"
    }
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(msg.groupName.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Group Invite", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(msg.groupName, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (isSender) {
                Text("Invite sent to this person.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Waiting for their response...", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Invited by ", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("@${msg.invitedBy}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = roleColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)) {
                        Text(roleLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            color = roleColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (msg.autoAdd) {
                    Spacer(Modifier.height(4.dp))
                    Text("You will be added directly upon accepting.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text("Your request will go to group admins for approval.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (isSender) {
                // Sender: only Cancel Invite
                androidx.compose.material3.OutlinedButton(
                    onClick  = onDecline,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancel Invite") }
            } else {
                // Receiver: Accept + Decline
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(
                        onClick  = onAccept,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (msg.autoAdd) "Accept & Join" else "Send Request") }
                    androidx.compose.material3.OutlinedButton(
                        onClick  = onDecline,
                        modifier = Modifier.weight(1f)
                    ) { Text("Decline") }
                }
            }
        }
    }
}

// Auto-list for FriendChat
private fun fcApplyAutoList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val newText = new.text; val oldText = old.text
    if (!newText.endsWith("\n") || newText.length <= oldText.length) return new
    val tbf = newText.dropLast(1)
    val ll  = tbf.substringAfterLast("\n", tbf)
    val di  = ll.indexOf(". ")
    if (di > 0) {
        val mn = ll.substring(0, di)
        if (mn.isNotEmpty() && mn.all { it.isDigit() }) {
            val ct = ll.substring(di + 2)
            if (ct.isEmpty()) { val r = tbf.dropLast(ll.length); return TextFieldValue(r, TextRange(r.length)) }
            val nx = (mn.toIntOrNull()?:0)+1; val r = "$newText$nx. "; return TextFieldValue(r, TextRange(r.length))
        }
    }
    if (ll == "- " || ll == "* ") { val r = tbf.dropLast(ll.length); return TextFieldValue(r, TextRange(r.length)) }
    if (ll.startsWith("- ") && ll.length>2) { val r= "$newText- "; return TextFieldValue(r, TextRange(r.length)) }
    if (ll.startsWith("* ") && ll.length>2) { val r= "$newText* "; return TextFieldValue(r, TextRange(r.length)) }
    val le = Regex("""^[a-zA-Z]\. $"""); val la = Regex("""^([a-zA-Z])\. (.+)$""")
    if (le.matches(ll)) { val r = tbf.dropLast(ll.length); return TextFieldValue(r, TextRange(r.length)) }
    val lm = la.matchEntire(ll)
    if (lm != null) {
        val ch = lm.groupValues[1].first()
        val nx = when (ch) {
            'z' -> 'z'
            'Z' -> 'Z'
            else -> ch+1
        }
        val r = "$newText$nx. "; return TextFieldValue(r, TextRange(r.length))
    }
    return TextFieldValue(newText, TextRange(newText.length))
}

// Table JSON parser
private fun fcParseTableJson(jsonText: String): Pair<Int, List<List<String>>>? {
    return try {
        val json = JSONObject(jsonText); val cols = json.getInt("cols"); val jr = json.getJSONArray("rows")
        val rows = (0 until jr.length()).map { r -> val row = jr.getJSONArray(r); (0 until cols).map { c -> row.optString(c,"") } }
        if (cols>0 && rows.isNotEmpty()) Pair(cols,rows) else null
    } catch (_: Exception) { null }
}

@Composable
private fun FcTableBubble(jsonText: String, isOwn: Boolean) {
    val parsed = remember(jsonText) { fcParseTableJson(jsonText) }
    if (parsed == null) { Text(jsonText, fontSize = 12.sp, modifier = Modifier.padding(8.dp)); return }
    val (cols, rows) = parsed
    val hBg = if (isOwn) MaterialTheme.colorScheme.primary.copy(0.9f) else MaterialTheme.colorScheme.primary
    val hFg = MaterialTheme.colorScheme.onPrimary
    val cBg = if (isOwn) MaterialTheme.colorScheme.primary.copy(0.7f) else MaterialTheme.colorScheme.surfaceVariant
    val cBgA= if (isOwn) MaterialTheme.colorScheme.primary.copy(0.6f) else MaterialTheme.colorScheme.surface
    val cFg = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val bdr = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(0.3f) else MaterialTheme.colorScheme.outline.copy(0.4f)
    val hs = rememberScrollState()
    Column(modifier = Modifier.horizontalScroll(hs).padding(4.dp)) {
        for (r in 0 until rows.size) {
            val row = rows[r]; val ih = r==0
            Row {
                for (c in 0 until cols) {
                    Box(modifier = Modifier.border(0.5.dp,bdr).background(when{ih->hBg;r%2==1->cBg;else->cBgA}).padding(horizontal=8.dp,vertical=5.dp).widthIn(min=48.dp,max=120.dp)) {
                        Text(row.getOrElse(c){""}, fontSize=12.sp, fontWeight=if(ih)FontWeight.Bold else FontWeight.Normal, color=if(ih)hFg else cFg, maxLines=3, overflow=TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// FriendChat bubble with full interactions
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun FriendChatBubble(
    msg: ChatMsg,
    isHighlighted: Boolean,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onTap: () -> Unit,
    onSwipeReply: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForAll: () -> Unit,
    onEmoji: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onSelect: () -> Unit,
    onReactionTap: (String) -> Unit,
    onReplyTap: (String) -> Unit = {},
    isReplied: Boolean = false,
    canEdit: Boolean,
    canDeleteForAll: Boolean
) {
    val tf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val scope = rememberCoroutineScope()
    val offX = remember { Animatable(0f) }
    var tapped by remember { mutableStateOf(false) }
    LaunchedEffect(tapped) {
        if (tapped) {
            kotlinx.coroutines.delay(160); tapped = false
        }
    }

    val bg = when {
        msg.isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
        tapped -> if (msg.isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(
            0.18f
        )

        msg.isOwn -> MaterialTheme.colorScheme.primary.copy(0.85f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val sh = RoundedCornerShape(
        topStart = if (msg.replyToId.isNotBlank() && !msg.isDeleted) 4.dp else 16.dp,
        topEnd = if (msg.replyToId.isNotBlank() && !msg.isDeleted) 4.dp else 16.dp,
        bottomStart = if (msg.isOwn) 16.dp else 4.dp,
        bottomEnd = if (msg.isOwn) 4.dp else 16.dp
    )
    // Pulsing highlight: animate alpha between 0 and 0.35 when isHighlighted
    val pulseAnim = remember { Animatable(0f) }
    // Gold border pulse: 0->1->0 repeated for 2s
    LaunchedEffect(isReplied) {
        if (isReplied) {
            repeat(4) {
                pulseAnim.animateTo(1f, tween(250))
                pulseAnim.animateTo(0f, tween(250))
            }
        } else {
            pulseAnim.snapTo(0f)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Action bar
        if (isHighlighted && !isReplied && !msg.isDeleted && !isSelectMode) {
            Row(
                modifier = Modifier.then(
                    if (msg.isOwn) Modifier.align(Alignment.End) else Modifier.align(
                        Alignment.Start
                    )
                )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FCActionIcon(Icons.AutoMirrored.Filled.Reply, "Reply", onClick = onReply)
                if (canEdit) FCActionIcon(Icons.Default.Edit, "Edit", onClick = onEdit)
                FCActionIcon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { if (canDeleteForAll) onDeleteForAll() else onDeleteForMe() })
                FCActionIcon(Icons.Default.EmojiEmotions, "Emoji", onClick = onEmoji)
                FCActionIcon(Icons.Default.ContentCopy, "Copy", onClick = onCopy)
                FCActionIcon(
                    Icons.AutoMirrored.Filled.Forward,
                    "Forward",
                    onClick = { if (!msg.isDeleted) onForward() })
                FCActionIcon(
                    Icons.Default.CheckBox,
                    "Select",
                    onClick = onClick@{ onSelect();return@onClick })
            }
        }
        val goldColor = Color(0xFFFFD700)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).then(
                when {
                    isSelected    -> Modifier.background(MaterialTheme.colorScheme.primary.copy(0.18f))
                    isHighlighted -> Modifier.background(MaterialTheme.colorScheme.primary.copy(0.07f), RoundedCornerShape(8.dp))
                    else          -> Modifier
                }
            ),
            horizontalArrangement = if (msg.isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!msg.isOwn) {
                Box {
                    AvatarCircle(letter = msg.senderAvatarLetter, sizeDp = 28, username = msg.senderId)
                    if (isSelectMode) Icon(
                        if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = if (msg.isOwn) Alignment.End else Alignment.Start) {
                if (!msg.isOwn) Text(
                    msg.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                // Forwarded label
                if (msg.isForwarded && !msg.isDeleted) {
                    Text(
                        "Forwarded", fontSize = 10.sp, fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                // Reply preview
                if (msg.replyToId.isNotBlank() && !msg.isDeleted) {
                    // Show correct icon in reply preview based on dataType
                    val replyIcon = when (msg.replyToDataType) {
                        "voice" -> " "
                        "file" -> " "
                        else -> ""
                    }
                    Surface(
                        modifier = Modifier.widthIn(max = 260.dp).padding(bottom = 2.dp)
                            .clickable { onReplyTap(msg.replyToId) },
                        color = if (msg.isOwn) MaterialTheme.colorScheme.primary.copy(0.55f) else MaterialTheme.colorScheme.surfaceVariant.copy(
                            0.7f
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 4.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                msg.replyToSender,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                replyIcon + msg.replyToPreview,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Swipeable bubble
                Box(
                    modifier = Modifier.then(
                        if (msg.text.startsWith("[TABLE]")) Modifier.wrapContentWidth() else Modifier.widthIn(
                            max = 260.dp
                        )
                    )
                        .offset { IntOffset(offX.value.roundToInt(), 0) }
                        .pointerInput(msg.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(offX.value) > 60f) onSwipeReply();offX.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                    }
                                },
                                onHorizontalDrag = { _, d ->
                                    scope.launch {
                                        offX.snapTo(
                                            (offX.value + d).coerceIn(
                                                -80f,
                                                80f
                                            )
                                        )
                                    }
                                }
                            )
                        }) {
                    Box {
                        Surface(
                            modifier = Modifier.then(
                                if (msg.text.startsWith("[TABLE]")) Modifier.wrapContentWidth() else Modifier.widthIn(max = 260.dp)
                            )
                            .then(
                                if (isReplied && pulseAnim.value > 0f)
                                    Modifier.border(2.dp, goldColor.copy(alpha = pulseAnim.value), sh)
                                else Modifier
                            )
                            .clickable { tapped = true; onTap() }, color = bg, shape = sh
                        ) {
                            if (msg.isDeleted) Text(
                                msg.deletedBy.ifBlank { "Deleted" },
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                            else if (msg.text.startsWith("[TABLE]")) FcTableBubble(
                                msg.text.removePrefix(
                                    "[TABLE]"
                                ), msg.isOwn
                            )
                            else Text(
                                msg.text,
                                fontSize = 14.sp,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } // end Box
                    // Reactions
                }
                if (!msg.isDeleted && msg.reactions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        msg.reactions.forEach { (emoji, names) ->
                            Surface(
                                modifier = Modifier.clickable { onReactionTap(emoji) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 6.dp,
                                        vertical = 2.dp
                                    ), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 14.sp); Spacer(Modifier.width(2.dp))
                                    Text(
                                        names.size.toString(),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (msg.isEdited && !msg.isDeleted) Text(
                        "edited",
                        fontSize = 9.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                    )
                    if (msg.timestamp > 0) Text(
                        tf.format(Date(msg.timestamp)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    // Read receipt — only on own messages
                    if (msg.isOwn && !msg.isDeleted) {
                        val isRead = msg.readBy.any { it != msg.senderId }
                        Text(
                            text = if (isRead) "\u2713\u2713" else "\u2713",
                            fontSize = 10.sp,
                            color = if (isRead) Color(0xFF00BCD4)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                if (msg.isOwn) {
                    if (isSelectMode) Icon(
                        if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
    }
}
    @Composable
    private fun FCHelpSection(title: String, body: String) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }

    // Wrapper composable that adds action bar + swipe-reply to voice/file bubbles
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun VoiceFileBubbleWrapper(
        msg: ChatMsg,
        isHighlighted: Boolean,
        isReplied: Boolean = false,
        isSelected: Boolean,
        isSelectMode: Boolean,
        onTap: () -> Unit,
        onReply: () -> Unit,
        onDelete: () -> Unit,
        onDeleteAll: () -> Unit,
        onForward: () -> Unit,
        onSelect: () -> Unit,
        onEmoji: () -> Unit,
        canDeleteAll: Boolean,
        content: @Composable () -> Unit
    ) {
        val scope = rememberCoroutineScope()
        val offX = remember { Animatable(0f) }
        val bg = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(0.15f)
            isHighlighted -> MaterialTheme.colorScheme.primary.copy(0.07f)
            else -> Color.Transparent
        }
        val wrapPulse = remember { Animatable(0f) }
        LaunchedEffect(isReplied) {
            if (isReplied) {
                repeat(3) {
                    wrapPulse.animateTo(0.25f, tween(250))
                    wrapPulse.animateTo(0f, tween(250))
                }
            } else {
                wrapPulse.snapTo(0f)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isReplied && wrapPulse.value > 0f)
                        MaterialTheme.colorScheme.primary.copy(wrapPulse.value)
                    else bg
                )
        ) {
            // Action bar
            if (isHighlighted && !isReplied && !isSelectMode) {
                Row(
                    modifier = Modifier
                        .then(
                            if (msg.isOwn) Modifier.align(Alignment.End) else Modifier.align(
                                Alignment.Start
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FCActionIcon(Icons.AutoMirrored.Filled.Reply, "Reply", onClick = onReply)
                    FCActionIcon(Icons.Default.EmojiEmotions, "Emoji", onClick = onEmoji)
                    FCActionIcon(Icons.Default.Share, "Forward", onClick = onForward)
                    if (canDeleteAll) FCActionIcon(
                        Icons.Default.DeleteForever, "Delete",
                        tint = MaterialTheme.colorScheme.error, onClick = onDeleteAll
                    )
                    else FCActionIcon(Icons.Default.Delete, "Delete for me", onClick = onDelete)
                    FCActionIcon(Icons.Default.SelectAll, "Select", onClick = onSelect)
                }
            }
            // Swipeable content
            Box(
                modifier = Modifier
                    .offset { IntOffset(offX.value.roundToInt(), 0) }
                    .pointerInput(msg.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (abs(offX.value) > 60f) onReply()
                                    offX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            },
                            onHorizontalDrag = { _, d ->
                                scope.launch { offX.snapTo((offX.value + d).coerceIn(-80f, 80f)) }
                            }
                        )
                    }
                    .clickable { onTap() }
            ) {
                content()
            }
            // Timestamp + read receipt for voice/file
            val wrapTf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Row(
                modifier = Modifier
                    .then(if (msg.isOwn) Modifier.align(Alignment.End) else Modifier.align(Alignment.Start))
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (msg.timestamp > 0) Text(
                    wrapTf.format(Date(msg.timestamp)), fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                    modifier = Modifier.padding(end = 2.dp)
                )
                if (msg.isOwn && !msg.isDeleted) {
                    val isRead = msg.readBy.any { it != msg.senderId }
                    Text(
                        text = if (isRead) "✓✓" else "✓",
                        fontSize = 10.sp,
                        color = if (isRead) Color(0xFF00BCD4)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    @Composable
    private fun FCActionIcon(
        icon: ImageVector,
        label: String,
        tint: Color = Color.Unspecified,
        onClick: () -> Unit
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
            Icon(
                icon,
                label,
                modifier = Modifier.size(17.dp),
                tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun FileChatBubble(
        msg: ChatMsg,
        isOwn: Boolean,
        onDownload: () -> Unit,
        onReactionTap: (String) -> Unit = {}
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isOwn) {
                AvatarCircle(
                    letter   = msg.senderAvatarLetter,
                    sizeDp   = 28,
                    username = msg.senderId
                ); Spacer(Modifier.width(6.dp))
            }
            Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
                if (msg.isDeleted) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            msg.deletedBy.ifBlank { "Deleted" }, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.widthIn(max = 260.dp),
                        color = if (isOwn) MaterialTheme.colorScheme.primary.copy(0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwn) 16.dp else 4.dp,
                            bottomEnd = if (isOwn) 4.dp else 16.dp
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Folder,
                                    null,
                                    tint = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    msg.dirName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                msg.fileName,
                                fontSize = 11.sp,
                                color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!isOwn) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    onClick = onDownload,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Download,
                                            "Download",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp)); Text(
                                        "Download",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    }
                                }
                            }
                        }
                    }
                    // Reactions
                    if (msg.reactions.isNotEmpty()) {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            msg.reactions.forEach { (emoji, names) ->
                                Surface(
                                    modifier = Modifier.clickable { onReactionTap(emoji) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 6.dp,
                                            vertical = 2.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(emoji, fontSize = 14.sp)
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            names.size.toString(),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (isOwn) Spacer(Modifier.width(6.dp))
        }
    }


