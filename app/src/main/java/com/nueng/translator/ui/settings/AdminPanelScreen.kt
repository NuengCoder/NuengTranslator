package com.nueng.translator.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.data.model.FirebaseUser
import com.nueng.translator.data.repository.LanguageWordRepository
import com.nueng.translator.ui.components.LanguageSelectorRow
import com.nueng.translator.ui.translate.EditLanguageWordDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AquaColor = Color(0xFF00BCD4)
private val DarkBlueColor = Color(0xFF0A1929)
private val DarkBlueSurface = Color(0xFF0D47A1)

private val WORD_TYPES = listOf(
    "" to "None", "n." to "n. (Noun)", "pn." to "pn. (Pronoun)",
    "v." to "v. (Verb)", "adj." to "adj. (Adjective)", "adv." to "adv. (Adverb)",
    "prep." to "prep. (Preposition)", "conj." to "conj. (Conjunction)",
    "interj." to "interj. (Interjection)", "det." to "det. (Determiner)",
    "num." to "num. (Number)", "mw." to "mw. (Measure Word)",
    "phr." to "phr. (Phrase)", "idiom" to "idiom (Idiom)"
)

private val tfColors @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White, unfocusedTextColor = Color.White.copy(0.8f),
    focusedBorderColor = AquaColor, unfocusedBorderColor = AquaColor.copy(0.3f),
    focusedLabelColor = AquaColor, unfocusedLabelColor = Color.White.copy(0.5f),
    cursorColor = AquaColor, focusedContainerColor = DarkBlueColor, unfocusedContainerColor = DarkBlueColor
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    adminUserId: Long,
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.loadData() }
    LaunchedEffect(uiState.addWordSuccess) {
        if (uiState.addWordSuccess) { snackbarHostState.showSnackbar("Word added!"); viewModel.clearAddWordSuccess() }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    val tabs = listOf(
        AdminTab("Info", Icons.Default.Storage),
        AdminTab("Users", Icons.Default.People),
        AdminTab("Add Word", Icons.Default.Add),
        AdminTab("Words", Icons.Default.Build)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = AquaColor, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp)); Text("Admin Panel", color = Color.White)
                } },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AquaColor) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlueSurface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DarkBlueSurface) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        icon = { Icon(tab.icon, tab.label, tint = if (selectedTab == i) AquaColor else Color.White.copy(0.5f)) },
                        label = { Text(tab.label, fontSize = 10.sp, color = if (selectedTab == i) AquaColor else Color.White.copy(0.5f)) },
                        selected = selectedTab == i, onClick = { selectedTab = i }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBlueColor
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                0 -> InfoTab(uiState)
                1 -> UsersTab(uiState.users)
                2 -> AddWordTab(adminUserId, viewModel)
                3 -> ControlWordsTab(viewModel)
            }
        }
    }
}

private data class AdminTab(val label: String, val icon: ImageVector)

// ===================== TAB 0: INFO =====================
@Composable
private fun InfoTab(uiState: AdminUiState) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Default.Construction, null, tint = AquaColor, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("What to control, Master?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AquaColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        InfoCard("Total Users", "${uiState.totalUsers}")
        InfoCard("Online Users (5min)", "${uiState.onlineUsers}")
        InfoCard("Total Words", "${uiState.wordCount}")
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).border(1.dp, AquaColor.copy(0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkBlueSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 14.sp, color = Color.White.copy(0.7f), modifier = Modifier.weight(1f))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AquaColor, textAlign = TextAlign.End)
        }
    }
}

// ===================== TAB 1: USERS =====================
@Composable
private fun UsersTab(users: List<FirebaseUser>) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filtered = if (searchQuery.isBlank()) users.take(5) else users.filter { it.username.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(searchQuery, { searchQuery = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Search username...", color = Color.White.copy(0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = AquaColor) }, singleLine = true, colors = tfColors)
        Spacer(Modifier.height(8.dp))
        if (searchQuery.isBlank()) {
            Text("Showing first 5 users. Search to find more.", fontSize = 12.sp, color = Color.White.copy(0.4f))
            Spacer(Modifier.height(4.dp))
        }
        LazyColumn {
            items(filtered) { user -> UserCard(user) }
            if (filtered.isEmpty()) { item { Text("No users found", color = Color.White.copy(0.5f), modifier = Modifier.padding(16.dp)) } }
        }
    }
}

@Composable
private fun UserCard(user: FirebaseUser) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val isOnline = (System.currentTimeMillis() - user.lastOnline) < 5 * 60 * 1000
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).border(1.dp, AquaColor.copy(0.2f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkBlueSurface), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    if (user.role == "admin") Text("ADMIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaColor)
                    if (user.role == "guest") Text("GUEST", fontSize = 10.sp, color = Color.White.copy(0.4f))
                }
                Text("ID: ${user.username}", fontSize = 11.sp, color = Color.White.copy(0.4f))
                Text("Last: ${dateFormat.format(Date(user.lastOnline))}", fontSize = 11.sp, color = Color.White.copy(0.4f))
            }
            Surface(Modifier.size(10.dp), shape = RoundedCornerShape(5.dp),
                color = if (isOnline) Color(0xFF4CAF50) else Color(0xFF666666)) {}
        }
    }
}

// ===================== TAB 2: ADD WORD =====================
@Composable
private fun AddWordTab(adminUserId: Long, viewModel: AdminPanelViewModel) {
    var word by rememberSaveable { mutableStateOf("") }
    var wordType by rememberSaveable { mutableStateOf("") }
    var pinyin by rememberSaveable { mutableStateOf("") }
    var translation by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    var translationExample by rememberSaveable { mutableStateOf("") }
    var lang1 by rememberSaveable { mutableStateOf("en") }
    var lang2 by rememberSaveable { mutableStateOf("zh") }
    var showTypeMenu by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            LanguageSelectorRow(lang1, lang2, { lang1 = it }, { lang2 = it }, { val t = lang1; lang1 = lang2; lang2 = t })
            Spacer(Modifier.height(8.dp))
            Box {
                Surface(Modifier.fillMaxWidth().clickable { showTypeMenu = true }.border(1.dp, AquaColor.copy(0.3f), RoundedCornerShape(4.dp)), color = DarkBlueColor) {
                    Text(if (wordType.isBlank()) "Word Type (optional)" else WORD_TYPES.find { it.first == wordType }?.second ?: wordType,
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp), fontSize = 14.sp,
                        color = if (wordType.isBlank()) Color.White.copy(0.5f) else Color.White)
                }
                DropdownMenu(showTypeMenu, { showTypeMenu = false }) {
                    WORD_TYPES.forEach { (code, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { wordType = code; showTypeMenu = false }) }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(word, { word = it }, label = { Text("Word") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(pinyin, { pinyin = it }, label = { Text("Pinyin") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(translation, { translation = it }, label = { Text("Translation") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(example, { example = it }, label = { Text("Example Sentence") }, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(translationExample, { translationExample = it }, label = { Text("Translation Example") }, modifier = Modifier.fillMaxWidth(), colors = tfColors)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                viewModel.addWord(word, pinyin, lang1, translation, lang2, example, translationExample, adminUserId, wordType)
                word = ""; wordType = ""; pinyin = ""; translation = ""; example = ""; translationExample = ""
            }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AquaColor)) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Add to Language Table", color = DarkBlueColor)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ===================== TAB 3: CONTROL WORDS =====================
@Composable
private fun ControlWordsTab(viewModel: AdminPanelViewModel) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var lang1 by rememberSaveable { mutableStateOf("en") }
    var lang2 by rememberSaveable { mutableStateOf("zh") }
    var editingWord by remember { mutableStateOf<LanguageWord?>(null) }
    var deletingWord by remember { mutableStateOf<LanguageWord?>(null) }

    // Observe words from LanguageWordRepository via ViewModel
    val words by viewModel.getWordsFlow(searchQuery, lang1, lang2).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        LanguageSelectorRow(lang1, lang2, { lang1 = it }, { lang2 = it }, { val t = lang1; lang1 = lang2; lang2 = t })

        OutlinedTextField(searchQuery, { searchQuery = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("Search words...", color = Color.White.copy(0.5f)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = AquaColor) }, singleLine = true, colors = tfColors)
        Spacer(Modifier.height(8.dp))
        Text("Edit or delete words from the Language Table", fontSize = 12.sp, color = Color.White.copy(0.4f))
        Spacer(Modifier.height(4.dp))

        LazyColumn {
            items(items = words, key = { it.id }) { word ->
                AdminWordCard(word = word, onEdit = { editingWord = it }, onDelete = { deletingWord = it })
            }
            if (words.isEmpty()) {
                item {
                    Text(if (searchQuery.isBlank()) "Select language pair to browse" else "No words found",
                        color = Color.White.copy(0.5f), modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    editingWord?.let { word ->
        EditLanguageWordDialog(word = word, onDismiss = { editingWord = null },
            onSave = { updated -> viewModel.updateWord(updated); editingWord = null })
    }

    deletingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { deletingWord = null },
            title = { Text("Delete Word?") },
            text = { Text("Delete " + word.word + " from the Language Table?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteWord(word); deletingWord = null }) {
                Text("Delete", color = Color(0xFFEF5350)) } },
            dismissButton = { TextButton(onClick = { deletingWord = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AdminWordCard(word: LanguageWord, onEdit: (LanguageWord) -> Unit, onDelete: (LanguageWord) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).border(1.dp, AquaColor.copy(0.3f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkBlueSurface), shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(start = 16.dp, top = 10.dp, end = 4.dp, bottom = 10.dp)) {
            if (word.wordType.isNotBlank()) {
                Text(word.wordType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaColor, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(2.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word.word, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        if (word.pinyin.isNotBlank()) { Spacer(Modifier.width(6.dp)); Text(word.pinyin, fontSize = 12.sp, color = Color.White.copy(0.5f)) }
                    }
                    Text(word.translation, fontSize = 14.sp, color = Color.White.copy(0.7f))
                }
                Row {
                    IconButton(onClick = { onEdit(word) }, Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, "Edit", Modifier.size(16.dp), tint = AquaColor)
                    }
                    IconButton(onClick = { onDelete(word) }, Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = Color(0xFFEF5350))
                    }
                }
            }
        }
    }
}
