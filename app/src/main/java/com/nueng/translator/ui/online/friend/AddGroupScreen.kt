package com.nueng.translator.ui.online.friend

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nueng.translator.ui.online.settings.AvatarCircle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGroupChat: (groupId: String) -> Unit = {},
    viewModel: AddGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate to group chat once created
    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId ->
            onNavigateToGroupChat(groupId)
            viewModel.onNavigatedToGroup()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = "Make a Group",
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
                    // Balance centering
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = Color.Transparent)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Group name input ───────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value         = uiState.groupName,
                    onValueChange = { viewModel.onGroupNameChange(it) },
                    label         = { Text("Group Name") },
                    placeholder   = { Text("Enter group name...") },
                    leadingIcon   = {
                        Icon(Icons.Default.Groups, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    isError       = uiState.errorMessage.isNotEmpty()
                )
                if (uiState.errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(uiState.errorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

            // ── Friend selection header ────────────────────────
            Row(
                modifier          = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Select Friends",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )
                val selectedCount = uiState.friends.count { it.isSelected }
                if (selectedCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = "$selectedCount selected",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Friend list ────────────────────────────────────
            if (uiState.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                }
            } else if (uiState.friends.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("No friends yet!", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add friends first to create a group.", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items = uiState.friends, key = { it.userId }) { friend ->
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleFriend(friend.userId) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Checkbox
                            Icon(
                                imageVector        = if (friend.isSelected)
                                                         Icons.Default.CheckBox
                                                     else
                                                         Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (friend.isSelected) "Selected" else "Not selected",
                                tint               = if (friend.isSelected)
                                                         MaterialTheme.colorScheme.primary
                                                     else
                                                         MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier           = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            AvatarCircle(letter = friend.avatarLetter, sizeDp = 44, username = friend.userId)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text       = friend.displayName,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(start = 82.dp))
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            // ── Create Group button ────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Button(
                    onClick  = { viewModel.createGroup() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled  = !uiState.isCreating && uiState.groupName.isNotBlank()
                              && uiState.friends.any { it.isSelected }
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(22.dp),
                            color       = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Groups, null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val count = uiState.friends.count { it.isSelected }
                        val label = if (count > 0) "Create Group ($count members + you)"
                                    else "Create Group"
                        Text(label, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
