package com.nueng.translator.ui.translate

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
import com.nueng.translator.data.local.entity.LanguageWord

private val WORD_TYPES = listOf(
    "" to "None", "n." to "n. (Noun)", "pn." to "pn. (Pronoun)",
    "v." to "v. (Verb)", "adj." to "adj. (Adjective)", "adv." to "adv. (Adverb)",
    "prep." to "prep. (Preposition)", "conj." to "conj. (Conjunction)",
    "interj." to "interj. (Interjection)", "det." to "det. (Determiner)",
    "num." to "num. (Number)", "mw." to "mw. (Measure Word)",
    "phr." to "phr. (Phrase)", "idiom" to "idiom (Idiom)"
)

@Composable
fun EditLanguageWordDialog(
    word: LanguageWord,
    onDismiss: () -> Unit,
    onSave: (LanguageWord) -> Unit
) {
    var editWord by rememberSaveable { mutableStateOf(word.word) }
    var editType by rememberSaveable { mutableStateOf(word.wordType) }
    var editPinyin by rememberSaveable { mutableStateOf(word.pinyin) }
    var editTranslation by rememberSaveable { mutableStateOf(word.translation) }
    var editExample by rememberSaveable { mutableStateOf(word.exampleSentence) }
    var editTransExample by rememberSaveable { mutableStateOf(word.translationExampleSentence) }
    var showTypeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Language Word") },
        text = {
            Column {
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showTypeMenu = true },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (editType.isBlank()) "Word Type" else WORD_TYPES.find { it.first == editType }?.second ?: editType,
                            modifier = Modifier.fillMaxWidth().clickable { showTypeMenu = true },
                            fontSize = 14.sp
                        )
                    }
                    DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        WORD_TYPES.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { editType = code; showTypeMenu = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editWord, onValueChange = { editWord = it },
                    label = { Text("Word") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editPinyin, onValueChange = { editPinyin = it },
                    label = { Text("Pinyin") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editTranslation, onValueChange = { editTranslation = it },
                    label = { Text("Translation") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editExample, onValueChange = { editExample = it },
                    label = { Text("Example") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = editTransExample, onValueChange = { editTransExample = it },
                    label = { Text("Translation Example") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (editWord.isNotBlank() && editTranslation.isNotBlank()) {
                    onSave(word.copy(
                        word = editWord.trim(), wordType = editType.trim(),
                        pinyin = editPinyin.trim(), translation = editTranslation.trim(),
                        exampleSentence = editExample.trim(),
                        translationExampleSentence = editTransExample.trim()
                    ))
                    onDismiss()
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
