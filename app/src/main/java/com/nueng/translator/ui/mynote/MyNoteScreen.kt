package com.nueng.translator.ui.mynote

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.ui.components.DraggableFab
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.ui.theme.CardTextSecondary
import com.nueng.translator.ui.theme.DarkNoteBorder
import com.nueng.translator.ui.theme.LightNoteBorder
import com.nueng.translator.ui.theme.NoteCardDarkBg

@Composable
fun MyNoteScreen(
    modifier: Modifier = Modifier,
    lang1: String = "en",
    lang2: String = "zh",
    viewModel: MyNoteViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val isGuest by viewModel.isGuest.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<UserData?>(null) }

    if (isGuest) {
        Column(
            modifier = modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("My Note is not available for guests", fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please register or login to save your own words", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center)
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search your notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            if (notes.isEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (searchQuery.isBlank()) "No notes yet" else "No results found",
                        fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + to add your first word", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(note = note, onEdit = { editingNote = it }, onDelete = { viewModel.deleteNote(it) })
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        DraggableFab(onClick = { showAddDialog = true })
    }

    if (showAddDialog) {
        AddEditNoteDialog(lang1 = lang1, lang2 = lang2, onDismiss = { showAddDialog = false },
            onSave = { word, pinyin, langCode, translation, transLangCode, example, transExample, wordType ->
                viewModel.addNote(word, pinyin, langCode, translation, transLangCode, example, transExample, wordType)
            })
    }

    editingNote?.let { note ->
        AddEditNoteDialog(existingNote = note, lang1 = note.langCode, lang2 = note.translationLangCode,
            onDismiss = { editingNote = null },
            onSave = { word, pinyin, langCode, translation, transLangCode, example, transExample, wordType ->
                viewModel.updateNote(note.copy(word = word.trim(), wordType = wordType.trim(),
                    pinyin = pinyin.trim(), langCode = langCode, translation = translation.trim(),
                    translationLangCode = transLangCode, exampleSentence = example.trim(),
                    translationExampleSentence = transExample.trim()))
            })
    }
}

@Composable
private fun NoteCard(note: UserData, onEdit: (UserData) -> Unit, onDelete: (UserData) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) DarkNoteBorder else LightNoteBorder
    val accentColor = borderColor

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = NoteCardDarkBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp)) {
            if (note.wordType.isNotBlank()) {
                Text(note.wordType, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = accentColor, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(note.word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                    if (note.pinyin.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(note.pinyin, fontSize = 14.sp, color = CardTextSecondary)
                    }
                }
                Row {
                    IconButton(onClick = { onEdit(note) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, "Edit", Modifier.size(18.dp), tint = accentColor)
                    }
                    IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(18.dp), tint = Color(0xFFEF5350))
                    }
                }
            }

            Text(note.translation, fontSize = 16.sp, color = CardTextPrimary.copy(alpha = 0.85f))

            if (note.exampleSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(note.exampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = CardTextHint)
            }
            if (note.translationExampleSentence.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(note.translationExampleSentence, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                    color = CardTextHint.copy(alpha = 0.7f))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
