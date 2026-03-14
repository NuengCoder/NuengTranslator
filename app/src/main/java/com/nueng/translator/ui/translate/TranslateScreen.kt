package com.nueng.translator.ui.translate

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.data.local.entity.LanguageWord
import com.nueng.translator.ui.components.LanguageSelectorRow
import com.nueng.translator.ui.theme.CardDarkBg
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.ui.theme.CardTextSecondary
import com.nueng.translator.ui.theme.DarkCardBorder
import com.nueng.translator.ui.theme.LightCardBorder

@Composable
fun TranslateScreen(
    modifier: Modifier = Modifier,
    onNavigateToStrokeDraw: (String) -> Unit = {},
    onNavigateToCamera: (String) -> Unit = {},
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lang1 by viewModel.lang1.collectAsState()
    val lang2 by viewModel.lang2.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        LanguageSelectorRow(
            lang1 = lang1, lang2 = lang2,
            onLang1Change = { viewModel.setLang1(it) },
            onLang2Change = { viewModel.setLang2(it) },
            onSwap = { viewModel.swapLanguages() }
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search word?") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )
            VoiceInputButton(langCode = lang1, onResult = { viewModel.onSearchQueryChange(it) })
            IconButton(onClick = { onNavigateToCamera(lang1) }) {
                Icon(Icons.Default.CameraAlt, "Camera OCR", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = { onNavigateToStrokeDraw(lang1) }) {
                Icon(Icons.Default.Draw, "Draw", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (searchQuery.isBlank()) "Search or browse words" else "No results found",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(items = results, key = { it.id }) { word ->
                    CleanWordCard(word = word)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CleanWordCard(word: LanguageWord) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) DarkCardBorder else LightCardBorder

    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (word.wordType.isNotBlank()) {
                Text(word.wordType, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = borderColor, fontStyle = FontStyle.Italic)
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                if (word.pinyin.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(word.pinyin, fontSize = 14.sp, color = CardTextSecondary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(word.translation, fontSize = 16.sp, color = CardTextPrimary.copy(0.85f))
            if (word.exampleSentence.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(word.exampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = CardTextHint)
            }
            if (word.translationExampleSentence.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(word.translationExampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = CardTextHint.copy(0.7f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float = (0.299f * red + 0.587f * green + 0.114f * blue)
