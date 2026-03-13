package com.nueng.translator.ui.mynote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.data.local.entity.UserData

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

@Composable
fun AddEditNoteDialog(
    existingNote: UserData? = null,
    lang1: String,
    lang2: String,
    onDismiss: () -> Unit,
    onSave: (word: String, pinyin: String, langCode: String, translation: String, translationLangCode: String, example: String, translationExample: String, wordType: String) -> Unit
) {
    var word by rememberSaveable { mutableStateOf(existingNote?.word ?: "") }
    var wordType by rememberSaveable { mutableStateOf(existingNote?.wordType ?: "") }
    var pinyin by rememberSaveable { mutableStateOf(existingNote?.pinyin ?: "") }
    var translation by rememberSaveable { mutableStateOf(existingNote?.translation ?: "") }
    var example by rememberSaveable { mutableStateOf(existingNote?.exampleSentence ?: "") }
    var translationExample by rememberSaveable { mutableStateOf(existingNote?.translationExampleSentence ?: "") }
    var showTypeMenu by remember { mutableStateOf(false) }

    val isEdit = existingNote != null
    val title = if (isEdit) "Edit Word" else "Add Word"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                // Word Type selector
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTypeMenu = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (wordType.isBlank()) "Word Type (optional)" else WORD_TYPES.find { it.first == wordType }?.second ?: wordType,
                            modifier = Modifier.fillMaxWidth().height(48.dp).then(
                                Modifier.clickable { showTypeMenu = true }
                            ),
                            fontSize = 14.sp,
                            color = if (wordType.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        WORD_TYPES.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { wordType = code; showTypeMenu = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = word, onValueChange = { word = it },
                    label = { Text("Word") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pinyin, onValueChange = { pinyin = it },
                    label = { Text("Pinyin / Pronunciation") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = translation, onValueChange = { translation = it },
                    label = { Text("Translation") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = example, onValueChange = { example = it },
                    label = { Text("Example Sentence") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = translationExample, onValueChange = { translationExample = it },
                    label = { Text("Translation Example") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (word.isNotBlank() && translation.isNotBlank()) {
                        onSave(word, pinyin, lang1, translation, lang2, example, translationExample, wordType)
                        onDismiss()
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
