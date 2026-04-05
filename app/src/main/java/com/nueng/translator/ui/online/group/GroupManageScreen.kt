package com.nueng.translator.ui.online.group

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManageScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToFileStorage: (groupId: String, groupName: String) -> Unit = { _, _ -> },
    viewModel: GroupManageViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }

    var kickTarget    by remember { mutableStateOf<ManageMemberItem?>(null) }
    var promoteTarget by remember { mutableStateOf<ManageMemberItem?>(null) }
    var demoteTarget  by remember { mutableStateOf<ManageMemberItem?>(null) }
    var showWipeDlg   by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) { viewModel.load(groupId) }
    LaunchedEffect(uiState.snackMessage) {
        if (uiState.snackMessage.isNotEmpty()) {
            snackbarState.showSnackbar(uiState.snackMessage)
            viewModel.clearSnack()
        }
    }
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) { viewModel.clearNavigateBack(); onNavigateBack() }
    }

    val isCreator = uiState.myRole == "creator"

    kickTarget?.let { t ->
        AlertDialog(onDismissRequest = { kickTarget = null },
            title = { Text("Remove ${t.displayName}?") },
            text  = { Text("Remove ${t.displayName} from the group?") },
            confirmButton = { TextButton(onClick = { viewModel.kick(t); kickTarget = null }) {
                Text("Remove", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { kickTarget = null }) { Text("Cancel") } })
    }
    promoteTarget?.let { t ->
        AlertDialog(onDismissRequest = { promoteTarget = null },
            title = { Text("Promote to Admin?") },
            text  = { Text("Make ${t.displayName} an admin?") },
            confirmButton = { TextButton(onClick = { viewModel.promote(t); promoteTarget = null }) {
                Text("Promote") } },
            dismissButton = { TextButton(onClick = { promoteTarget = null }) { Text("Cancel") } })
    }
    demoteTarget?.let { t ->
        AlertDialog(onDismissRequest = { demoteTarget = null },
            title = { Text("Demote to Member?") },
            text  = { Text("Remove admin role from ${t.displayName}?") },
            confirmButton = { TextButton(onClick = { viewModel.demote(t); demoteTarget = null }) {
                Text("Demote", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { demoteTarget = null }) { Text("Cancel") } })
    }
    if (showWipeDlg) {
        AlertDialog(onDismissRequest = { showWipeDlg = false },
            title = { Text("Wipe Entire Chat?", color = MaterialTheme.colorScheme.error) },
            text  = { Text("Permanently delete ALL messages in ${uiState.groupName}?") },
            confirmButton = { TextButton(onClick = { viewModel.wipeChat(); showWipeDlg = false }) {
                Text("Wipe", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showWipeDlg = false }) { Text("Cancel") } })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Group Manage Panel", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                actions = {
                    if (isCreator) {
                        IconButton(onClick = { showWipeDlg = true }) {
                            Icon(Icons.Default.DeleteForever, "Wipe Chat",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { onNavigateToFileStorage(groupId, uiState.groupName) }) {
                        Icon(Icons.Default.Folder, "File Storage",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {

                // ── Group Profile Header ──────────────────────────────────────
                item {
                    Spacer(Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(80.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center) {
                            Text(uiState.groupName.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                lineHeight = 36.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (uiState.myRole in listOf("creator","admin")) {
                            var editingName by remember { mutableStateOf(false) }
                            var nameInput   by remember { mutableStateOf(uiState.groupName) }
                            if (editingName) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it },
                                        modifier = Modifier.weight(1f), singleLine = true,
                                        label = { Text("Group Name") }, shape = RoundedCornerShape(12.dp))
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.renameGroup(nameInput); editingName = false }) {
                                        Icon(Icons.Default.Check, "Save", tint = MaterialTheme.colorScheme.primary) }
                                    IconButton(onClick = { editingName = false; nameInput = uiState.groupName }) {
                                        Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.error) }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { editingName = true; nameInput = uiState.groupName }) {
                                    Text(uiState.groupName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        } else {
                            Text(uiState.groupName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${uiState.members.size} members", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                }

                // ── Join Requests Section (collapsible) ──────────────────────
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { viewModel.toggleRequestsExpanded() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.requests.isNotEmpty()) {
                            Box(modifier = Modifier.size(8.dp).background(
                                MaterialTheme.colorScheme.error,
                                CircleShape))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("Join Requests (${uiState.requests.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (uiState.requestsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                // Auto-accept toggle
                if (uiState.requestsExpanded) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Accept New Members", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("Anyone with an invite link joins instantly",
                                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = uiState.autoAccept, onCheckedChange = { viewModel.toggleAutoAccept() })
                        }
                    }
                    if (uiState.requests.isEmpty()) {
                        item {
                            Text("No pending requests", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                        }
                    } else {
                        items(uiState.requests, key = { "req_${it.username}" }) { req ->
                            RequestCard(req = req,
                                onAccept = { viewModel.acceptRequest(req) },
                                onReject = { viewModel.rejectRequest(req) })
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                }

                // ── Pending Invites Section ───────────────────────────────────
                if (uiState.pendingInvites.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(
                                MaterialTheme.colorScheme.error, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("Pending Invites (${uiState.pendingInvites.size})",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                    items(uiState.pendingInvites, key = { it.inviteeUsername }) { invite ->
                        InviteCard(
                            invite    = invite,
                            onAccept  = { viewModel.acceptInvite(invite) },
                            onReject  = { viewModel.rejectInvite(invite) }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
                }

                // ── Members Section (collapsible) ─────────────────────────────
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleMembersExpanded() }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Members (${uiState.members.size})",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (uiState.membersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Search bar (only when expanded)
                item {
                    AnimatedVisibility(visible = uiState.membersExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                        OutlinedTextField(
                            value         = uiState.searchQuery,
                            onValueChange = { viewModel.onSearch(it) },
                            modifier      = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            placeholder   = { Text("Search members...") },
                            leadingIcon   = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            singleLine    = true,
                            shape         = RoundedCornerShape(24.dp)
                        )
                    }
                }

                // Member cards
                if (uiState.membersExpanded) {
                    items(uiState.filteredMembers, key = { it.username }) { member ->
                        val isMe      = member.username == uiState.myUsername
                        val canKick   = !isMe && when (uiState.myRole) {
                            "creator" -> member.role != "creator"
                            "admin"   -> member.role == "member"
                            else      -> false
                        }
                        val canPromote = !isMe && isCreator && member.role == "member"
                        val canDemote  = !isMe && isCreator && member.role == "admin"
                        ManageMemberCard(
                            member     = member,
                            isMe       = isMe,
                            canKick    = canKick,
                            canPromote = canPromote,
                            canDemote  = canDemote,
                            onKick     = { kickTarget    = member },
                            onPromote  = { promoteTarget = member },
                            onDemote   = { demoteTarget  = member }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RequestCard(req: GroupRequest, onAccept: () -> Unit, onReject: () -> Unit) {
    val tf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape    = RoundedCornerShape(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(letter = req.avatarLetter, sizeDp = 38, username = req.username)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(req.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Invited by @${req.invitedBy} (${req.invitedByRole})", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (req.timestamp > 0)
                    Text(tf.format(Date(req.timestamp)), fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            IconButton(onClick = onAccept, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onReject, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun InviteCard(invite: PendingInvite, onAccept: () -> Unit, onReject: () -> Unit) {
    val tf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(letter = invite.inviteeUsername.firstOrNull()?.uppercaseChar() ?: '?', sizeDp = 38, username = invite.inviteeUsername)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.inviteeUsername, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("Invited by ${invite.fromUsername}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (invite.timestamp > 0) {
                    Text(tf.format(Date(invite.timestamp)), fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
            IconButton(onClick = onAccept, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onReject, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Reject", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ManageMemberCard(
    member: ManageMemberItem, isMe: Boolean,
    canKick: Boolean, canPromote: Boolean, canDemote: Boolean,
    onKick: () -> Unit, onPromote: () -> Unit, onDemote: () -> Unit
) {
    val roleColor = when (member.role) { "creator"->Color(0xFFFFA000); "admin"->Color(0xFF00BCD4); else->Color.Gray }
    val roleLabel = when (member.role) { "creator"->"Creator"; "admin"->"Admin"; else->"Member" }
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape  = RoundedCornerShape(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AvatarCircle(letter = member.avatarLetter, sizeDp = 42, username = member.username)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.displayName + if (isMe) " (You)" else "", fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${member.username}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                androidx.compose.material3.Surface(color = roleColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(top = 3.dp)) {
                    Text(roleLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = roleColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            if (canPromote) {
                IconButton(onClick = onPromote, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, "Promote",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (canDemote) {
                IconButton(onClick = onDemote, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, "Demote",
                        tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
            }
            if (canKick) {
                IconButton(onClick = onKick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PersonRemove, "Kick",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
