package com.nueng.translator.ui.study

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.data.model.PinyinData
import com.nueng.translator.data.model.StrokeItem
import com.nueng.translator.ui.theme.CardDarkBg
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.util.TtsHelper

// NOTE: NOT scrollable. Must be placed inside a LazyColumn item{}.
@Composable
fun StrokeLearningContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DisposableEffect(Unit) { TtsHelper.init(context); onDispose { } }

    val accentPurple = Color(0xFFAB47BC)
    val accentTeal = Color(0xFF26A69A)
    val accentAmber = Color(0xFFFFCA28)

    Column(modifier = modifier.fillMaxWidth()) {
        // Basic Strokes
        Text("Basic Strokes (基本笔画)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentPurple, modifier = Modifier.padding(vertical = 4.dp))
        Text("The 6 fundamental strokes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        PinyinData.basicStrokes.forEach { StrokeCard(it, accentPurple); Spacer(Modifier.height(6.dp)) }

        // Turning Strokes
        Spacer(Modifier.height(12.dp))
        Text("Turning Strokes (转折笔画)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentTeal, modifier = Modifier.padding(vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        PinyinData.turningStrokes.forEach { StrokeCard(it, accentTeal); Spacer(Modifier.height(6.dp)) }

        // Writing Rules
        Spacer(Modifier.height(12.dp))
        Text("Writing Rules (书写规则)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentAmber, modifier = Modifier.padding(vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        PinyinData.writingRules.forEachIndexed { index, rule ->
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, accentAmber.copy(0.3f), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(10.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("${index + 1}.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentAmber, modifier = Modifier.width(24.dp))
                    Text(rule, fontSize = 14.sp, color = CardTextPrimary, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun StrokeCard(stroke: StrokeItem, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, accentColor.copy(0.3f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDarkBg), shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (stroke.unicode.isNotBlank()) {
                Text(stroke.unicode, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = accentColor,
                    textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stroke.chinese, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(stroke.pinyin, fontSize = 14.sp, color = CardTextHint, fontStyle = FontStyle.Italic)
                }
                if (stroke.name.isNotBlank()) Text(stroke.name, fontSize = 13.sp, color = accentColor.copy(0.8f))
                if (stroke.description.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(stroke.description, fontSize = 12.sp, color = CardTextHint, lineHeight = 16.sp) }
            }
            IconButton(onClick = { TtsHelper.speak(stroke.chinese, "zh") }) {
                Icon(Icons.Default.VolumeUp, "Speak", tint = accentColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}
