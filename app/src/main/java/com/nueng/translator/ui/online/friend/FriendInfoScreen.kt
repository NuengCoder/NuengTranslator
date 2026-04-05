package com.nueng.translator.ui.online.friend

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.profile.RankBadge
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendInfoScreen(
    friendUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateBackToFriendList: () -> Unit = {},
    onNavigateToFileStorage: (friendUserId: String, friendDisplayName: String) -> Unit = { _, _ -> },
    viewModel: FriendInfoViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val snackbarState   = remember { SnackbarHostState() }
    var showUnfriendDialog by remember { mutableStateOf(false) }
    var showBlockDialog    by remember { mutableStateOf(false) }

    LaunchedEffect(friendUserId) { viewModel.load(friendUserId) }

    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage.isNotEmpty()) {
            snackbarState.showSnackbar(uiState.actionMessage)
            viewModel.clearActionMessage()
        }
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.clearNavigateBack()
            onNavigateBackToFriendList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Friend Info",
                        modifier   = Modifier.fillMaxWidth(),
                        textAlign  = TextAlign.Center,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = Color.Transparent)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    modifier    = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
            } else {
                val displayName  = uiState.nickname.ifBlank { uiState.username }
                val avatarLetter = if (displayName.isNotEmpty()) displayName.first().uppercaseChar() else '?'

                // ── Avatar + name + rank ───────────────────────────
                AvatarCircle(letter = avatarLetter, sizeDp = 88, username = friendUserId)
                Spacer(modifier = Modifier.height(14.dp))
                Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                if (uiState.nickname.isNotBlank()) {
                    Text("@" + uiState.username, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(10.dp))
                RankBadge(rank = uiState.rank)

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))

                // ── Bio card ───────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape  = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val bioText = uiState.bio.ifBlank { "Hello My name is " + uiState.username }
                        Text(
                            text      = bioText,
                            fontSize  = 14.sp,
                            color     = if (uiState.bio.isBlank())
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.onSurface,
                            fontStyle = if (uiState.bio.isBlank()) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── File Storage button ────────────────────────────
                OutlinedButton(
                    onClick  = { onNavigateToFileStorage(friendUserId, displayName) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$displayName's File Storage", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // ── Block / Unblock button ─────────────────────────
                // Hide entirely when THEY blocked US — we can't interact
                if (!uiState.isBlockedByThem) {
                    if (uiState.isBlocked) {
                        OutlinedButton(
                            onClick  = { showBlockDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unblock $displayName", fontSize = 14.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick  = { showBlockDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Block $displayName", fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Unfriend button ────────────────────────────────
                Button(
                    onClick  = { showUnfriendDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unfriend", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // ── Unfriend dialog ────────────────────────────────────────────────────
    if (showUnfriendDialog) {
        val displayName = uiState.nickname.ifBlank { uiState.username }
        AlertDialog(
            onDismissRequest = { showUnfriendDialog = false },
            title            = { Text("Unfriend?") },
            text             = { Text("Remove $displayName from your friends? You can add them again later.") },
            confirmButton    = {
                TextButton(onClick = { viewModel.unfriend(); showUnfriendDialog = false }) {
                    Text("Unfriend", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showUnfriendDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Block / Unblock dialog ─────────────────────────────────────────────
    if (showBlockDialog) {
        val displayName = uiState.nickname.ifBlank { uiState.username }
        if (uiState.isBlocked) {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title            = { Text("Unblock User?") },
                text             = { Text("Unblock $displayName? They will be able to message you again.") },
                confirmButton    = {
                    TextButton(onClick = { viewModel.unblockUser(); showBlockDialog = false }) {
                        Text("Unblock")
                    }
                },
                dismissButton    = {
                    TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                title            = { Text("Block User?") },
                text             = { Text("Block $displayName? They won't be able to send you messages.") },
                confirmButton    = {
                    TextButton(onClick = { viewModel.blockUser(); showBlockDialog = false }) {
                        Text("Block", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton    = {
                    TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
