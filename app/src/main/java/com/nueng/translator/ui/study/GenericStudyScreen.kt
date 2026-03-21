package com.nueng.translator.ui.study

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.data.model.HskWord
import com.nueng.translator.ui.theme.CardDarkBg
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.ui.theme.DarkCardBorder
import com.nueng.translator.util.Languages
import com.nueng.translator.util.WordPackLoader
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericStudyScreen(
    onNavigateBack: () -> Unit,
    packName: String,
    jsonFileName: String,
    availableLangs: List<String>,
    isSourceChinese: Boolean = true
) {
    val context = LocalContext.current
    var words by remember { mutableStateOf<List<HskWord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(jsonFileName) {
        val actualFile = if (jsonFileName.endsWith(".json")) jsonFileName else "$jsonFileName.json"
        words = WordPackLoader.loadFromAssets(context, actualFile)
        isLoading = false
    }

    var isFlashcardMode by rememberSaveable { mutableStateOf(true) }
    var selectedLang by rememberSaveable { mutableStateOf(availableLangs.firstOrNull() ?: "en") }
    var showLangMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val pagerState = if (words.isNotEmpty()) rememberPagerState(pageCount = { words.size }) else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$packName (${words.size} words)") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    Box {
                        OutlinedButton(onClick = { showLangMenu = true }) { Text(Languages.getDisplayName(selectedLang), fontSize = 12.sp) }
                        DropdownMenu(expanded = showLangMenu, onDismissRequest = { showLangMenu = false }) {
                            availableLangs.forEach { lang ->
                                DropdownMenuItem(text = { Text(Languages.getDisplayName(lang)) },
                                    onClick = { selectedLang = lang; showLangMenu = false })
                            }
                        }
                    }
                    IconButton(onClick = { isFlashcardMode = !isFlashcardMode }) {
                        Icon(if (isFlashcardMode) Icons.AutoMirrored.Filled.List else Icons.Default.Style,
                            "Toggle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            words.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("No words yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Text("Add words to $jsonFileName.json", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f), textAlign = TextAlign.Center)
                    }
                }
            }
            isFlashcardMode && pagerState != null -> {
                // Swipeable flashcards
                Column(Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
                    val progress = (pagerState.currentPage + 1).toFloat() / words.size
                    LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth().height(6.dp), color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("${pagerState.currentPage + 1} / ${words.size}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))

                    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                        val word = words[page.coerceIn(0, words.size - 1)]
                        var showAnswer by remember { mutableStateOf(false) }

                        val sourceText = if (isSourceChinese) word.chinese else word.english.ifBlank { word.chinese }
                        val sourceSub = word.pinyin

                        Card(
                            Modifier.fillMaxSize().padding(horizontal = 4.dp)
                                .clickable { showAnswer = !showAnswer }
                                .border(2.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (word.type.isNotBlank()) {
                                        Text(word.type, fontSize = 12.sp, color = DarkCardBorder, fontStyle = FontStyle.Italic)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(sourceText, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary, textAlign = TextAlign.Center)
                                    Spacer(Modifier.height(8.dp))
                                    Text(sourceSub, fontSize = 20.sp, color = CardTextPrimary.copy(0.7f), textAlign = TextAlign.Center)

                                    if (showAnswer) {
                                        Spacer(Modifier.height(16.dp))
                                        Text(word.getTranslation(selectedLang), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)

                                        val exSource = if (isSourceChinese) word.exampleChinese else (word.exampleTranslations["en"] ?: word.exampleEnglish)
                                        if (exSource.isNotBlank()) {
                                            Spacer(Modifier.height(20.dp))
                                            Text(exSource, fontSize = 16.sp, color = CardTextPrimary.copy(0.8f), textAlign = TextAlign.Center)
                                            if (isSourceChinese && word.examplePinyin.isNotBlank()) {
                                                Spacer(Modifier.height(4.dp))
                                                Text(word.examplePinyin, fontSize = 14.sp, color = CardTextHint, textAlign = TextAlign.Center)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(word.getExampleTranslation(selectedLang), fontSize = 14.sp, fontStyle = FontStyle.Italic,
                                                color = CardTextHint, textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Spacer(Modifier.height(24.dp))
                                        Text("Tap to reveal \u2022 Swipe for next", fontSize = 14.sp, color = CardTextHint.copy(0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                // List view - tap to jump to flashcard
                LazyColumn(Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = 16.dp)) {
                    items(words, key = { it.id }) { word ->
                        val sourceText = if (isSourceChinese) word.chinese else word.english.ifBlank { word.chinese }

                        Card(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .border(1.dp, DarkCardBorder.copy(0.5f), RoundedCornerShape(10.dp))
                                .clickable {
                                    val idx = words.indexOf(word)
                                    if (idx >= 0 && pagerState != null) {
                                        coroutineScope.launch { pagerState.scrollToPage(idx) }
                                        isFlashcardMode = true
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${word.id}.", fontSize = 12.sp, color = CardTextHint, modifier = Modifier.width(28.dp))
                                    if (word.type.isNotBlank()) { Text(word.type, fontSize = 10.sp, color = DarkCardBorder, fontStyle = FontStyle.Italic); Spacer(Modifier.width(6.dp)) }
                                    Text(sourceText, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(word.pinyin, fontSize = 13.sp, color = CardTextPrimary.copy(0.6f))
                                }
                                Text(word.getTranslation(selectedLang), fontSize = 14.sp, color = CardTextPrimary.copy(0.8f), modifier = Modifier.padding(start = 28.dp))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
