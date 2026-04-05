package com.nueng.translator.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.util.Languages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateToLogin: () -> Unit,
    onNavigateToAdminPanel: () -> Unit,
    onNavigateToQr: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState    by viewModel.uiState.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val uiLanguage by viewModel.uiLanguage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var colorExpanded    by rememberSaveable { mutableStateOf(false) }

    // Color sliders — init from saved values
    var fgR by rememberSaveable { mutableFloatStateOf(255f) }
    var fgG by rememberSaveable { mutableFloatStateOf(255f) }
    var fgB by rememberSaveable { mutableFloatStateOf(255f) }
    var fgA by rememberSaveable { mutableFloatStateOf(255f) }
    var bgR by rememberSaveable { mutableFloatStateOf(18f) }
    var bgG by rememberSaveable { mutableFloatStateOf(18f) }
    var bgB by rememberSaveable { mutableFloatStateOf(18f) }
    var bgA by rememberSaveable { mutableFloatStateOf(255f) }
    var txR by rememberSaveable { mutableFloatStateOf(255f) }
    var txG by rememberSaveable { mutableFloatStateOf(255f) }
    var txB by rememberSaveable { mutableFloatStateOf(255f) }
    var txA by rememberSaveable { mutableFloatStateOf(255f) }
    var atR by rememberSaveable { mutableFloatStateOf(176f) }
    var atG by rememberSaveable { mutableFloatStateOf(176f) }
    var atB by rememberSaveable { mutableFloatStateOf(176f) }
    var atA by rememberSaveable { mutableFloatStateOf(255f) }

    // Load saved colors into sliders
    LaunchedEffect(uiState.colorFg) {
        if (uiState.colorFg != 0L) {
            val c = Color(uiState.colorFg.toULong().toInt())
            fgR = c.red*255; fgG = c.green*255; fgB = c.blue*255; fgA = c.alpha*255
        }
    }
    LaunchedEffect(uiState.colorBg) {
        if (uiState.colorBg != 0L) {
            val c = Color(uiState.colorBg.toULong().toInt())
            bgR = c.red*255; bgG = c.green*255; bgB = c.blue*255; bgA = c.alpha*255
        }
    }
    LaunchedEffect(uiState.colorText) {
        if (uiState.colorText != 0L) {
            val c = Color(uiState.colorText.toULong().toInt())
            txR = c.red*255; txG = c.green*255; txB = c.blue*255; txA = c.alpha*255
        }
    }
    LaunchedEffect(uiState.colorAppText) {
        if (uiState.colorAppText != 0L) {
            val c = Color(uiState.colorAppText.toULong().toInt())
            atR = c.red*255; atG = c.green*255; atB = c.blue*255; atA = c.alpha*255
        }
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) { snackbarHostState.showSnackbar("All note data deleted."); viewModel.clearDeleteSuccess() }
    }
    LaunchedEffect(uiState.colorSaved) {
        if (uiState.colorSaved) { snackbarHostState.showSnackbar("Colors saved!"); viewModel.clearColorSaved() }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // ── User Card ──────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(64.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (uiState.isGuest) "Guest" else (uiState.user?.username ?: "User"),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (!uiState.isGuest && uiState.user != null) {
                        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(Date(uiState.user!!.createdAt))
                        Text("Joined $dateStr", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (uiState.isAdmin) Text("Admin", fontSize = 12.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    } else Text("Guest", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // QR code — tap to open
                Surface(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val uname = uiState.user?.username ?: ""
                        if (uname.isNotBlank()) onNavigateToQr(uname)
                    },
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.QrCode, "QR Code",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // ── Dark Mode ──────────────────────────────────────────────────────
        SettingsRow(icon = Icons.Default.DarkMode, title = "Dark Mode",
            trailing = { Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) }) })

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // ── Color Settings (collapsible) ───────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth().clickable { colorExpanded = !colorExpanded }
            .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Palette, null, modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Color Settings", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text("Customize app colors", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if (colorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = colorExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                ColorSliderGroup("Foreground (FG) — Primary color", fgR, fgG, fgB, fgA,
                    { fgR=it },{ fgG=it },{ fgB=it },{ fgA=it })
                Spacer(Modifier.height(12.dp))
                ColorSliderGroup("Background (BG)", bgR, bgG, bgB, bgA,
                    { bgR=it },{ bgG=it },{ bgB=it },{ bgA=it })
                Spacer(Modifier.height(12.dp))
                ColorSliderGroup("Text — onSurface", txR, txG, txB, txA,
                    { txR=it },{ txG=it },{ txB=it },{ txA=it })
                Spacer(Modifier.height(12.dp))
                ColorSliderGroup("AppText — onSurfaceVariant", atR, atG, atB, atA,
                    { atR=it },{ atG=it },{ atB=it },{ atA=it })
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.saveColors(
                            fg      = Color(fgR/255f, fgG/255f, fgB/255f, fgA/255f).toArgb().toLong(),
                            bg      = Color(bgR/255f, bgG/255f, bgB/255f, bgA/255f).toArgb().toLong(),
                            text    = Color(txR/255f, txG/255f, txB/255f, txA/255f).toArgb().toLong(),
                            appText = Color(atR/255f, atG/255f, atB/255f, atA/255f).toArgb().toLong()
                        )
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save Colors")
                    }
                    Button(onClick = { viewModel.saveColors(0L, 0L, 0L, 0L)
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

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // ── UI Language ────────────────────────────────────────────────────
        SettingsRow(icon = Icons.Default.Language, title = "UI Language",
            subtitle = "${Languages.getDisplayName(uiLanguage)} (coming soon)",
            onClick = { showLanguageMenu = true }) {
            DropdownMenu(expanded = showLanguageMenu, onDismissRequest = { showLanguageMenu = false }) {
                Languages.SUPPORTED.forEach { lang ->
                    DropdownMenuItem(text = { Text("${lang.nameNative} (${lang.nameEn})") },
                        onClick = { viewModel.setUiLanguage(lang.code); showLanguageMenu = false })
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // ── Delete All Note Data ───────────────────────────────────────────
        if (!uiState.isGuest) {
            SettingsRow(icon = Icons.Default.Delete, title = "Delete All My Note Data",
                subtitle = "Remove all saved words and directories",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { showDeleteDialog = true })
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        // ── Logout ─────────────────────────────────────────────────────────
        SettingsRow(icon = Icons.AutoMirrored.Filled.Logout, title = "Logout",
            iconTint = MaterialTheme.colorScheme.error, onClick = { showLogoutDialog = true })

        // ── Admin Panel ────────────────────────────────────────────────────
        if (uiState.isAdmin) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)
                .border(2.dp, Color(0xFF00BCD4), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToAdminPanel() }
                    .padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(28.dp),
                        tint = Color(0xFF00BCD4))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Admin Panel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Manage users & language dictionary", fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        SnackbarHost(hostState = snackbarHostState)
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Note Data?") },
            text  = { Text("This will permanently remove all your saved words and directories.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteMyNoteData(); showDeleteDialog = false }) {
                Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
    }
    if (showLogoutDialog) {
        AlertDialog(onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout?") }, text = { Text("Are you sure you want to logout?") },
            confirmButton = { TextButton(onClick = { viewModel.logout(); showLogoutDialog = false; onNavigateToLogin() }) {
                Text("Logout", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") } })
    }
}

@Composable
fun ColorSliderGroup(
    label: String, r: Float, g: Float, b: Float, a: Float,
    onR:(Float)->Unit, onG:(Float)->Unit, onB:(Float)->Unit, onA:(Float)->Unit
) {
    val previewColor = Color(r/255f, g/255f, b/255f, a/255f)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Box(modifier = Modifier.size(20.dp).background(previewColor, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary)
    }
    listOf("R" to Pair(r,onR), "G" to Pair(g,onG), "B" to Pair(b,onB), "A" to Pair(a,onA))
        .forEach { (ch, pair) ->
            val (value, setter) = pair
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ch, fontSize = 11.sp, modifier = Modifier.width(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                androidx.compose.material3.Slider(value = value, onValueChange = setter,
                    valueRange = 0f..255f, modifier = Modifier.weight(1f))
                Text(value.toInt().toString(), fontSize = 10.sp, modifier = Modifier.width(30.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
}

@Composable
private fun SettingsRow(
    icon: ImageVector, title: String, subtitle: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = iconTint)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}
