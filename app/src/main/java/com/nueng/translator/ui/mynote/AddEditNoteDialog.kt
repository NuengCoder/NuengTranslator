package com.nueng.translator.ui.mynote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum
import com.github.houbb.pinyin.util.PinyinHelper
import com.nueng.translator.data.local.entity.UserData
import com.nueng.translator.ui.translate.StrokeDrawScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

private fun isChinese(text: String): Boolean =
    text.any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF }

private fun lookupPinyin(word: String): String {
    return try {
        PinyinHelper.toPinyin(word, PinyinStyleEnum.DEFAULT, "")
    } catch (_: Exception) { "" }
}

private fun getCharOptions(ch: Char): List<String> {
    return try {
        PinyinHelper.toPinyinList(ch, PinyinStyleEnum.DEFAULT)
            .filterNotNull().distinct().filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }
}

@Composable
fun AddEditNoteDialog(
    existingNote: UserData? = null,
    lang1: String,
    lang2: String,
    onDismiss: () -> Unit,
    onSave: (word: String, pinyin: String, langCode: String, translation: String,
             translationLangCode: String, example: String, translationExample: String,
             wordType: String) -> Unit
) {
    var word          by rememberSaveable { mutableStateOf(existingNote?.word ?: "") }
    var wordType      by rememberSaveable { mutableStateOf(existingNote?.wordType ?: "") }
    var pinyin        by rememberSaveable { mutableStateOf(existingNote?.pinyin ?: "") }
    var translation   by rememberSaveable { mutableStateOf(existingNote?.translation ?: "") }
    var example       by rememberSaveable { mutableStateOf(existingNote?.exampleSentence ?: "") }
    var transExample  by rememberSaveable { mutableStateOf(existingNote?.translationExampleSentence ?: "") }
    var showTypeMenu  by remember { mutableStateOf(false) }
    var showStrokeDraw by remember { mutableStateOf(false) }
    var lastWord      by remember { mutableStateOf("__INIT__") }
    var polyOptions   by remember { mutableStateOf<Map<Int, List<String>>>(emptyMap()) }
    var polySelected  by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LaunchedEffect(word) {
        val trimmed = word.trim()
        if (trimmed.isNotBlank() && isChinese(trimmed) && trimmed != lastWord) {
            lastWord = trimmed
            val result = withContext(Dispatchers.IO) {
                try { lookupPinyin(trimmed) } catch (_: Exception) { "" }
            }
            if (result.isNotBlank()) pinyin = result
            val opts = withContext(Dispatchers.IO) {
                trimmed.mapIndexed { i, ch ->
                    if (isChinese(ch.toString())) {
                        val options = getCharOptions(ch)
                        if (options.size > 1) i to options else null
                    } else null
                }.filterNotNull().toMap()
            }
            polyOptions  = opts
            polySelected = opts.mapValues { 0 }
        } else if (trimmed.isBlank() || !isChinese(trimmed)) {
            polyOptions  = emptyMap()
            polySelected = emptyMap()
        }
    }

    fun rebuildFromPoly() {
        val trimmed = word.trim()
        if (trimmed.isBlank()) return
        val sb = StringBuilder()
        trimmed.forEachIndexed { i, ch ->
            val opts = polyOptions[i]
            if (opts != null) {
                sb.append(opts.getOrElse(polySelected[i] ?: 0) { opts[0] })
            } else {
                try { sb.append(lookupPinyin(ch.toString()).ifBlank { ch.toString() }) }
                catch (_: Exception) { sb.append(ch) }
            }
        }
        pinyin = sb.toString()
    }

    // ── StrokeDraw fullscreen dialog ──────────────────────────────────────────
    if (showStrokeDraw) {
        Dialog(
            onDismissRequest = { showStrokeDraw = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress      = true,
                dismissOnClickOutside   = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                StrokeDrawScreen(
                    lang1              = "zh",
                    onNavigateBack     = { showStrokeDraw = false },
                    onCharacterSelected = { char ->
                        word = word + char   // append to existing word
                        showStrokeDraw = false
                    }
                )
            }
        }
    }

    // ── Main Add/Edit dialog ──────────────────────────────────────────────────
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = if (existingNote != null) "Edit Word" else "Add Word",
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = { showStrokeDraw = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Draw,
                        contentDescription = "Draw character",
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Word Type dropdown
                Box {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { showTypeMenu = true },
                        color    = MaterialTheme.colorScheme.surfaceVariant,
                        shape    = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text     = if (wordType.isBlank()) "Word Type (optional)"
                            else WORD_TYPES.find { it.first == wordType }?.second ?: wordType,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            fontSize = 14.sp,
                            color    = if (wordType.isBlank())
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(expanded = showTypeMenu, onDismissRequest = { showTypeMenu = false }) {
                        WORD_TYPES.forEach { (code, label) ->
                            DropdownMenuItem(
                                text    = { Text(label) },
                                onClick = { wordType = code; showTypeMenu = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value          = word,
                    onValueChange  = { word = it },
                    label          = { Text("Word") },
                    placeholder    = { Text("Type or draw a word...") },
                    singleLine     = true,
                    modifier       = Modifier.fillMaxWidth(),
                    supportingText = if (isChinese(word.trim()) && word.trim().isNotBlank()) {
                        { Text("Chinese detected \u2014 pinyin auto-filled",
                            fontSize = 10.sp, color = MaterialTheme.colorScheme.primary) }
                    } else null
                )

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value         = pinyin,
                    onValueChange = { pinyin = it; polyOptions = emptyMap() },
                    label         = { Text("Pinyin / Pronunciation") },
                    placeholder   = { Text("e.g. n\u01d0h\u01ce\u01ce") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                // Polyphonic picker
                if (polyOptions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Pick tone for polyphonic characters:",
                        fontSize = 10.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    polyOptions.forEach { (charIdx, opts) ->
                        val ch = word.trim().getOrNull(charIdx) ?: return@forEach
                        Text("$ch :", fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp))
                        Row(modifier = Modifier.wrapContentWidth()) {
                            opts.forEachIndexed { j, reading ->
                                FilterChip(
                                    selected = (polySelected[charIdx] ?: 0) == j,
                                    onClick  = {
                                        polySelected = polySelected.toMutableMap()
                                            .also { m -> m[charIdx] = j }
                                        rebuildFromPoly()
                                    },
                                    label    = { Text(reading, fontSize = 12.sp) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value         = translation,
                    onValueChange = { translation = it },
                    label         = { Text("Translation") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value         = example,
                    onValueChange = { example = it },
                    label         = { Text("Example Sentence (optional)") },
                    placeholder   = { Text("...") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 1,
                    maxLines      = 3
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value         = transExample,
                    onValueChange = { transExample = it },
                    label         = { Text("Translation Example (optional)") },
                    placeholder   = { Text("...") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 1,
                    maxLines      = 3
                )

                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = word.isNotBlank() && translation.isNotBlank(),
                onClick = {
                    onSave(word.trim(), pinyin.trim(), lang1,
                        translation.trim(), lang2,
                        example.trim(), transExample.trim(), wordType)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}