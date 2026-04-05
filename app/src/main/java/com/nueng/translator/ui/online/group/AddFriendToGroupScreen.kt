package com.nueng.translator.ui.online.group

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendToGroupScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    viewModel: AddFriendToGroupViewModel = hiltViewModel()
) {
    val uiState      by viewModel.uiState.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    LaunchedEffect(groupId) { viewModel.load(groupId) }
    LaunchedEffect(uiState.sendDone) {
        if (uiState.sendDone) onNavigateBack()
    }
    LaunchedEffect(uiState.snackMessage) {
        if (uiState.snackMessage.isNotEmpty()) {
            snackbarState.showSnackbar(uiState.snackMessage)
            viewModel.clearSnack()
        }
    }

    val selected = uiState.friends.count { it.isSelected }
    val isAdminOrCreator = uiState.myRole in listOf("admin", "creator")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = {
                    Column {
                        Text("Add to ${uiState.groupName.ifBlank { "Group" }}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (isAdminOrCreator)
                            Text("Selected friends will be added directly", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        else
                            Text("Invite will be sent for admin approval", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (selected > 0) {
                        Button(onClick = { viewModel.sendInvites() },
                            modifier = Modifier.padding(end = 8.dp)) {
                            Text(if (isAdminOrCreator) "Add ($selected)" else "Invite ($selected)", fontSize = 13.sp)
                        }
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
        } else if (uiState.friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No friends to invite.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
                item { Spacer(Modifier.height(8.dp)) }
                items(uiState.friends, key = { it.username }) { friend ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            .clickable(enabled = !friend.alreadyMember) { viewModel.toggle(friend.username) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selection indicator
                        Box(
                            modifier = Modifier.size(24.dp)
                                .clip(CircleShape)
                                .border(2.dp,
                                    if (friend.isSelected) MaterialTheme.colorScheme.primary
                                    else if (friend.alreadyMember) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f)
                                    else MaterialTheme.colorScheme.outline, CircleShape)
                                .then(if (friend.isSelected)
                                    Modifier.then(Modifier)
                                else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (friend.isSelected)
                                Icon(Icons.Default.Check, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        AvatarCircle(letter = friend.avatarLetter, sizeDp = 40, username = friend.username)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(friend.displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = if (friend.alreadyMember) MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                        else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("@${friend.username}", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
                        }
                        if (friend.alreadyMember) {
                            Text("Already in group", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary.copy(0.7f))
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
