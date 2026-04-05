package com.nueng.translator.ui.online.global

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val EMOJI_LIST = listOf(
    "\uD83D\uDE00","\uD83D\uDE01","\uD83D\uDE02","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE05",
    "\uD83D\uDE06","\uD83D\uDE07","\uD83D\uDE08","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE0B",
    "\uD83D\uDE0C","\uD83D\uDE0D","\uD83D\uDE0E","\uD83D\uDE0F","\uD83D\uDE10","\uD83D\uDE11",
    "\uD83D\uDE12","\uD83D\uDE13","\uD83D\uDE14","\uD83D\uDE15","\uD83D\uDE16","\uD83D\uDE17",
    "\uD83D\uDE18","\uD83D\uDE19","\uD83D\uDE1A","\uD83D\uDE1B","\uD83D\uDE1C","\uD83D\uDE1D",
    "\uD83D\uDE1E","\uD83D\uDE1F","\uD83D\uDE20","\uD83D\uDE21","\uD83D\uDE22","\uD83D\uDE23",
    "\uD83D\uDE24","\uD83D\uDE25","\uD83D\uDE26","\uD83D\uDE27","\uD83D\uDE28","\uD83D\uDE29",
    "\uD83D\uDE2A","\uD83D\uDE2B","\uD83D\uDE2C","\uD83D\uDE2D","\uD83D\uDE2E","\uD83D\uDE2F",
    "\uD83D\uDE30","\uD83D\uDE31","\uD83D\uDE32","\uD83D\uDE33","\uD83D\uDE34","\uD83D\uDE35",
    "\uD83D\uDE36","\uD83D\uDE37","\uD83E\uDD70","\uD83E\uDD73","\uD83E\uDD74","\uD83E\uDD75",
    "\uD83E\uDD76","\uD83E\uDD77","\uD83E\uDD78","\uD83E\uDD79","\uD83E\uDD7A","\uD83E\uDDD0",
    "\uD83D\uDC4D","\uD83D\uDC4E","\uD83D\uDC4F","\uD83D\uDC50","\uD83D\uDC4A","\uD83E\uDD1B",
    "\uD83E\uDD1C","\uD83E\uDD1E","\uD83E\uDD1F","\uD83D\uDC48","\uD83D\uDC49","\uD83D\uDC46",
    "\uD83D\uDD95","\uD83D\uDC47","\uD83D\uDD90","\uD83D\uDC4B","\uD83E\uDD19","\uD83D\uDC4C",
    "\uD83E\uDD0F","\uD83D\uDC8A","\uD83D\uDC8B","\uD83D\uDC95","\u2764\uFE0F","\uD83E\uDDE1",
    "\uD83D\uDC9B","\uD83D\uDC9A","\uD83D\uDC99","\uD83D\uDC96","\uD83D\uDC97","\uD83D\uDC98",
    "\uD83D\uDC9D","\uD83D\uDC9E","\uD83D\uDC94","\uD83D\uDCA5","\uD83D\uDCAF","\uD83D\uDD25",
    "\uD83C\uDF89","\uD83C\uDF88","\uD83C\uDF81","\uD83C\uDF82","\uD83C\uDF83","\uD83D\uDC7B",
    "\uD83D\uDC80","\uD83D\uDC7D","\uD83D\uDC7E","\uD83E\uDD16","\uD83D\uDCA9","\uD83D\uDC31",
    "\uD83D\uDC36","\uD83D\uDC37","\uD83D\uDC3C","\uD83D\uDC3B","\uD83D\uDC2F","\uD83E\uDD81",
    "\uD83C\uDF55","\uD83C\uDF54","\uD83C\uDF5F","\uD83C\uDF57","\uD83C\uDF63","\uD83C\uDF71",
    "\uD83C\uDF5C","\uD83C\uDF5D","\uD83C\uDF5E","\uD83C\uDF6E","\uD83C\uDF6F","\uD83C\uDF70",
    "\uD83C\uDF4E","\uD83C\uDF4F","\uD83C\uDF47","\uD83C\uDF49","\uD83C\uDF4A","\uD83C\uDF34",
    "\uD83C\uDF33","\uD83C\uDF31","\uD83C\uDF3A","\uD83C\uDF38","\uD83C\uDF37","\uD83C\uDF39",
    "\u26BD","\uD83C\uDFC0","\uD83C\uDFB8","\uD83C\uDFB9","\uD83C\uDFBA","\uD83C\uDFBB",
    "\uD83D\uDE80","\u2708\uFE0F","\uD83D\uDE97","\uD83D\uDE82","\uD83C\uDF0D","\uD83C\uDF1F",
    "\u2B50","\uD83C\uDF19","\u2600\uFE0F","\uD83C\uDF08","\u26A1","\u26C5"
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun OnlineGlobalScreen(
    modifier: Modifier = Modifier,
    onNavigateToProfile: (userId: String) -> Unit = {},
    onNavigateToForward: (text: String) -> Unit = {},
    viewModel: GlobalChatViewModel = hiltViewModel()
) {
    val uiState   by viewModel.uiState.collectAsState()
    val listState  = rememberLazyListState()
    var inputText  by remember { mutableStateOf(TextFieldValue("")) }
    val clipboard  = LocalClipboardManager.current

    var actionMsg              by remember { mutableStateOf<GlobalMsg?>(null) }
    var showDeleteDialog       by remember { mutableStateOf(false) }
    var deleteForEveryone      by remember { mutableStateOf(false) }
    var showEmojiPicker        by remember { mutableStateOf(false) }
    var showSelectDeleteDialog by remember { mutableStateOf(false) }
    var selectDeleteForAll     by remember { mutableStateOf(false) }
    var showHelpDialog         by remember { mutableStateOf(false) }
    var showFunDialog          by remember { mutableStateOf(false) }
    var showTableBuilder       by remember { mutableStateOf(false) }
    var tableCols              by remember { mutableIntStateOf(0) }
    var tableRows              by remember { mutableIntStateOf(0) }
    var tableCells             by remember { mutableStateOf(listOf<List<String>>()) }

    LaunchedEffect(uiState.editingMsg?.id) {
        val editing = uiState.editingMsg
        if (editing != null) inputText = TextFieldValue(editing.text, TextRange(editing.text.length))
    }
    LaunchedEffect(Unit) { viewModel.reattach() }
    DisposableEffect(Unit) { onDispose { viewModel.detach() } }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
            viewModel.markAllRead()
        }
    }
    LaunchedEffect(uiState.highlightedMsgId) {
        val hid = uiState.highlightedMsgId
        if (hid.isNotBlank()) {
            val idx = uiState.messages.indexOfFirst { it.id == hid }
            if (idx >= 0) listState.animateScrollToItem(idx)
        }
    }

    // -- Help dialog --
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("NuengChat Help", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HelpSection("Global Chat",
                        "Public chat wiped daily at 7 AM.\n" +
                        "Tap bubble -> action bar (Reply, Edit, Delete, Emoji, Copy, Forward, Select).\n" +
                        "Swipe bubble left/right -> quick reply.\n" +
                        "Tap Select -> multi-select messages."
                    )
                    HelpSection("Print Commands",
                        "Type these and tap Send:\n\n" +
                        "  table(col,row) or table(n) -> build a visual table\n" +
                        "  loop(n,msg) or repeat(n,msg) -> repeat msg n times (1-20)\n" +
                        "  fun() -> show all print commands\n" +
                        "  help() -> show this guide"
                    )
                    HelpSection("Auto-List Formatting",
                        "Start a line with a prefix + space then type:\n\n" +
                        "  1.  -> numbered list (auto-increments)\n" +
                        "  -   -> dash bullet list\n" +
                        "  *   -> star bullet list\n" +
                        "  a.  -> letter list (a b c ... z)\n" +
                        "  A.  -> uppercase letters (A B C ... Z)\n\n" +
                        "Enter on empty item -> cancel list."
                    )
                    HelpSection("Friends & Groups",
                        "OFriend tab: chat with friends, groups, yourself (My Notes) or AI bot.\n" +
                        "Add friends via the + icon using their username.\n" +
                        "Create groups via the group+ icon."
                    )
                    HelpSection("Forward",
                        "Tap Forward on any message -> ForwardScreen.\n" +
                        "Select one or more friends/groups -> Send."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got it!") }
            }
        )
    }

    // -- Fun / Print commands dialog --
    if (showFunDialog) {
        AlertDialog(
            onDismissRequest = { showFunDialog = false },
            title = { Text("Print Commands", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    HelpSection("table(col,row) or table(n)",
                        "Opens table builder.\n" +
                        "Row 1 = header. Fill cells then tap Send Table.\n\n" +
                        "  table(3,4) -> 3 columns, 4 rows\n" +
                        "  table(3)   -> 3x3 square table\n\n" +
                        "Max: 8 columns, 20 rows."
                    )
                    HelpSection("loop(n, msg) or repeat(n, msg)",
                        "Sends msg repeated n times on separate lines.\n" +
                        "Range: 1 to 20. Out of range -> plain text.\n\n" +
                        "  loop(3, Hello!)    -> 3 lines of Hello!\n" +
                        "  repeat(5, NuengAI) -> 5 lines\n\n" +
                        "Case-insensitive: Loop, LOOP, Repeat, REPEAT."
                    )
                    HelpSection("Auto-List",
                        "  1.  -> numbered (1. 2. 3. ...)\n" +
                        "  -   -> dash bullet\n" +
                        "  *   -> star bullet\n" +
                        "  a.  -> lowercase letters\n" +
                        "  A.  -> uppercase letters\n" +
                        "Enter on empty item -> cancel."
                    )
                    HelpSection("Meta Commands",
                        "  help() -> full NuengChat guide\n" +
                        "  fun()  -> this print commands list"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFunDialog = false }) { Text("Got it!") }
            }
        )
    }

    // -- Table builder dialog --
    if (showTableBuilder && tableCols > 0 && tableRows > 0) {
        AlertDialog(
            onDismissRequest = { showTableBuilder = false },
            title = { Text("Table ($tableCols x $tableRows)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Row 1 = header. Fill in your data:",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        val hScroll = rememberScrollState()
                        val vScroll = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(vScroll)) {
                            for (r in 0 until tableRows) {
                                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                                    for (c in 0 until tableCols) {
                                        val isHeader = r == 0
                                        OutlinedTextField(
                                            value = tableCells.getOrNull(r)?.getOrNull(c) ?: "",
                                            onValueChange = { newVal ->
                                                val updated = tableCells.mapIndexed { ri, row ->
                                                    row.mapIndexed { ci, cell ->
                                                        if (ri == r && ci == c) newVal else cell
                                                    }
                                                }
                                                tableCells = updated
                                            },
                                            modifier = Modifier.width(90.dp).padding(2.dp),
                                            singleLine = true,
                                            placeholder = {
                                                Text(if (isHeader) "H${c+1}" else "r${r}c${c+1}", fontSize = 10.sp)
                                            },
                                            label = if (isHeader) ({ Text("Header", fontSize = 9.sp) }) else null,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 12.sp,
                                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                                            ),
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
                    val jsonRows = JSONArray()
                    for (r in 0 until tableRows) {
                        val jsonRow = JSONArray()
                        for (c in 0 until tableCols) { jsonRow.put(tableCells.getOrNull(r)?.getOrNull(c) ?: "") }
                        jsonRows.put(jsonRow)
                    }
                    val jsonObj = JSONObject()
                    jsonObj.put("cols", tableCols)
                    jsonObj.put("rows", jsonRows)
                    viewModel.sendMessage("[TABLE]$jsonObj")
                    showTableBuilder = false
                    inputText = TextFieldValue("")
                }) { Text("Send Table", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTableBuilder = false }) { Text("Cancel") }
            }
        )
    }

    // -- SelectView delete dialog --
    if (showSelectDeleteDialog) {
        val count = uiState.selectedMsgIds.size
        AlertDialog(
            onDismissRequest = { showSelectDeleteDialog = false },
            title = { Text(if (selectDeleteForAll) "Delete for everyone?" else "Delete for you?") },
            text  = {
                Text(if (selectDeleteForAll) "$count message(s) will be deleted for all users."
                     else "$count message(s) will be hidden from your view only.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectDeleteForAll) viewModel.deleteSelectedForEveryone()
                    else viewModel.deleteSelectedForMe()
                    showSelectDeleteDialog = false
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSelectDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // -- Delete dialog --
    if (showDeleteDialog && actionMsg != null) {
        val msg = actionMsg!!
        val hasReplies = uiState.messages.any { it.replyToId == msg.id }
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; actionMsg = null },
            title = { Text(if (deleteForEveryone) "Delete for everyone?" else "Delete for you?") },
            text  = {
                Text(if (deleteForEveryone)
                    "This message" + (if (hasReplies) " and its replies" else "") + " will be deleted for all users."
                else "This message will be hidden from your view only.")
            },
            confirmButton = {
                TextButton(onClick = {
                    if (deleteForEveryone) viewModel.deleteForEveryone(msg) else viewModel.deleteForMe(msg)
                    showDeleteDialog = false; actionMsg = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false; actionMsg = null }) { Text("Cancel") }
            }
        )
    }

    // -- Emoji picker --
    if (showEmojiPicker && actionMsg != null) {
        val msg = actionMsg!!
        AlertDialog(
            onDismissRequest = { showEmojiPicker = false; actionMsg = null },
            title = { Text("React", fontSize = 15.sp) },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                        GlobalChatViewModel.QUICK_EMOJIS.forEach { emoji ->
                            Text(emoji, fontSize = 26.sp, modifier = Modifier.clickable {
                                viewModel.toggleReaction(msg, emoji)
                                showEmojiPicker = false; actionMsg = null
                            })
                        }
                    }
                    LazyRow(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                        items(EMOJI_LIST.chunked(6)) { col ->
                            Column {
                                col.forEach { emoji ->
                                    Text(emoji, fontSize = 22.sp,
                                        modifier = Modifier.padding(3.dp).clickable {
                                            viewModel.toggleReaction(msg, emoji)
                                            showEmojiPicker = false; actionMsg = null
                                        })
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEmojiPicker = false; actionMsg = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // -- Header --
        if (uiState.isSelectMode) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.exitSelectMode() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("${uiState.selectedMsgIds.size} selected", fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val joined = viewModel.getSelectedMessages()
                        .joinToString("\n") { "${it.senderName} : ${it.text}" }
                    clipboard.setText(AnnotatedString(joined))
                    viewModel.exitSelectMode()
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                IconButton(
                    onClick = {
                        val joined = viewModel.getSelectedMessages()
                            .joinToString("\n") { "${it.senderName} : ${it.text}" }
                        onNavigateToForward(joined)
                        viewModel.exitSelectMode()
                    },
                    enabled = uiState.selectedMsgIds.isNotEmpty()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Forward, "Forward",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f))
                }
                IconButton(
                    onClick = {
                        selectDeleteForAll = viewModel.canDeleteSelectedForEveryone()
                        showSelectDeleteDialog = true
                    },
                    enabled = uiState.selectedMsgIds.isNotEmpty()
                ) {
                    Icon(Icons.Default.Delete, "Delete",
                        tint = if (uiState.selectedMsgIds.isNotEmpty()) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Public, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Global Chat", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (uiState.isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (uiState.isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (uiState.isConnected) "Online" else "Offline",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Messages are wiped daily at 7 AM", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
        }

        // -- Messages --
        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
        } else if (uiState.messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Public, null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No messages yet.", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Be the first to say hello!", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                items(items = uiState.messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { msg ->
                    GlobalChatBubble(
                        msg             = msg,
                        isHighlighted   = actionMsg?.id == msg.id || uiState.editingMsg?.id == msg.id,
                        isReplied       = uiState.highlightedMsgId == msg.id,
                        isSelected      = msg.id in uiState.selectedMsgIds,
                        isSelectMode    = uiState.isSelectMode,
                        onTapAvatar     = { if (!msg.isOwn && !uiState.isSelectMode) onNavigateToProfile(msg.senderId) },
                        onSwipeReply    = { if (!uiState.isSelectMode) viewModel.setReplyTo(msg) },
                        onReplyTap      = { id -> viewModel.highlightMsg(id) },
                        onTap           = {
                            if (uiState.isSelectMode) viewModel.toggleSelect(msg)
                            else actionMsg = if (actionMsg?.id == msg.id) null else msg
                        },
                        onReactionTap   = { emoji -> viewModel.toggleReaction(msg, emoji) },
                        onReply         = { viewModel.setReplyTo(msg); actionMsg = null },
                        onEdit          = { viewModel.startEdit(msg); actionMsg = null },
                        onDeleteForMe   = { deleteForEveryone = false; showDeleteDialog = true },
                        onDeleteForAll  = { deleteForEveryone = true; showDeleteDialog = true },
                        onEmoji         = { showEmojiPicker = true },
                        onCopy          = { clipboard.setText(AnnotatedString(msg.text)); actionMsg = null },
                        onForward       = { if (!msg.isDeleted) { onNavigateToForward(msg.text); actionMsg = null } },
                        onSelect        = { viewModel.enterSelectMode(msg); actionMsg = null },
                        canEdit         = viewModel.canEdit(msg),
                        canDeleteForAll = viewModel.canDeleteForEveryone(msg)
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        // -- Reply bar --
        AnimatedVisibility(
            visible = uiState.replyingTo != null,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            uiState.replyingTo?.let { reply ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reply.senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Text(reply.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.clearReply() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // -- Edit banner --
        AnimatedVisibility(
            visible = uiState.editingMsg != null,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editing message", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.cancelEdit(); inputText = TextFieldValue("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // -- Bottom section (input bar) --
        if (!uiState.isSelectMode) Column(modifier = Modifier.fillMaxWidth()) {
            // Emoji Panel
            var showGlcEmojiPanel  by remember { mutableStateOf(false) }
            var glcSelectingFav    by remember { mutableStateOf(false) }
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val glcScope           = rememberCoroutineScope()
            AnimatedVisibility(
                visible = showGlcEmojiPanel,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .verticalScroll(rememberScrollState())
                    .padding(4.dp)
                ) {
                    if (glcSelectingFav) {
                        Text("Tap to set as favourite ⭐", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                    }
                    val emojiRows = GLC_EMOJI_LIST.chunked(8)
                    emojiRows.forEach { rowEmojis ->
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowEmojis.forEach { emoji ->
                                Box(modifier = Modifier.size(40.dp)
                                    .combinedClickable(
                                        onClick = {
                                            if (glcSelectingFav) {
                                                viewModel.setFavEmoji(emoji)
                                                glcSelectingFav = false
                                                showGlcEmojiPanel = false
                                            } else {
                                                inputText = TextFieldValue(
                                                    inputText.text + emoji,
                                                    TextRange(inputText.text.length + emoji.length))
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.setFavEmoji(emoji)
                                            glcSelectingFav = false
                                            showGlcEmojiPanel = false
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
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { newVal ->
                        val autoResult = applyAutoList(inputText, newVal)
                        inputText = if (autoResult.text != newVal.text) autoResult else newVal
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(if (uiState.editingMsg != null) "Edit your message..."
                             else "Say something to the world...", fontSize = 13.sp)
                    },
                    singleLine  = false,
                    maxLines    = 5,
                    shape       = RoundedCornerShape(16.dp)
                )
                // Emoji/Keyboard toggle
                IconButton(onClick = {
                    if (showGlcEmojiPanel) {
                        showGlcEmojiPanel = false
                        glcSelectingFav   = false
                        glcScope.launch {
                            kotlinx.coroutines.delay(100)
                            keyboardController?.show()
                        }
                    } else {
                        showGlcEmojiPanel = true
                        keyboardController?.hide()
                    }
                }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (showGlcEmojiPanel) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                        if (showGlcEmojiPanel) "Keyboard" else "Emoji",
                        tint = if (showGlcEmojiPanel) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Fav emoji
                Box(modifier = Modifier.size(36.dp).clip(CircleShape)
                    .combinedClickable(
                        onClick     = { viewModel.sendMessage(uiState.favEmoji) },
                        onLongClick = {
                            glcSelectingFav = true
                            showGlcEmojiPanel = true
                            keyboardController?.hide()
                        }
                    ),
                    contentAlignment = Alignment.Center) {
                    Text(uiState.favEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                    color = if (inputText.text.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = {
                        val raw = inputText.text.trim()
                        // table(col,row) or table(n)
                        val tableMatch2 = Regex("^[Tt][Aa][Bb][Ll][Ee]\\((\\d+),(\\d+)\\)$").matchEntire(raw)
                        val tableMatch1 = Regex("^[Tt][Aa][Bb][Ll][Ee]\\((\\d+)\\)$").matchEntire(raw)
                        if (tableMatch2 != null || tableMatch1 != null) {
                            val c: Int; val r: Int
                            if (tableMatch2 != null) {
                                c = tableMatch2.groupValues[1].toIntOrNull()?.coerceIn(1, 8) ?: 2
                                r = tableMatch2.groupValues[2].toIntOrNull()?.coerceIn(1, 20) ?: 2
                            } else {
                                val n = tableMatch1!!.groupValues[1].toIntOrNull()?.coerceIn(1, 8) ?: 2
                                c = n; r = n
                            }
                            tableCols = c; tableRows = r
                            tableCells = List(r) { List(c) { "" } }
                            showTableBuilder = true; inputText = TextFieldValue("")
                        } else {
                            // help() / fun()
                            if (raw.equals("help()", ignoreCase = true)) {
                                showHelpDialog = true; inputText = TextFieldValue("")
                            } else if (raw.equals("fun()", ignoreCase = true)) {
                                showFunDialog = true; inputText = TextFieldValue("")
                            } else {
                                // loop(n,msg) / repeat(n,msg)
                                val loopMatch = Regex(
                                    "^(?:loop|repeat)\\(\\s*(\\d+)\\s*,\\s*(.+)\\)$",
                                    RegexOption.IGNORE_CASE
                                ).matchEntire(raw)
                                if (loopMatch != null) {
                                    val times = loopMatch.groupValues[1].toIntOrNull() ?: 0
                                    val msg   = loopMatch.groupValues[2].trim()
                                    if (times in 1..20) {
                                        viewModel.sendMessage((1..times).joinToString("\n") { msg })
                                    } else {
                                        viewModel.sendMessage(raw)
                                    }
                                    inputText = TextFieldValue("")
                                } else if (raw.isNotBlank()) {
                                    viewModel.sendMessage(raw)
                                    inputText = TextFieldValue("")
                                }
                            }
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (uiState.editingMsg != null) Icons.Default.Edit else Icons.AutoMirrored.Filled.Send,
                            "Send",
                            tint = if (inputText.text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } // end bottom section
    }
}

// Shared emoji list
private val GLC_EMOJI_LIST = listOf(
    "\uD83D\uDE00","\uD83D\uDE01","\uD83D\uDE02","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE05","\uD83D\uDE06","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE0B","\uD83D\uDE0C","\uD83D\uDE0D","\uD83D\uDE0E","\uD83D\uDE0F","\uD83D\uDE10","\uD83D\uDE11",
    "\uD83D\uDE12","\uD83D\uDE13","\uD83D\uDE14","\uD83D\uDE15","\uD83D\uDE16","\uD83D\uDE17","\uD83D\uDE18","\uD83D\uDE19","\uD83D\uDE1A","\uD83D\uDE1B","\uD83D\uDE1C","\uD83D\uDE1D","\uD83D\uDE1E","\uD83D\uDE1F","\uD83D\uDE20","\uD83D\uDE21",
    "\uD83D\uDE22","\uD83D\uDE28","\uD83D\uDE2D","\uD83D\uDE31","\uD83D\uDE33","\uD83D\uDE35",
    "\uD83D\uDC4D","\uD83D\uDC4E","\uD83D\uDC4F","\uD83D\uDC4A","\uD83D\uDC4B","\uD83D\uDC4C",
    "\u2764\uFE0F","\uD83D\uDC95","\uD83D\uDC99","\uD83D\uDC9A","\uD83D\uDC9B","\uD83D\uDCA5",
    "\uD83D\uDE80","\u2708\uFE0F","\uD83C\uDF89","\uD83C\uDF81","\uD83C\uDF55","\uD83C\uDF54",
    "\uD83D\uDC31","\uD83D\uDC36","\uD83D\uDC3C","\uD83D\uDCA9","\uD83E\uDD16","\uD83D\uDC7B",
    "\u26BD","\uD83C\uDFC0","\uD83C\uDF1F","\u2B50","\uD83C\uDF08","\u26A1"
)

// -- Auto-list helper --
private fun applyAutoList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val newText = new.text
    val oldText = old.text
    if (!newText.endsWith("\n") || newText.length <= oldText.length) return new
    val textBeforeNewline = newText.dropLast(1)
    val lastLine = textBeforeNewline.substringAfterLast("\n", textBeforeNewline)
    // Numbered list
    val dotSpaceIdx = lastLine.indexOf(". ")
    if (dotSpaceIdx > 0) {
        val maybeNum = lastLine.substring(0, dotSpaceIdx)
        if (maybeNum.isNotEmpty() && maybeNum.all { it.isDigit() }) {
            val content = lastLine.substring(dotSpaceIdx + 2)
            if (content.isEmpty()) {
                val result = textBeforeNewline.dropLast(lastLine.length)
                return TextFieldValue(result, TextRange(result.length))
            } else {
                val nextNum = (maybeNum.toIntOrNull() ?: 0) + 1
                val result = "$newText$nextNum. "
                return TextFieldValue(result, TextRange(result.length))
            }
        }
    }
    // Bullet list
    if (lastLine == "- " || lastLine == "* ") {
        val result = textBeforeNewline.dropLast(lastLine.length)
        return TextFieldValue(result, TextRange(result.length))
    }
    if (lastLine.startsWith("- ") && lastLine.length > 2) {
        val result = "$newText- "
        return TextFieldValue(result, TextRange(result.length))
    }
    if (lastLine.startsWith("* ") && lastLine.length > 2) {
        val result = "$newText* "
        return TextFieldValue(result, TextRange(result.length))
    }
    // Letter list
    val letterEmpty  = Regex("""^[a-zA-Z]\. $""")
    val letterActive = Regex("""^([a-zA-Z])\. (.+)$""")
    if (letterEmpty.matches(lastLine)) {
        val result = textBeforeNewline.dropLast(lastLine.length)
        return TextFieldValue(result, TextRange(result.length))
    }
    val lm = letterActive.matchEntire(lastLine)
    if (lm != null) {
        val ch = lm.groupValues[1].first()
        val next = when (ch) {
            'z' -> 'z'
            'Z' -> 'Z'
            else -> ch + 1
        }
        val result = "$newText$next. "
        return TextFieldValue(result, TextRange(result.length))
    }
    return TextFieldValue(newText, TextRange(newText.length))
}

// -- Parse table JSON (outside composable) --
private fun parseTableJson(jsonText: String): Pair<Int, List<List<String>>>? {
    return try {
        val json     = JSONObject(jsonText)
        val cols     = json.getInt("cols")
        val jsonRows = json.getJSONArray("rows")
        val rows     = (0 until jsonRows.length()).map { r ->
            val jsonRow = jsonRows.getJSONArray(r)
            (0 until cols).map { c -> jsonRow.optString(c, "") }
        }
        if (cols > 0 && rows.isNotEmpty()) Pair(cols, rows) else null
    } catch (_: Exception) { null }
}

@Composable
private fun TableBubbleContent(jsonText: String, isOwn: Boolean) {
    val parsed = remember(jsonText) { parseTableJson(jsonText) }
    if (parsed == null) {
        Text(jsonText, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        return
    }
    val (cols, rows) = parsed
    val rowCount  = rows.size
    val headerBg  = if (isOwn) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary
    val headerFg  = MaterialTheme.colorScheme.onPrimary
    val cellBg    = if (isOwn) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant
    val cellBgAlt = if (isOwn) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
    val cellFg    = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderClr = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val hScroll = rememberScrollState()
    Column(modifier = Modifier.horizontalScroll(hScroll).padding(4.dp)) {
        for (r in 0 until rowCount) {
            val row      = rows[r]
            val isHeader = r == 0
            Row {
                for (c in 0 until cols) {
                    val cell = row.getOrElse(c) { "" }
                    Box(
                        modifier = Modifier
                            .border(0.5.dp, borderClr)
                            .background(when { isHeader -> headerBg; r % 2 == 1 -> cellBg; else -> cellBgAlt })
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .widthIn(min = 48.dp, max = 120.dp)
                    ) {
                        Text(cell, fontSize = 12.sp,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            color = if (isHeader) headerFg else cellFg,
                            maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(body, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp)
    }
}

// -- Bubble --
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlobalChatBubble(
    msg: GlobalMsg,
    isHighlighted: Boolean,
    isReplied: Boolean = false,
    isSelected: Boolean,
    isSelectMode: Boolean,
    onTapAvatar: () -> Unit,
    onSwipeReply: () -> Unit,
    onReplyTap: (String) -> Unit = {},
    onTap: () -> Unit,
    onReactionTap: (String) -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForAll: () -> Unit,
    onEmoji: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onSelect: () -> Unit,
    canEdit: Boolean,
    canDeleteForAll: Boolean
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val scope      = rememberCoroutineScope()
    val offsetX    = remember { Animatable(0f) }
    var tapped     by remember { mutableStateOf(false) }

    LaunchedEffect(tapped) {
        if (tapped) { kotlinx.coroutines.delay(160); tapped = false }
    }

    val bubbleBg = when {
        msg.isDeleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        tapped        -> if (msg.isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        msg.isOwn     -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else          -> MaterialTheme.colorScheme.surfaceVariant
    }
    val bubbleShape = RoundedCornerShape(
        topStart    = if (msg.replyToId.isNotBlank() && !msg.isDeleted) 4.dp else 16.dp,
        topEnd      = if (msg.replyToId.isNotBlank() && !msg.isDeleted) 4.dp else 16.dp,
        bottomStart = if (msg.isOwn) 16.dp else 4.dp,
        bottomEnd   = if (msg.isOwn) 4.dp else 16.dp
    )
    val goldColor = Color(0xFFFFD700)
    val pulseAnim = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isReplied) {
        if (isReplied) {
            repeat(4) {
                pulseAnim.animateTo(1f, androidx.compose.animation.core.tween(250))
                pulseAnim.animateTo(0f, androidx.compose.animation.core.tween(250))
            }
        } else { pulseAnim.snapTo(0f) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Action bar
        if (isHighlighted && !isReplied && !msg.isDeleted && !isSelectMode) {
            Row(
                modifier = Modifier
                    .then(if (msg.isOwn) Modifier.align(Alignment.End) else Modifier.align(Alignment.Start))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                MsgActionIcon(Icons.AutoMirrored.Filled.Reply, "Reply", onClick = onReply)
                if (canEdit) MsgActionIcon(Icons.Default.Edit, "Edit", onClick = onEdit)
                MsgActionIcon(
                    icon    = Icons.Default.Delete,
                    label   = "Delete",
                    tint    = MaterialTheme.colorScheme.error,
                    onClick = { if (canDeleteForAll) onDeleteForAll() else onDeleteForMe() }
                )
                MsgActionIcon(Icons.Default.EmojiEmotions, "Emoji", onClick = onEmoji)
                MsgActionIcon(Icons.Default.ContentCopy, "Copy",  onClick = onCopy)
                MsgActionIcon(Icons.AutoMirrored.Filled.Forward, "Forward",   onClick = { if (!msg.isDeleted) onForward() })
                MsgActionIcon(Icons.Default.CheckBox, "Select",   onClick = onClick@{ onSelect(); return@onClick })
            }
        }

        // Row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .then(when {
                    isSelected    -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    isHighlighted && !isReplied -> Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                    else          -> Modifier
                }),
            horizontalArrangement = if (msg.isOwn) Arrangement.End else Arrangement.Start,
            verticalAlignment     = Alignment.Bottom
        ) {
            if (!msg.isOwn) {
                Box {
                    AvatarCircle(letter = msg.senderAvatarLetter, sizeDp = 28, username = msg.senderId,
                        modifier = Modifier.clickable { onTapAvatar() })
                    if (isSelectMode) {
                        Icon(
                            if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp).align(Alignment.BottomEnd)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(horizontalAlignment = if (msg.isOwn) Alignment.End else Alignment.Start) {
                if (!msg.isOwn) {
                    Text(msg.senderName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                }
                // Forwarded label
                if (msg.isForwarded && !msg.isDeleted) {
                    Text("Forwarded", fontSize = 10.sp, fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                }
                // Reply preview
                if (msg.replyToId.isNotBlank() && !msg.isDeleted) {
                    // replyToText shows preview, tap scrolls to original
                    Surface(
                        modifier = Modifier.widthIn(max = 260.dp).padding(bottom = 2.dp)
                            .clickable { onReplyTap(msg.replyToId) },
                        color    = if (msg.isOwn) MaterialTheme.colorScheme.primary.copy(0.55f)
                                   else MaterialTheme.colorScheme.surfaceVariant.copy(0.7f),
                        shape    = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(msg.replyToSender, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            Text(msg.replyToText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary.copy(0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Swipeable bubble
                Box(
                    modifier = Modifier
                        .then(if (msg.text.startsWith("[TABLE]")) Modifier.wrapContentWidth() else Modifier.widthIn(max = 260.dp))
                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        .pointerInput(msg.id) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (abs(offsetX.value) > 60f) onSwipeReply()
                                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                },
                                onHorizontalDrag = { _, drag ->
                                    scope.launch { offsetX.snapTo((offsetX.value + drag).coerceIn(-80f, 80f)) }
                                }
                            )
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .then(if (msg.text.startsWith("[TABLE]")) Modifier.wrapContentWidth() else Modifier.widthIn(max = 260.dp))
                            .then(if (isReplied && pulseAnim.value > 0f) Modifier.border(2.dp, goldColor.copy(alpha = pulseAnim.value), bubbleShape) else Modifier)
                            .clickable { tapped = true; onTap() },
                        color = bubbleBg,
                        shape = bubbleShape
                    ) {
                        if (msg.isDeleted) {
                            Text(msg.deletedBy.ifBlank { "Deleted" }, fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        } else if (msg.text.startsWith("[TABLE]")) {
                            TableBubbleContent(jsonText = msg.text.removePrefix("[TABLE]"), isOwn = msg.isOwn)
                        } else {
                            Text(msg.text, fontSize = 14.sp,
                                color = if (msg.isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                        }
                    }
                }
                // Reactions
                if (!msg.isDeleted && msg.reactions.isNotEmpty()) {
                    FlowRow(modifier = Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        msg.reactions.forEach { (emoji, names) ->
                            Surface(modifier = Modifier.clickable { onReactionTap(emoji) },
                                shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(emoji, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(names.size.toString(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                // Timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (msg.isEdited && !msg.isDeleted) {
                        Text("edited", fontSize = 9.sp, fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 4.dp, end = 2.dp))
                    }
                    if (msg.timestamp > 0) {
                        Text(timeFormat.format(Date(msg.timestamp)), fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                    }
                }
            }
            if (msg.isOwn) {
                if (isSelectMode) {
                    Icon(
                        if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(start = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Composable
private fun MsgActionIcon(
    icon: ImageVector,
    label: String,
    tint: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, label, modifier = Modifier.size(17.dp),
            tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint)
    }
}
