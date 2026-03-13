package com.nueng.translator.ui.study

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StudyScreen(modifier: Modifier = Modifier) {
    val accentColor = MaterialTheme.colorScheme.primary
    val cardBg = MaterialTheme.colorScheme.surface

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item { SectionHeader(title = "Character Stroke Practice") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .border(2.dp, accentColor, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Brush, contentDescription = null,
                        modifier = Modifier.size(48.dp), tint = accentColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Stroke Order Practice", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Input a character to see its stroke pattern", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    ComingSoonBadge()
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "IELTS Vocabulary")
        }
        item { LevelGrid(prefix = "IELTS", levels = (1..6).toList(), color = Color(0xFF4CAF50)) }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = "HSK Vocabulary")
        }
        item { LevelGrid(prefix = "HSK", levels = (1..6).toList(), color = Color(0xFFFF7043)) }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.School, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LevelGrid(prefix: String, levels: List<Int>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.chunked(3).forEach { rowLevels ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowLevels.forEach { level ->
                    LevelCard(title = "$prefix $level", color = color, modifier = Modifier.weight(1f))
                }
                repeat(3 - rowLevels.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LevelCard(title: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.aspectRatio(1.4f)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = color, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(6.dp))
                ComingSoonBadge(small = true)
            }
        }
    }
}

@Composable
private fun ComingSoonBadge(small: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, contentDescription = null,
            modifier = Modifier.size(if (small) 12.dp else 14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Coming Soon", fontSize = if (small) 10.sp else 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
