package com.nueng.translator.ui.online.settings

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val db = FirebaseDatabase.getInstance(
    "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
)

// Simple in-memory cache: username -> base64
private val avatarCache = mutableMapOf<String, String>()

@Composable
fun AvatarCircle(
    letter: Char,
    sizeDp: Int = 48,
    backgroundColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    avatarBase64: String = "",    // direct base64 if already known
    username: String = ""         // if set, fetch from Firebase (cached)
) {
    val bgColor = if (backgroundColor != Color.Unspecified) backgroundColor
                  else avatarColorForChar(letter)

    // Resolve which base64 to use
    var resolvedBase64 by remember(avatarBase64, username) {
        mutableStateOf(
            when {
                avatarBase64.isNotBlank() -> avatarBase64
                username.isNotBlank()     -> avatarCache[username] ?: ""
                else                      -> ""
            }
        )
    }

    // Fetch from Firebase if username given and not in cache
    LaunchedEffect(username) {
        if (username.isNotBlank() && resolvedBase64.isBlank()) {
            try {
                val snap = withContext(Dispatchers.IO) {
                    db.getReference("online_profiles").child(username)
                        .child("avatarBase64").get().await()
                }
                val b64 = snap.getValue(String::class.java) ?: ""
                if (b64.isNotBlank()) {
                    avatarCache[username] = b64
                    resolvedBase64 = b64
                }
            } catch (_: Exception) {}
        }
    }

    // Also update if avatarBase64 param changes (e.g. after upload in OSettings)
    LaunchedEffect(avatarBase64) {
        if (avatarBase64.isNotBlank()) {
            resolvedBase64 = avatarBase64
            if (username.isNotBlank()) avatarCache[username] = avatarBase64
        }
    }

    val bitmap = remember(resolvedBase64) {
        if (resolvedBase64.isNotBlank()) {
            try {
                val bytes = Base64.decode(resolvedBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        } else null
    }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(if (bitmap == null) bgColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = "Avatar",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(sizeDp.dp).clip(CircleShape)
            )
        } else {
            Text(
                text       = letter.uppercaseChar().toString(),
                fontSize   = (sizeDp * 0.4f).sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }
}

fun avatarColorForChar(c: Char): Color {
    val colors = listOf(
        Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF00695C),
        Color(0xFFAD1457), Color(0xFF4527A0), Color(0xFF0277BD),
        Color(0xFF558B2F), Color(0xFFE65100), Color(0xFF37474F)
    )
    return colors[(c.uppercaseChar().code) % colors.size]
}
