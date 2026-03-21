package com.nueng.translator.ui.study

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun StudyScreen(
    modifier: Modifier = Modifier,
    onNavigateToStudyPack: (pack: String, file: String, type: String) -> Unit = { _, _, _ -> }
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(selected = pagerState.currentPage == 0,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Pinyin") },
                icon = { Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp)) })
            Tab(selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Strokes") },
                icon = { Icon(Icons.Default.Brush, null, Modifier.size(18.dp)) })
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    when (page) {
                        0 -> PinyinLearningContent()
                        1 -> StrokeLearningContent()
                    }
                }
                item { Spacer(Modifier.height(8.dp)); VocabSectionHeader("IELTS Vocabulary  (EN \u2192 ZH/TH/LO/VI/ID)") }
                item { LevelGrid("IELTS", (1..6).toList(), Color(0xFF4CAF50)) { level ->
                    onNavigateToStudyPack("IELTS$level", "ielts$level", "ielts")
                } }
                item { Spacer(Modifier.height(8.dp)); VocabSectionHeader("HSK Vocabulary  (ZH \u2192 EN/TH/LO/VI/ID)") }
                item { LevelGrid("HSK", (1..6).toList(), Color(0xFFFF7043)) { level ->
                    onNavigateToStudyPack("HSK$level", "hsk$level", "hsk")
                } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun VocabSectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.School, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LevelGrid(prefix: String, levels: List<Int>, color: Color, onClick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { level ->
                    Card(Modifier.weight(1f).aspectRatio(1.4f).border(2.dp, color, RoundedCornerShape(12.dp))
                        .clickable { onClick(level) },
                        colors = CardDefaults.cardColors(containerColor = color.copy(0.2f)), shape = RoundedCornerShape(12.dp)
                    ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("$prefix $level", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color) } }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
