package com.nueng.translator.ui.online.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nueng.translator.ui.online.settings.AvatarCircle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sin
import kotlin.math.abs

enum class VoicePlayState { NOT_DOWNLOADED, DOWNLOADING, READY, PLAYING, DONE }

private const val BAR_COUNT = 28

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VoiceChatBubble(
    msgId: String,
    voiceData: String,          // Base64
    voiceDuration: Int,         // seconds
    senderName: String,
    senderLetter: Char,
    senderId: String = "",
    isOwn: Boolean,
    timestamp: Long,
    onReply: () -> Unit   = {},
    onDelete: () -> Unit  = {},
    onForward: () -> Unit = {},
    isDeleted: Boolean = false,
    deletedBy: String = "",
    reactions: Map<String, List<String>> = emptyMap(),
    onReactionTap: (String) -> Unit = {},
    replyToSender: String = "",
    replyToPreview: String = "",
    replyToDataType: String = "",
    onReplyTap: (String) -> Unit = {},
    replyToId: String = ""
) {
    val context   = LocalContext.current
    val cacheFile = remember(msgId) { File(context.cacheDir, "voice_$msgId.3gp") }

    // Consider cached only if file exists with meaningful size (>= 100 bytes)
    val isCached = cacheFile.exists() && cacheFile.length() >= 100
    var playState  by remember { mutableStateOf(
        if (isCached) VoicePlayState.READY
        else if (voiceData.isNotBlank()) VoicePlayState.READY  // data in memory, no need to download
        else VoicePlayState.NOT_DOWNLOADED
    )}
    var player     by remember { mutableStateOf<MediaPlayer?>(null) }
    var elapsed    by remember { mutableIntStateOf(0) }

    // Clean up player on leave
    DisposableEffect(msgId) {
        onDispose {
            player?.stop(); player?.release(); player = null
        }
    }

    // Track elapsed while playing
    LaunchedEffect(playState) {
        if (playState == VoicePlayState.PLAYING) {
            while (playState == VoicePlayState.PLAYING) {
                kotlinx.coroutines.delay(500)
                elapsed = (player?.currentPosition ?: 0) / 1000
            }
        }
    }

    val tf     = SimpleDateFormat("HH:mm", Locale.getDefault())
    val isOwn2 = isOwn
    val bubbleBg = if (isOwn2) MaterialTheme.colorScheme.primary.copy(0.85f)
                   else MaterialTheme.colorScheme.surfaceVariant
    val fgColor  = if (isOwn2) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurface
    val waveInactive = if (isOwn2) MaterialTheme.colorScheme.onPrimary.copy(0.4f)
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
    val waveActive   = if (isOwn2) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.primary

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalArrangement = if (isOwn2) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!isOwn2) {
            AvatarCircle(letter = senderLetter, sizeDp = 28, username = senderId.ifBlank { senderName })
            Spacer(Modifier.width(6.dp))
        }

        Column(horizontalAlignment = if (isOwn2) Alignment.End else Alignment.Start) {
            if (!isOwn2) {
                Text(senderName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            if (isDeleted) {
                // Deleted state — show grey italic label instead of player
                androidx.compose.material3.Surface(
                    modifier = Modifier.widthIn(min = 120.dp),
                    color    = if (isOwn2) MaterialTheme.colorScheme.primary.copy(0.3f)
                               else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                    shape    = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isOwn2) 16.dp else 4.dp,
                        bottomEnd   = if (isOwn2) 4.dp else 16.dp)
                ) {
                    Text(deletedBy.ifBlank { "Deleted" }, fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            } else
            Surface(
                modifier = Modifier.widthIn(min = 160.dp, max = 220.dp)
                    .clip(RoundedCornerShape(
                        topStart    = if (replyToSender.isNotBlank()) 4.dp else 16.dp,
                        topEnd      = if (replyToSender.isNotBlank()) 4.dp else 16.dp,
                        bottomStart = if (isOwn2) 16.dp else 4.dp,
                        bottomEnd   = if (isOwn2) 4.dp else 16.dp
                    )),
                color = bubbleBg
            ) {
                Column {
                    // Reply preview strip
                    if (replyToSender.isNotBlank()) {
                        val replyIcon = when (replyToDataType) {
                            "voice" -> " "
                            "file"  -> " "
                            else    -> ""
                        }
                        androidx.compose.material3.Surface(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (replyToId.isNotBlank()) onReplyTap(replyToId)
                            },
                            color    = if (isOwn2) MaterialTheme.colorScheme.onPrimary.copy(0.18f)
                                       else MaterialTheme.colorScheme.onSurface.copy(0.10f),
                            shape    = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                                Text(
                                    replyToSender,
                                    fontSize   = 11.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color      = if (isOwn2) MaterialTheme.colorScheme.onPrimary
                                                 else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    replyIcon + replyToPreview,
                                    fontSize  = 11.sp,
                                    maxLines  = 1,
                                    overflow  = TextOverflow.Ellipsis,
                                    color     = if (isOwn2) MaterialTheme.colorScheme.onPrimary.copy(0.7f)
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(
                        modifier          = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Play/Download/Pause button
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isOwn2) MaterialTheme.colorScheme.onPrimary.copy(0.2f)
                                        else MaterialTheme.colorScheme.primary.copy(0.12f))
                            .clickable {
                                when (playState) {
                                    VoicePlayState.NOT_DOWNLOADED -> {
                                        if (voiceData.isNotBlank()) {
                                            // Data in memory — write to cache then play
                                            playState = VoicePlayState.DOWNLOADING
                                            Thread {
                                                try {
                                                    val bytes = Base64.decode(voiceData, Base64.DEFAULT)
                                                    cacheFile.writeBytes(bytes)
                                                    // Start playback on main thread
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        playState = VoicePlayState.READY
                                                        startPlayback(context, cacheFile,
                                                            onStart  = { p -> player = p; playState = VoicePlayState.PLAYING; elapsed = 0 },
                                                            onFinish = { playState = VoicePlayState.DONE; elapsed = voiceDuration }
                                                        )
                                                    }
                                                } catch (_: Exception) {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                        playState = VoicePlayState.NOT_DOWNLOADED
                                                    }
                                                }
                                            }.start()
                                        }
                                    }
                                    VoicePlayState.READY, VoicePlayState.DONE -> {
                                        elapsed = 0
                                        // Ensure cache exists before playing
                                        if (!cacheFile.exists() || cacheFile.length() < 100) {
                                            if (voiceData.isNotBlank()) {
                                                try {
                                                    val bytes = Base64.decode(voiceData, Base64.DEFAULT)
                                                    cacheFile.writeBytes(bytes)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                        startPlayback(context, cacheFile,
                                            onStart  = { p -> player = p; playState = VoicePlayState.PLAYING },
                                            onFinish = { playState = VoicePlayState.DONE; elapsed = voiceDuration }
                                        )
                                    }
                                    VoicePlayState.PLAYING -> {
                                        player?.pause(); playState = VoicePlayState.READY
                                    }
                                    VoicePlayState.DOWNLOADING -> { /* wait */ }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (playState) {
                            VoicePlayState.NOT_DOWNLOADED ->
                                Icon(Icons.Default.Download, "Download", tint = fgColor, modifier = Modifier.size(16.dp))
                            VoicePlayState.DOWNLOADING ->
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = fgColor)
                            VoicePlayState.PLAYING ->
                                Icon(Icons.Default.Pause, "Pause", tint = fgColor, modifier = Modifier.size(16.dp))
                            else ->
                                Icon(Icons.Default.PlayArrow, "Play",
                                    tint = if (playState == VoicePlayState.DONE) fgColor.copy(0.6f) else fgColor,
                                    modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // Waveform + duration
                    Column(modifier = Modifier.weight(1f)) {
                        WaveformBar(
                            isPlaying    = playState == VoicePlayState.PLAYING,
                            progress     = if (voiceDuration > 0) elapsed.toFloat() / voiceDuration else 0f,
                            activeColor  = waveActive,
                            inactiveColor = waveInactive
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text      = formatDuration(
                                if (playState == VoicePlayState.PLAYING || playState == VoicePlayState.DONE)
                                    elapsed else voiceDuration
                            ),
                            fontSize  = 10.sp,
                            color     = fgColor.copy(0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                } // end Column
            }
            // Reactions row
            if (!isDeleted && reactions.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    reactions.forEach { (emoji, names) ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier.clickable { onReactionTap(emoji) },
                            shape    = RoundedCornerShape(12.dp),
                            color    = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text(emoji, fontSize = 14.sp)
                                Spacer(Modifier.width(2.dp))
                                Text(names.size.toString(), fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

        }

        if (isOwn2) Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun WaveformBar(
    isPlaying: Boolean,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "phase"
    )

    Row(
        modifier = Modifier.height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until BAR_COUNT) {
            val frac    = i.toFloat() / BAR_COUNT
            val passed  = frac <= progress

            // Bar height: sine wave for visual interest; animated when playing
            val baseH   = 0.3f + 0.7f * abs(sin(Math.PI * i / BAR_COUNT)).toFloat()
            val animH   = if (isPlaying && passed)
                0.3f + 0.7f * abs(sin(animPhase + i * 0.4f))
            else baseH

            val barH    = (animH * 18).dp
            val color   = if (passed) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barH)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60; val s = seconds % 60
    return "%d:%02d".format(m, s)
}

private fun startPlayback(
    context: Context,
    cacheFile: File,
    onStart: (MediaPlayer) -> Unit,
    onFinish: () -> Unit
) {
    try {
        val mp = MediaPlayer()
        mp.setDataSource(cacheFile.absolutePath)
        mp.prepare()
        mp.setOnCompletionListener { onFinish() }
        mp.start()
        onStart(mp)
    } catch (e: Exception) {
        onFinish()
    }
}
