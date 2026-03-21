package com.nueng.translator.ui.study

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.data.model.PinyinData
import com.nueng.translator.data.model.PinyinItem
import com.nueng.translator.ui.theme.CardDarkBg
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.util.TtsHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PinyinLearningContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DisposableEffect(Unit) { TtsHelper.init(context); onDispose { } }

    val accentBlue = Color(0xFF42A5F5)
    val accentOrange = Color(0xFFFF7043)
    val accentGreen = Color(0xFF66BB6A)

    Column(modifier = modifier.fillMaxWidth()) {
        // Simple Vowels
        Text("Simple Vowels (单韵母)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentBlue, modifier = Modifier.padding(vertical = 4.dp))
        Text("Tap each tone to hear its sound", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PinyinData.simpleVowels.forEach { item -> VowelCard(item, accentBlue); Spacer(Modifier.height(8.dp)) }

        // Compound Vowels
        Spacer(Modifier.height(12.dp))
        Text("Compound Vowels (复韵母)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentGreen, modifier = Modifier.padding(vertical = 4.dp))
        Text("Tap each tone to hear — ${PinyinData.compoundVowels.size} combinations", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PinyinData.compoundVowels.forEach { item -> VowelCard(item, accentGreen); Spacer(Modifier.height(8.dp)) }

        // Consonants
        Spacer(Modifier.height(12.dp))
        Text("Consonants (声母)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentOrange, modifier = Modifier.padding(vertical = 4.dp))
        Text("23 initial consonants — tap to hear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PinyinData.consonants.forEach { CompactPinyinChip(it, accentOrange) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VowelCard(item: PinyinItem, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.pinyin, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accentColor)
                Row {
                    IconButton(onClick = { TtsHelper.speak(item.example.ifBlank { item.pinyin }, "zh") }) {
                        Icon(Icons.Default.VolumeUp, "Play", tint = accentColor, modifier = Modifier.size(28.dp))
                    }
                    IconButton(onClick = { TtsHelper.speakSlow(item.example.ifBlank { item.pinyin }, "zh") }) {
                        Icon(Icons.Default.SlowMotionVideo, "Slow", tint = accentColor.copy(0.6f), modifier = Modifier.size(24.dp))
                    }
                }
            }

            // Tones row - each tone plays its own Chinese character via TTS
            if (item.tones.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val toneLabels = listOf("1st", "2nd", "3rd", "4th")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tones.forEachIndexed { index, tone ->
                        val toneChar = item.toneChars.getOrNull(index) ?: item.example
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { TtsHelper.speak(toneChar, "zh") }
                                .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(tone, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = CardTextPrimary)
                            Text(toneChar, fontSize = 13.sp, color = accentColor.copy(0.7f))
                            Text(toneLabels.getOrElse(index) { "" }, fontSize = 9.sp, color = CardTextHint)
                        }
                    }
                }
            }

            // Example
            if (item.example.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ex: ", fontSize = 12.sp, color = CardTextHint)
                    Text(item.example, fontSize = 16.sp, color = CardTextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(item.examplePinyin, fontSize = 13.sp, color = CardTextHint, fontStyle = FontStyle.Italic)
                }
            }
        }
    }
}

@Composable
private fun CompactPinyinChip(item: PinyinItem, accentColor: Color) {
    Card(
        modifier = Modifier.clickable { TtsHelper.speak(item.example.ifBlank { item.pinyin }, "zh") }
            .border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.pinyin, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            if (item.example.isNotBlank()) Text(item.example, fontSize = 13.sp, color = CardTextPrimary.copy(0.7f))
        }
    }
}
