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
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.data.local.entity.User
import com.nueng.translator.ui.components.LanguageSelectorRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AquaColor = Color(0xFF00BCD4)
private val DarkBlueColor = Color(0xFF0A1929)
private val DarkBlueSurface = Color(0xFF0D47A1)

private val WORD_TYPES = listOf(
    "" to "None",
    "n." to "n. (Noun)",
    "pn." to "pn. (Pronoun)",
    "v." to "v. (Verb)",
    "adj." to "adj. (Adjective)",
    "adv." to "adv. (Adverb)",
    "prep." to "prep. (Preposition)",
    "conj." to "conj. (Conjunction)",
    "interj." to "interj. (Interjection)",
    "det." to "det. (Determiner)",
    "num." to "num. (Number)",
    "mw." to "mw. (Measure Word)",
    "phr." to "phr. (Phrase)",
    "idiom" to "idiom (Idiom)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onNavigateBack: () -> Unit,
    adminUserId: Long,
    userDao: UserDao,
    viewModel: AdminPanelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val users = userDao.getAllUsers()
        viewModel.refreshUsers(users)
    }

    LaunchedEffect(uiState.addWordSuccess) {
        if (uiState.addWordSuccess) {
            snackbarHostState.showSnackbar("Word added to Language Table!")
            viewModel.clearAddWordSuccess()
            val users = userDao.getAllUsers()
            viewModel.refreshUsers(users)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = AquaColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Panel", color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AquaColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlueSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBlueColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            item {
                AdminSectionHeader(
                    icon = Icons.Default.People,
                    title = "All Users (${uiState.users.size})"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.users) { user ->
                UserRow(user = user)
            }

            if (uiState.users.isEmpty()) {
                item {
                    Text(
                        "No users found",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                AdminSectionHeader(
                    icon = Icons.Default.Add,
                    title = "Add Language Word (${uiState.wordCount} total)"
                )
                Spacer(modifier = Modifier.height(8.dp))
                AddWordForm(
                    adminUserId = adminUserId,
                    onAddWord = { word, wordType, pinyin, l1, translation, l2, ex, trEx ->
                        viewModel.addWord(word, pinyin, l1, translation, l2, ex, trEx, adminUserId, wordType)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                AdminSectionHeader(
                    icon = Icons.Default.Construction,
                    title = "Other"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AquaColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkBlueSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Coming Soon",
                        modifier = Modifier.padding(20.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun AdminSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = AquaColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AquaColor)
    }
}

@Composable
private fun UserRow(user: User) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val lastOnline = dateFormat.format(Date(user.lastOnline))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(1.dp, AquaColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkBlueSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.username, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    if (user.role == "admin") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ADMIN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AquaColor)
                    }
                    if (user.role == "guest") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GUEST", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
                    }
                }
                Text("Last online: $lastOnline", fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun AddWordForm(
    adminUserId: Long,
    onAddWord: (String, String, String, String, String, String, String, String) -> Unit
) {
    var word by rememberSaveable { mutableStateOf("") }
    var wordType by rememberSaveable { mutableStateOf("") }
    var pinyin by rememberSaveable { mutableStateOf("") }
    var translation by rememberSaveable { mutableStateOf("") }
    var example by rememberSaveable { mutableStateOf("") }
    var translationExample by rememberSaveable { mutableStateOf("") }
    var lang1 by rememberSaveable { mutableStateOf("en") }
    var lang2 by rememberSaveable { mutableStateOf("zh") }
    var showTypeMenu by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White.copy(alpha = 0.8f),
        focusedBorderColor = AquaColor,
        unfocusedBorderColor = AquaColor.copy(alpha = 0.3f),
        focusedLabelColor = AquaColor,
        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
        cursorColor = AquaColor,
        focusedContainerColor = DarkBlueColor,
        unfocusedContainerColor = DarkBlueColor
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AquaColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkBlueSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LanguageSelectorRow(
                lang1 = lang1,
                lang2 = lang2,
                onLang1Change = { lang1 = it },
                onLang2Change = { lang2 = it },
                onSwap = {
                    val temp = lang1
                    lang1 = lang2
                    lang2 = temp
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Word Type selector
            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTypeMenu = true }
                        .border(1.dp, AquaColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
                    color = DarkBlueColor
                ) {
                    Text(
                        text = if (wordType.isBlank()) "Word Type (optional)" else WORD_TYPES.find { it.first == wordType }?.second ?: wordType,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        fontSize = 14.sp,
                        color = if (wordType.isBlank()) Color.White.copy(alpha = 0.5f) else Color.White
                    )
                }
                DropdownMenu(
                    expanded = showTypeMenu,
                    onDismissRequest = { showTypeMenu = false }
                ) {
                    WORD_TYPES.forEach { (code, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                wordType = code
                                showTypeMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = word, onValueChange = { word = it },
                label = { Text("Word") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = pinyin, onValueChange = { pinyin = it },
                label = { Text("Pinyin / Pronunciation") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = translation, onValueChange = { translation = it },
                label = { Text("Translation") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = example, onValueChange = { example = it },
                label = { Text("Example Sentence") },
                modifier = Modifier.fillMaxWidth(), colors = textFieldColors
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = translationExample, onValueChange = { translationExample = it },
                label = { Text("Translation Example") },
                modifier = Modifier.fillMaxWidth(), colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onAddWord(word, wordType, pinyin, lang1, translation, lang2, example, translationExample)
                    word = ""; wordType = ""; pinyin = ""; translation = ""
                    example = ""; translationExample = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AquaColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to Language Table", color = DarkBlueColor)
            }
        }
    }
}
