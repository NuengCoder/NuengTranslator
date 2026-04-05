package com.nueng.translator.ui.online.group

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val GC_EMOJI_LIST = listOf(
    "\uD83D\uDE00","\uD83D\uDE01","\uD83D\uDE02","\uD83D\uDE03","\uD83D\uDE04","\uD83D\uDE05",
    "\uD83D\uDE06","\uD83D\uDE09","\uD83D\uDE0A","\uD83D\uDE0B","\uD83D\uDE0C","\uD83D\uDE0D",
    "\uD83D\uDE0E","\uD83D\uDE0F","\uD83D\uDE10","\uD83D\uDE11","\uD83D\uDE12","\uD83D\uDE13",
    "\uD83D\uDE14","\uD83D\uDE15","\uD83D\uDE16","\uD83D\uDE17","\uD83D\uDE18","\uD83D\uDE19",
    "\uD83D\uDE1A","\uD83D\uDE1B","\uD83D\uDE1C","\uD83D\uDE1D","\uD83D\uDE1E","\uD83D\uDE1F",
    "\uD83D\uDE20","\uD83D\uDE21","\uD83D\uDE22","\uD83D\uDE28","\uD83D\uDE2D","\uD83D\uDE31",
    "\uD83D\uDE33","\uD83D\uDE35",
    "\uD83D\uDC4D","\uD83D\uDC4E","\uD83D\uDC4F","\uD83D\uDC4A","\uD83D\uDC4B","\uD83D\uDC4C",
    "\u2764\uFE0F","\uD83D\uDC95","\uD83D\uDC99","\uD83D\uDC9A","\uD83D\uDC9B","\uD83D\uDCA5",
    "\uD83D\uDE80","\u2708\uFE0F","\uD83C\uDF89","\uD83C\uDF81","\uD83C\uDF55","\uD83C\uDF54",
    "\uD83D\uDC31","\uD83D\uDC36","\uD83D\uDC3C","\uD83D\uDCA9","\uD83E\uDD16","\uD83D\uDC7B",
    "\u26BD","\uD83C\uDFC0","\uD83C\uDF1F","\u2B50","\uD83C\uDF08","\u26A1"
)

@Composable
fun GCVoiceRecordingBar(onRelease: () -> Unit, onCancel: () -> Unit) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); elapsedSec++ } }
    val infiniteTransition = rememberInfiniteTransition(label = "gcRec")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "gcPhase"
    )
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Spacer(Modifier.width(8.dp))
        Text("%d:%02d".format(elapsedSec/60, elapsedSec%60), fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(42.dp))
        Spacer(Modifier.width(8.dp))
        Row(modifier = Modifier.weight(1f).height(32.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until 28) {
                val h = (0.25f + 0.75f * kotlin.math.abs(kotlin.math.sin(phase + i * 0.45f)) * 28).dp
                Box(modifier = Modifier.width(3.dp).height(h).clip(RoundedCornerShape(1.5.dp))
                    .background(MaterialTheme.colorScheme.primary))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("Release", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.error.copy(0.15f))
            .pointerInput(Unit) { detectTapGestures(onPress = { tryAwaitRelease(); onRelease() }) },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Mic, "Recording", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun GCVoicePreviewBar(
    durationSec: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDiscard: () -> Unit,
    onSend: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gcPrev")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "gcPrevPhase"
    )
    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = onDiscard, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, "Discard", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
        }
        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(0.12f))
            .clickable { onPlay() },
            contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Text("%d:%02d".format(durationSec/60, durationSec%60), fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(36.dp))
        Row(modifier = Modifier.weight(1f).height(28.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (i in 0 until 24) {
                val h = if (isPlaying)
                    (0.25f + 0.75f * kotlin.math.abs(kotlin.math.sin(phase + i * 0.45f))) * 24
                else
                    (0.2f + 0.7f * kotlin.math.abs(kotlin.math.sin(Math.PI * i / 24).toFloat())) * 24
                Box(modifier = Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(1.5.dp))
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)))
            }
        }
        Box(modifier = Modifier.size(40.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onSend() },
            contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send",
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}
