package com.nueng.translator.ui.online.friend

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nueng.translator.ui.online.settings.AvatarCircle

@Composable
fun ImageChatBubble(
    imageData: String,         // base64
    isOwn: Boolean,
    senderName: String = "",
    senderLetter: Char = '?',
    senderId: String = "",
    timestamp: Long = 0L,
    isDeleted: Boolean = false,
    deletedBy: String = ""
) {
    var showFullscreen by remember { mutableStateOf(false) }

    // Decode bitmap
    val bitmap = remember(imageData) {
        if (imageData.isNotBlank()) {
            try {
                val bytes = Base64.decode(imageData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        } else null
    }

    if (showFullscreen && bitmap != null) {
        Dialog(onDismissRequest = { showFullscreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap             = bitmap.asImageBitmap(),
                    contentDescription = "Full image",
                    contentScale       = ContentScale.Fit,
                    modifier           = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Row(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isOwn)
            androidx.compose.foundation.layout.Arrangement.End
        else
            androidx.compose.foundation.layout.Arrangement.Start,
        verticalAlignment   = Alignment.Bottom
    ) {
        if (!isOwn) {
            AvatarCircle(letter = senderLetter, sizeDp = 28, username = senderId)
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start) {
            if (!isOwn && senderName.isNotBlank()) {
                Text(senderName, fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            if (isDeleted) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(deletedBy.ifBlank { "Deleted" },
                        fontSize  = 13.sp, fontStyle = FontStyle.Italic,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                        modifier  = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            } else if (bitmap != null) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                Box(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .clip(RoundedCornerShape(
                            topStart    = 16.dp,
                            topEnd      = 16.dp,
                            bottomStart = if (isOwn) 16.dp else 4.dp,
                            bottomEnd   = if (isOwn) 4.dp  else 16.dp
                        ))
                        .aspectRatio(ratio)
                        .clickable { showFullscreen = true }
                ) {
                    Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = "Image",
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            } else {
                // Loading / decode fail
                Box(
                    modifier          = Modifier.size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)),
                    contentAlignment  = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
        if (isOwn) Spacer(Modifier.width(6.dp))
    }
}
