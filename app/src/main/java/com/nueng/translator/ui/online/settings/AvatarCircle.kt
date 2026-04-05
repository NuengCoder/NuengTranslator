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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val db = FirebaseDatabase.getInstance(
    "https://nuengtranslator-default-rtdb.asia-southeast1.firebasedatabase.app"
)

// In-memory cache: username -> url or base64
private val avatarUrlCache  = mutableMapOf<String, String>() // username -> imgbb url
private val avatarB64Cache  = mutableMapOf<String, String>() // username -> base64 (fallback)

@Composable
fun AvatarCircle(
    letter: Char,
    sizeDp: Int = 48,
    backgroundColor: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    avatarBase64: String = "",   // direct base64 (from OSettings after upload)
    username: String = ""        // fetch from Firebase if set
) {
    val bgColor  = if (backgroundColor != Color.Unspecified) backgroundColor else avatarColorForChar(letter)
    val context  = LocalContext.current

    // Resolve what to show: url > direct base64 > cached base64
    var resolvedUrl by remember(avatarBase64, username) {
        mutableStateOf(if (username.isNotBlank()) avatarUrlCache[username] ?: "" else "")
    }
    var resolvedB64 by remember(avatarBase64, username) {
        mutableStateOf(
            when {
                avatarBase64.isNotBlank() -> avatarBase64
                username.isNotBlank()     -> avatarB64Cache[username] ?: ""
                else                      -> ""
            }
        )
    }

    // Fetch from Firebase when username given and nothing cached yet
    LaunchedEffect(username) {
        if (username.isNotBlank() && resolvedUrl.isBlank() && resolvedB64.isBlank()) {
            try {
                val snap = withContext(Dispatchers.IO) {
                    db.getReference("online_profiles").child(username).get().await()
                }
                val url = snap.child("avatarUrl").getValue(String::class.java) ?: ""
                val b64 = snap.child("avatarBase64").getValue(String::class.java) ?: ""
                if (url.isNotBlank()) {
                    avatarUrlCache[username] = url
                    resolvedUrl = url
                } else if (b64.isNotBlank()) {
                    avatarB64Cache[username] = b64
                    resolvedB64 = b64
                }
            } catch (_: Exception) {}
        }
    }

    // Update cache if direct base64 passed (e.g. after upload in OSettings)
    LaunchedEffect(avatarBase64) {
        if (avatarBase64.isNotBlank()) {
            resolvedB64 = avatarBase64
            if (username.isNotBlank()) avatarB64Cache[username] = avatarBase64
        }
    }

    // Decode base64 to bitmap if needed
    val bitmap = remember(resolvedB64) {
        if (resolvedB64.isNotBlank() && resolvedUrl.isBlank()) {
            try {
                val bytes = Base64.decode(resolvedB64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        } else null
    }

    Box(
        modifier         = modifier.size(sizeDp.dp).clip(CircleShape)
            .background(if (resolvedUrl.isBlank() && bitmap == null) bgColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        when {
            resolvedUrl.isNotBlank() -> {
                // Load from ImgBB URL via Coil
                SubcomposeAsyncImage(
                    model              = ImageRequest.Builder(context).data(resolvedUrl).crossfade(true).build(),
                    contentDescription = "Avatar",
                    contentScale       = ContentScale.Crop,
                    loading  = {
                        Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(bgColor),
                            contentAlignment = Alignment.Center) {
                            Text(letter.uppercaseChar().toString(),
                                fontSize = (sizeDp * 0.4f).sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    error    = {
                        Box(Modifier.size(sizeDp.dp).clip(CircleShape).background(bgColor),
                            contentAlignment = Alignment.Center) {
                            Text(letter.uppercaseChar().toString(),
                                fontSize = (sizeDp * 0.4f).sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    modifier = Modifier.size(sizeDp.dp).clip(CircleShape)
                )
            }
            bitmap != null -> {
                Image(
                    bitmap             = bitmap.asImageBitmap(),
                    contentDescription = "Avatar",
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.size(sizeDp.dp).clip(CircleShape)
                )
            }
            else -> {
                Text(
                    text       = letter.uppercaseChar().toString(),
                    fontSize   = (sizeDp * 0.4f).sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }
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