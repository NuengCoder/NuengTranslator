package com.nueng.translator.ui.online.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    targetUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: OtherProfileViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }

    LaunchedEffect(targetUserId) { viewModel.loadProfile(targetUserId) }
    LaunchedEffect(uiState.actionMessage) {
        if (uiState.actionMessage.isNotEmpty()) {
            snackbarState.showSnackbar(uiState.actionMessage)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = uiState.nickname.ifBlank { uiState.username.ifBlank { "..." } }
                    Text("$name's Profile", fontSize = 18.sp, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            Spacer(modifier = Modifier.height(28.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp), strokeWidth = 3.dp)
            } else {
                val displayName  = uiState.nickname.ifBlank { uiState.username }
                val avatarLetter = if (displayName.isNotEmpty()) displayName.first().uppercaseChar() else '?'

                AvatarCircle(letter = avatarLetter, sizeDp = 96, username = targetUserId)
                Spacer(modifier = Modifier.height(16.dp))

                // Show nickname as main name; show @username only if username is a real name (not just ID)
                Text(displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                if (uiState.nickname.isNotBlank() && uiState.username.isNotBlank()) {
                    Text("@" + uiState.username, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(10.dp))
                RankBadge(rank = uiState.rank)
                Spacer(modifier = Modifier.height(28.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(20.dp))

                // Bio card
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val bioText = uiState.bio.ifBlank { "Hello My name is " + uiState.username }
                        Text(
                            text      = bioText,
                            fontSize  = 14.sp,
                            color     = if (uiState.bio.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            fontStyle = if (uiState.bio.isBlank()) FontStyle.Italic else FontStyle.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Action buttons — driven by FriendStatus ────────────────
                when (uiState.friendStatus) {

                    FriendStatus.LOADING -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    }

                    FriendStatus.FRIENDS -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onNavigateToChat(targetUserId) }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick  = { viewModel.unfriend(targetUserId) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unfriend", fontSize = 14.sp)
                            }
                        }
                    }

                    FriendStatus.SENT -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick  = {},
                                enabled  = false,
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors   = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontSize = 14.sp)
                            }
                            OutlinedButton(onClick = { viewModel.cancelFriendRequest(targetUserId) }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Waiting...", fontSize = 14.sp)
                            }
                        }
                    }

                    FriendStatus.RECEIVED -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick  = { viewModel.acceptFriendRequest(targetUserId) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Accept", fontSize = 14.sp)
                            }
                            OutlinedButton(
                                onClick  = { viewModel.declineFriendRequest(targetUserId) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Decline", fontSize = 14.sp)
                            }
                        }
                    }

                    FriendStatus.NONE -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick  = {},
                                enabled  = false,
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors   = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", fontSize = 14.sp)
                            }
                            Button(onClick = { viewModel.sendFriendRequest(targetUserId) }, modifier = Modifier.weight(1f).height(50.dp)) {
                                Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Friend", fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
