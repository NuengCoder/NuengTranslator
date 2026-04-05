package com.nueng.translator.ui.online.group

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateBackToFriendList: () -> Unit = {},
    onNavigateToMemberProfile: (userId: String) -> Unit = {},
    onNavigateToFileStorage: (groupId: String, groupName: String) -> Unit = { _, _ -> },
    onNavigateToGroupManage: (groupId: String) -> Unit = {},
    onNavigateToAddFriendToGroup: (groupId: String) -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsState()
    val snackbarState   = remember { SnackbarHostState() }
    var showLeaveDialog  by remember { mutableStateOf(false) }
    var showWipeDialog   by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteInput      by remember { mutableStateOf("") }
    var kickTarget      by remember { mutableStateOf<GroupMemberItem?>(null) }
    var grantTarget     by remember { mutableStateOf<GroupMemberItem?>(null) }
    var revokeTarget    by remember { mutableStateOf<GroupMemberItem?>(null) }
    var showAllMembers  by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) { viewModel.load(groupId) }

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

    val isCreator      = uiState.myRole == "creator"
    val isAdmin        = uiState.myRole == "admin" || isCreator
    val displayMembers = if (showAllMembers) uiState.members else uiState.members.take(5)
    val hasMore        = uiState.members.size > 5 && !showAllMembers

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = uiState.groupName.ifBlank { "Group Info" },
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Any member can invite friends
                    IconButton(onClick = { onNavigateToAddFriendToGroup(groupId) }) {
                        Icon(Icons.Default.GroupAdd, "Add Member",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    // Admin/creator gets manage panel
                    if (uiState.myRole == "creator" || uiState.myRole == "admin") {
                        IconButton(onClick = { onNavigateToGroupManage(groupId) }) {
                            Icon(Icons.Default.Settings, "Manage Group",
                                tint = MaterialTheme.colorScheme.primary)
                        }
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color       = MaterialTheme.colorScheme.primary,
                    modifier    = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
            } else {
                AvatarCircle(letter = uiState.groupAvatarLetter, sizeDp = 80)
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.groupName, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Text(uiState.members.size.toString() + " members", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Members", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                    if (isAdmin) {
                        Surface(shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                color    = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                displayMembers.forEach { member ->
                    MemberCard(
                        member      = member,
                        isMe        = member.userId == uiState.myUserId,
                        onTapAvatar = { onNavigateToMemberProfile(member.userId) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (hasMore) {
                    TextButton(onClick = { showAllMembers = true }) {
                        Text("+" + (uiState.members.size - 5) + " more members...",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.members.size > 5 && showAllMembers) {
                    TextButton(onClick = { showAllMembers = false }) {
                        Text("Show less", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick  = { onNavigateToFileStorage(groupId, uiState.groupName) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.groupName + " File Storage", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick  = { showLeaveDialog = true },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,   // fixed — AutoMirrored
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Leave Group", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false; inviteInput = "" },
            title = { Text("Invite Member") },
            text  = {
                Column {
                    Text("Enter the username of the person to invite:",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value         = inviteInput,
                        onValueChange = { inviteInput = it },
                        placeholder   = { Text("username") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendInvite(inviteInput.trim())
                    showInviteDialog = false
                    inviteInput = ""
                }) { Text("Send Invite") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false; inviteInput = "" }) { Text("Cancel") }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title   = { Text("Leave Group?") },
            text    = { Text("You will leave " + uiState.groupName + ". You can be added back by a member.") },
            confirmButton = {
                TextButton(onClick = { viewModel.leaveGroup(); showLeaveDialog = false }) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") } }
        )
    }

    kickTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            title   = { Text("Remove Member?") },
            text    = { Text("Remove " + target.displayName + " from the group?") },
            confirmButton = {
                TextButton(onClick = { viewModel.kickMember(target.userId); kickTarget = null }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { kickTarget = null }) { Text("Cancel") } }
        )
    }

    grantTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { grantTarget = null },
            title   = { Text("Grant Admin?") },
            text    = { Text("Make ${target.displayName} an admin of this group?") },
            confirmButton = {
                TextButton(onClick = { viewModel.grantAdmin(target.userId); grantTarget = null }) {
                    Text("Grant Admin")
                }
            },
            dismissButton = { TextButton(onClick = { grantTarget = null }) { Text("Cancel") } }
        )
    }

    revokeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { revokeTarget = null },
            title   = { Text("Revoke Admin?") },
            text    = { Text("Remove admin role from ${target.displayName}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.revokeAdmin(target.userId); revokeTarget = null }) {
                    Text("Revoke", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { revokeTarget = null }) { Text("Cancel") } }
        )
    }

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title   = { Text("Wipe Entire Chat?", color = MaterialTheme.colorScheme.error) },
            text    = { Text("This will permanently delete ALL messages in ${uiState.groupName}. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.wipeChat(); showWipeDialog = false }) {
                    Text("Wipe", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showWipeDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MemberCard(
    member: GroupMemberItem,
    isMe: Boolean,
    onTapAvatar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(
                letter   = member.avatarLetter,
                sizeDp   = 40,
                username = member.userId,
                modifier = if (!isMe) Modifier.clickable { onTapAvatar() } else Modifier
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = member.displayName + if (isMe) " (You)" else "",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                when (member.role) {
                    "creator" -> Text("Creator", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFA000))
                    "admin"   -> Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    else      -> Text("Member", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}
