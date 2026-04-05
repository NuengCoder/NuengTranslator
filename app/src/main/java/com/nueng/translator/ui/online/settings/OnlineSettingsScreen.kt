package com.nueng.translator.ui.online.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.settings.ColorSliderGroup
import androidx.core.graphics.scale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnlineSettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToDirectory: (Long, String) -> Unit = { _, _ -> },
    onNavigateToQr: (String) -> Unit = {},
    viewModel: OnlineSettingsViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    val context = androidx.compose.ui.platform.LocalContext.current
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val stream = context.contentResolver.openInputStream(uri) ?: return@let
                val original = BitmapFactory.decodeStream(stream)
                stream.close()
                // Scale down to max 256x256 and compress to JPEG
                val maxSize = 256
                val scale = minOf(maxSize.toFloat() / original.width, maxSize.toFloat() / original.height, 1f)
                val scaled = if (scale < 1f) original.scale(
                    (original.width * scale).toInt(),
                    (original.height * scale).toInt()
                ) else original
                val out = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 75, out)
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
                viewModel.uploadAvatar(base64)
            } catch (_: Exception) {}
        }
    }

    var nicknameInput by rememberSaveable { mutableStateOf("") }
    var bioInput      by rememberSaveable { mutableStateOf("") }
    var unblockTarget by remember { mutableStateOf<String?>(null) }
    var showLogout    by remember { mutableStateOf(false) }

    var dirsExpanded  by rememberSaveable { mutableStateOf(true) }
    var blockExpanded by rememberSaveable { mutableStateOf(false) }
    var colorExpanded by rememberSaveable { mutableStateOf(false) }

    // Color sliders
    var fgR by rememberSaveable { mutableFloatStateOf(255f) }; var fgG by rememberSaveable { mutableFloatStateOf(255f) }
    var fgB by rememberSaveable { mutableFloatStateOf(255f) }; var fgA by rememberSaveable { mutableFloatStateOf(255f) }
    var bgR by rememberSaveable { mutableFloatStateOf(18f) }; var bgG by rememberSaveable { mutableFloatStateOf(18f) }
    var bgB by rememberSaveable { mutableFloatStateOf(18f) }; var bgA by rememberSaveable { mutableFloatStateOf(255f) }
    var txR by rememberSaveable { mutableFloatStateOf(255f) }; var txG by rememberSaveable { mutableFloatStateOf(255f) }
    var txB by rememberSaveable { mutableFloatStateOf(255f) }; var txA by rememberSaveable { mutableFloatStateOf(255f) }
    var atR by rememberSaveable { mutableFloatStateOf(176f) }; var atG by rememberSaveable { mutableFloatStateOf(176f) }
    var atB by rememberSaveable { mutableFloatStateOf(176f) }; var atA by rememberSaveable { mutableFloatStateOf(255f) }

    LaunchedEffect(Unit) { viewModel.loadColors() }
    LaunchedEffect(uiState.nickname) { if (nicknameInput.isBlank()) nicknameInput = uiState.nickname }
    LaunchedEffect(uiState.bio)      { if (bioInput.isBlank())      bioInput      = uiState.bio }
    LaunchedEffect(uiState.saveMessage) {
        if (uiState.saveMessage.isNotEmpty()) { snackbarState.showSnackbar(uiState.saveMessage); viewModel.clearSaveMessage() }
    }
    LaunchedEffect(uiState.colorFg) {
        if (uiState.colorFg != 0L) { val c = Color(uiState.colorFg.toULong().toInt()); fgR=c.red*255; fgG=c.green*255; fgB=c.blue*255; fgA=c.alpha*255 }
    }
    LaunchedEffect(uiState.colorBg) {
        if (uiState.colorBg != 0L) { val c = Color(uiState.colorBg.toULong().toInt()); bgR=c.red*255; bgG=c.green*255; bgB=c.blue*255; bgA=c.alpha*255 }
    }
    LaunchedEffect(uiState.colorText) {
        if (uiState.colorText != 0L) { val c = Color(uiState.colorText.toULong().toInt()); txR=c.red*255; txG=c.green*255; txB=c.blue*255; txA=c.alpha*255 }
    }
    LaunchedEffect(uiState.colorAppText) {
        if (uiState.colorAppText != 0L) { val c = Color(uiState.colorAppText.toULong().toInt()); atR=c.red*255; atG=c.green*255; atB=c.blue*255; atA=c.alpha*255 }
    }

    // Use nickname if set, fallback to username
    val displayName = uiState.nickname.ifBlank { uiState.username }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // ── Profile Header ─────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(72.dp)
                    .combinedClickable(
                        onClick      = {},
                        onLongClick  = { photoLauncher.launch("image/*") }
                    )
                ) {
                    AvatarCircle(
                        letter        = displayName.firstOrNull()?.uppercaseChar() ?: '?',
                        sizeDp        = 72,
                        avatarBase64  = uiState.avatarBase64,
                        modifier      = Modifier.size(72.dp)
                    )
                    // Camera hint overlay
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint               = MaterialTheme.colorScheme.onPrimary,
                            modifier           = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("@${uiState.username}", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // QR code — tap to open
                Surface(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                    .clickable { if (uiState.username.isNotBlank()) onNavigateToQr(uiState.username) },
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode, "My QR", tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            // ── Nickname ───────────────────────────────────────────────────
            OSection("Nickname")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = nicknameInput, onValueChange = { nicknameInput = it },
                label = { Text("Nickname") }, placeholder = { Text(uiState.username) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canChangeNickname)
            if (!uiState.canChangeNickname) {
                Spacer(Modifier.height(4.dp))
                Text("Changeable in ${uiState.daysUntilNicknameChange} day(s)", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            // ── Bio ────────────────────────────────────────────────────────
            OSection("Bio")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = bioInput, onValueChange = { bioInput = it },
                label = { Text("Bio") }, placeholder = { Text("Hello, I'm ${uiState.username}!") },
                modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)

            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.saveProfile(nicknameInput, bioInput) },
                modifier = Modifier.fillMaxWidth().height(50.dp), enabled = !uiState.isSaving) {
                if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("Save Profile", fontSize = 15.sp)
            }

            Spacer(Modifier.height(24.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))

            // ── Color Settings (collapsible) ───────────────────────────────
            CollapsibleHeader("Color Settings", colorExpanded, "FG / BG / Text / AppText") { colorExpanded = !colorExpanded }
            AnimatedVisibility(visible = colorExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    ColorSliderGroup("Foreground (FG)", fgR, fgG, fgB, fgA, { fgR=it },{ fgG=it },{ fgB=it },{ fgA=it })
                    Spacer(Modifier.height(12.dp))
                    ColorSliderGroup("Background (BG)", bgR, bgG, bgB, bgA, { bgR=it },{ bgG=it },{ bgB=it },{ bgA=it })
                    Spacer(Modifier.height(12.dp))
                    ColorSliderGroup("Text — onSurface", txR, txG, txB, txA, { txR=it },{ txG=it },{ txB=it },{ txA=it })
                    Spacer(Modifier.height(12.dp))
                    ColorSliderGroup("AppText — onSurfaceVariant", atR, atG, atB, atA, { atR=it },{ atG=it },{ atB=it },{ atA=it })
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            viewModel.saveColors(
                                fg      = Color(fgR/255f, fgG/255f, fgB/255f, fgA/255f).toArgb().toLong(),
                                bg      = Color(bgR/255f, bgG/255f, bgB/255f, bgA/255f).toArgb().toLong(),
                                text    = Color(txR/255f, txG/255f, txB/255f, txA/255f).toArgb().toLong(),
                                appText = Color(atR/255f, atG/255f, atB/255f, atA/255f).toArgb().toLong()
                            )
                        }, modifier = Modifier.weight(1f)) { Text("Save Colors") }
                        Button(onClick = {
                            viewModel.saveColors(0L, 0L, 0L, 0L)
                            fgR=255f; fgG=255f; fgB=255f; fgA=255f
                            bgR=18f;  bgG=18f;  bgB=18f;  bgA=255f
                            txR=255f; txG=255f; txB=255f; txA=255f
                            atR=176f; atG=176f; atB=176f; atA=255f
                        }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor   = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("Reset")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(8.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))

            // ── UI Language ────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, null, modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("UI Language", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Multi-language support coming soon", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(); Spacer(Modifier.height(8.dp))

            // ── My Note Directories (collapsible) ──────────────────────────
            CollapsibleHeader("My Note Directories (${uiState.directories.size})", dirsExpanded) { dirsExpanded = !dirsExpanded }
            AnimatedVisibility(visible = dirsExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (uiState.directories.isEmpty()) {
                        Text("No directories yet.", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    } else {
                        uiState.directories.forEach { dir ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .clickable { onNavigateToDirectory(dir.id, dir.name) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(dir.name, fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(); Spacer(Modifier.height(8.dp))

            // ── Block List (collapsible) ───────────────────────────────────
            CollapsibleHeader("Block List (${uiState.blockedUsers.size})", blockExpanded) { blockExpanded = !blockExpanded }
            AnimatedVisibility(visible = blockExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (uiState.blockedUsers.isEmpty()) {
                        Text("No blocked users.", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    } else {
                        uiState.blockedUsers.forEach { u ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                .border(1.dp, MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(10.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(u, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f))
                                    TextButton(onClick = { unblockTarget = u },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error)) {
                                        Text("Unblock", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(); Spacer(Modifier.height(16.dp))

            // ── Logout ─────────────────────────────────────────────────────
            Button(onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontSize = 15.sp)
            }

            Spacer(Modifier.height(40.dp))
        }

        SnackbarHost(hostState = snackbarState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    unblockTarget?.let { target ->
        AlertDialog(onDismissRequest = { unblockTarget = null },
            title = { Text("Unblock $target?") },
            text  = { Text("$target will be able to message you again.") },
            confirmButton = { TextButton(onClick = { viewModel.unblockUser(target); unblockTarget = null }) {
                Text("Unblock") } },
            dismissButton = { TextButton(onClick = { unblockTarget = null }) { Text("Cancel") } })
    }

    if (showLogout) {
        AlertDialog(onDismissRequest = { showLogout = false },
            title = { Text("Logout?") }, text = { Text("Are you sure you want to logout?") },
            confirmButton = { TextButton(onClick = {
                viewModel.logout(); showLogout = false; onNavigateToLogin?.invoke()
            }) { Text("Logout", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("Cancel") } })
    }
}

@Composable
private fun CollapsibleHeader(title: String, expanded: Boolean, subtitle: String? = null, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun OSection(label: String) {
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}
