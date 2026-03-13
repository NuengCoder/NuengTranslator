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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    isAdmin: Boolean = false,
    viewModel: TranslateViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val lang1 by viewModel.lang1.collectAsState()
    val lang2 by viewModel.lang2.collectAsState()
    val results by viewModel.searchResults.collectAsState()

    var editingWord by remember { mutableStateOf<LanguageWord?>(null) }
    var deletingWord by remember { mutableStateOf<LanguageWord?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        LanguageSelectorRow(
            lang1 = lang1, lang2 = lang2,
            onLang1Change = { viewModel.setLang1(it) },
            onLang2Change = { viewModel.setLang2(it) },
            onSwap = { viewModel.swapLanguages() }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Search word?") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (results.isEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (searchQuery.isBlank()) "Search or browse words" else "No results found",
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(items = results, key = { it.id }) { word ->
                    TranslateWordCard(word = word, isAdmin = isAdmin,
                        onEdit = { editingWord = it }, onDelete = { deletingWord = it })
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
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
            text = { Text("Delete " + word.word + " from the Language Table? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteWord(word); deletingWord = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingWord = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TranslateWordCard(
    word: LanguageWord, isAdmin: Boolean,
    onEdit: (LanguageWord) -> Unit, onDelete: (LanguageWord) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) DarkCardBorder else LightCardBorder
    val accentColor = borderColor

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp,
            end = if (isAdmin) 4.dp else 16.dp, bottom = 12.dp)) {
            if (word.wordType.isNotBlank()) {
                Text(word.wordType, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = accentColor, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(word.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                    if (word.pinyin.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(word.pinyin, fontSize = 14.sp, color = CardTextSecondary)
                    }
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = { onEdit(word) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp), tint = accentColor)
                        }
                        IconButton(onClick = { onDelete(word) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp), tint = Color(0xFFEF5350))
                        }
                    }
                }
            }

            Text(word.translation, fontSize = 16.sp, color = CardTextPrimary.copy(alpha = 0.85f))

            if (word.exampleSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(word.exampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = CardTextHint)
            }
            if (word.translationExampleSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(word.translationExampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                    color = CardTextHint.copy(alpha = 0.7f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
