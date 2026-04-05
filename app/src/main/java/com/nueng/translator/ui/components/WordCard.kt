package com.nueng.translator.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.ui.theme.CardDarkBg
import com.nueng.translator.ui.theme.CardTextHint
import com.nueng.translator.ui.theme.CardTextPrimary
import com.nueng.translator.ui.theme.CardTextSecondary
import com.nueng.translator.ui.theme.DarkCardBorder
import com.nueng.translator.ui.theme.LightCardBorder

@Composable
fun WordCard(
    word: String,
    wordType: String = "",
    pinyin: String = "",
    translation: String,
    exampleSentence: String = "",
    translationExampleSentence: String = "",
    modifier: Modifier = Modifier
) {
    val isDark      = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) DarkCardBorder else LightCardBorder
    val accentColor = borderColor
    val hasExtra    = exampleSentence.isNotBlank() || translationExampleSentence.isNotBlank()
    var expanded    by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .then(if (hasExtra) Modifier.clickable { expanded = !expanded } else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(containerColor = CardDarkBg),
        shape     = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (wordType.isNotBlank()) {
                Text(wordType, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = accentColor, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CardTextPrimary)
                if (pinyin.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(pinyin, fontSize = 14.sp, color = CardTextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(translation, fontSize = 16.sp, color = CardTextPrimary.copy(alpha = 0.85f))

            // Both examples expand/collapse together on card tap
            AnimatedVisibility(visible = expanded,
                enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    if (exampleSentence.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(exampleSentence, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic, color = CardTextHint)
                    }
                    if (translationExampleSentence.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(translationExampleSentence, fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            color = CardTextHint.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.luminance(): Float =
    (0.299f * red + 0.587f * green + 0.114f * blue)
