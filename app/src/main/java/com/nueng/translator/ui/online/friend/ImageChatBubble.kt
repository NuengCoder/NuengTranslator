package com.nueng.translator.ui.online.friend

import android.content.ContentValues
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.pm.ShortcutInfoCompat
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.nueng.translator.ui.online.settings.AvatarCircle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageChatBubble(
    imageData: String = "",
    imageUrl: String = "",
    isOwn: Boolean,
    senderName: String = "",
    senderLetter: Char = '?',
    senderId: String = "",
    timestamp: Long = 0L,
    isDeleted: Boolean = false,
    deletedBy: String = ""
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var showFullscreen by remember { mutableStateOf(false) }
    var saveMsg        by remember { mutableStateOf("") }

    // Decode base64 bitmap only if no URL available (legacy messages)
    val base64Bitmap = remember(imageData, imageUrl) {
        if (imageUrl.isBlank() && imageData.isNotBlank()) {
            try {
                val bytes = Base64.decode(imageData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        } else null
    }

    val hasImage = imageUrl.isNotBlank() || base64Bitmap != null

    // ── Fullscreen viewer ─────────────────────────────────────────────────────
    if (showFullscreen && hasImage) {
        var scale   by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        Dialog(
            onDismissRequest = { showFullscreen = false; scale = 1f; offsetX = 0f; offsetY = 0f },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

                // Image — URL via Coil or base64 bitmap
                if (imageUrl.isNotBlank()) {
                    SubcomposeAsyncImage(
                        model   = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = "Full image",
                        contentScale       = ContentScale.Fit,
                        loading  = { Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp)) } },
                        error    = { Icon(Icons.Default.BrokenImage, null, tint = Color.White, modifier = Modifier.size(48.dp)) },
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale   = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x * scale
                                    offsetY += pan.y * scale
                                }
                            }
                    )
                } else if (base64Bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap             = base64Bitmap.asImageBitmap(),
                        contentDescription = "Full image",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale   = (scale * zoom).coerceIn(1f, 5f)
                                    offsetX += pan.x * scale
                                    offsetY += pan.y * scale
                                }
                            }
                    )
                }

                // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = { showFullscreen = false; scale = 1f; offsetX = 0f; offsetY = 0f },
                        modifier = Modifier.size(40.dp).background(Color.Black.copy(0.5f), CircleShape)
                    ) { Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp)) }

                    if (saveMsg.isNotBlank()) {
                        Text(saveMsg, color = Color.White, fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp))
                    }

                    IconButton(
                        onClick  = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    try {
                                        val bytes = if (imageUrl.isNotBlank()) {
                                            val conn = java.net.URL(imageUrl).openConnection() as java.net.HttpURLConnection
                                            conn.connectTimeout = 10000; conn.readTimeout = 15000
                                            val b = conn.inputStream.readBytes()
                                            conn.disconnect(); b
                                        } else {
                                            Base64.decode(imageData, Base64.DEFAULT)
                                        }
                                        val filename = "NuengTranslator_${System.currentTimeMillis()}.jpg"
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val values = ContentValues().apply {
                                                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                                                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                                put(MediaStore.Images.Media.RELATIVE_PATH,
                                                    Environment.DIRECTORY_PICTURES + "/NuengTranslator")
                                            }
                                            val uri = context.contentResolver.insert(
                                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                                            uri?.let { u ->
                                                context.contentResolver.openOutputStream(u)?.use { it.write(bytes) }
                                                "Saved to Gallery!"
                                            } ?: "Save failed"
                                        } else "Save failed: unsupported OS"
                                    } catch (e: Exception) { "Save failed: ${e.message}" }
                                }
                                saveMsg = result
                                delay(2000)
                                saveMsg = ""
                            }
                        },
                        modifier = Modifier.size(40.dp).background(Color.Black.copy(0.5f), CircleShape)
                    ) { Icon(Icons.Default.Download, "Download", tint = Color.White, modifier = Modifier.size(20.dp)) }
                }

                // Zoom hint
                if (scale > 1f) {
                    Text("Tap to reset zoom", color = Color.White.copy(0.5f), fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .clickable { scale = 1f; offsetX = 0f; offsetY = 0f })
                }
            }
        }
    }

    // ── Chat bubble ───────────────────────────────────────────────────────────
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!isOwn) {
            AvatarCircle(letter = senderLetter, sizeDp = 28, username = senderId)
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            if (!isOwn && senderName.isNotBlank()) {
                Text(senderName, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            if (isDeleted) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f), shape = RoundedCornerShape(12.dp)) {
                    Text(deletedBy.ifBlank { "Deleted" }, fontSize = 13.sp, fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            } else if (imageUrl.isNotBlank()) {
                // URL image via Coil
                SubcomposeAsyncImage(
                    model   = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                    contentDescription = "Image",
                    contentScale       = ContentScale.Crop,
                    loading  = { Box(Modifier.size(120.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) } },
                    error    = { Box(Modifier.size(80.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        Alignment.Center) { Icon(Icons.Default.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    modifier = Modifier.widthIn(max = 220.dp)
                        .clip(RoundedCornerShape(
                            topStart = 16.dp, topEnd = 16.dp,
                            bottomStart = if (isOwn) 16.dp else 4.dp,
                            bottomEnd   = if (isOwn) 4.dp  else 16.dp))
                        .clickable { showFullscreen = true }
                )
            } else if (base64Bitmap != null) {
                // Legacy base64 image
                val ratio = base64Bitmap.width.toFloat() / base64Bitmap.height.toFloat()
                Box(modifier = Modifier.widthIn(max = 220.dp)
                    .clip(RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isOwn) 16.dp else 4.dp,
                        bottomEnd   = if (isOwn) 4.dp  else 16.dp))
                    .aspectRatio(ratio)
                    .clickable { showFullscreen = true }
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = base64Bitmap.asImageBitmap(),
                        contentDescription = "Image",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(modifier = Modifier.size(80.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        if (isOwn) Spacer(Modifier.width(6.dp))
    }
}