package com.nueng.translator.ui.online.friend

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun OnlineFriendScreen(
    modifier: Modifier = Modifier,
    onNavigateToAddFriend: () -> Unit = {},
    onNavigateToAddGroup: () -> Unit = {},
    onSwitchToProfile: () -> Unit = {},
    onNavigateToBot: () -> Unit = {},
    onNavigateToSelfChat: (chatId: String, selfUserId: String) -> Unit = { _, _ -> },
    onNavigateToFriendChat: (chatId: String, friendUserId: String) -> Unit = { _, _ -> },
    onNavigateToGroupChat: (groupId: String) -> Unit = {},
    viewModel: OnlineFriendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var groupsExpanded  by rememberSaveable { mutableStateOf(true) }
    var friendsExpanded by rememberSaveable { mutableStateOf(true) }

    Column(modifier = modifier.fillMaxSize()) {

        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("NuengChat", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector        = if (uiState.isConnected) Icons.Default.Cloud else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint               = if (uiState.isConnected) Color(0xFF4CAF50) else Color(0xFFEF5350),
                        modifier           = Modifier.size(16.dp)
                    )
                }
            },
            actions = {
                // + Friend icon with pending request badge
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onNavigateToAddFriend) {
                        Icon(Icons.Default.PersonAdd, "Add Friend",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    if (uiState.pendingRequestCount > 0) {
                        val badgeText = if (uiState.pendingRequestCount > 9) "9+"
                                        else uiState.pendingRequestCount.toString()
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 2.dp, end = 2.dp)
                                .size(if (uiState.pendingRequestCount > 9) 19.dp else 16.dp)
                                .background(
                                    Color(0xFFFFC107),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = badgeText,
                                fontSize   = if (uiState.pendingRequestCount > 9) 7.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight  = if (uiState.pendingRequestCount > 9) 7.sp else 8.sp,
                                textAlign   = androidx.compose.ui.text.style.TextAlign.Center,
                                color      = Color.Black
                            )
                        }
                    }
                }
                IconButton(onClick = onNavigateToAddGroup) {
                    Icon(Icons.Default.GroupAdd, "Add Group", tint = MaterialTheme.colorScheme.primary)
                }
                Box(
                    modifier         = Modifier
                        .padding(end = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onSwitchToProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = "My Profile",
                        tint               = MaterialTheme.colorScheme.onSurface,
                        modifier           = Modifier.size(24.dp)
                    )
                }
            }
        )

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            item {
                BotCard(onClick = onNavigateToBot)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                val selfId      = uiState.myUserId
                val selfDisplay = uiState.myDisplayName.ifBlank { selfId }
                val selfChatId  = if (selfId.isNotEmpty()) "${selfId}_${selfId}" else ""
                SelfChatCard(
                    displayName = selfDisplay,
                    onClick     = {
                        if (selfChatId.isNotEmpty()) onNavigateToSelfChat(selfChatId, selfId)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (uiState.groups.isNotEmpty()) {
                item {
                    CollapsibleSectionHeader(
                        title      = "Groups",
                        count      = uiState.groups.size,
                        isExpanded = groupsExpanded,
                        onToggle   = { groupsExpanded = !groupsExpanded }
                    )
                }
                item {
                    AnimatedVisibility(
                        visible = groupsExpanded,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Column {
                            uiState.groups.forEach { group ->
                                GroupChatCard(
                                    group       = group,
                                    unreadCount = group.unreadCount,
                                    onClick     = { onNavigateToGroupChat(group.groupId) }
                                )
                            }
                        }
                    }
                }
                item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }
            }

            item {
                CollapsibleSectionHeader(
                    title      = "Friends",
                    count      = uiState.friends.size,
                    isExpanded = friendsExpanded,
                    onToggle   = { friendsExpanded = !friendsExpanded }
                )
            }
            item {
                AnimatedVisibility(
                    visible = friendsExpanded,
                    enter   = expandVertically(),
                    exit    = shrinkVertically()
                ) {
                    Column {
                        if (uiState.friends.isEmpty()) {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No friends yet. Tap + to add someone!",
                                    fontSize = 14.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            uiState.friends.forEach { friend ->
                                FriendCard(
                                    friend      = friend,
                                    unreadCount = friend.unreadCount,
                                    onClick     = { onNavigateToFriendChat(friend.chatId, friend.userId) }
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = if (isExpanded) Icons.Default.KeyboardArrowDown
                                 else Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text       = title,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.weight(1f)
        )
        if (count > 0) {
            Text(count.toString(), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BotCard(onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SmartToy, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text("NuengChatBot", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface)
            Text("Tap to chat with AI bot", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelfChatCard(displayName: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.BookmarkBorder, null,
                tint     = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text       = "My Notes",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text    = "Chat with yourself • $displayName",
                fontSize = 12.sp,
                color   = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupChatCard(group: GroupItem, onClick: () -> Unit, unreadCount: Int = 0) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge {
                        Text(
                            text     = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        ) {
            AvatarCircle(letter = group.avatarLetter, sizeDp = 48)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(group.groupName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                if (group.lastMessageTime > 0) {
                    Text(formatTime(group.lastMessageTime), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val preview = group.lastMessage.ifBlank { group.memberCount.toString() + " members" }
            Text(preview, fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FriendCard(friend: FriendItem, onClick: () -> Unit, unreadCount: Int = 0) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge {
                        Text(
                            text     = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        ) {
            AvatarCircle(letter = friend.avatarLetter, sizeDp = 48, username = friend.userId)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(friend.displayName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                if (friend.lastMessageTime > 0) {
                    Text(formatTime(friend.lastMessageTime), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val preview = friend.lastMessage.ifBlank { "Tap to start chatting" }
            Text(preview, fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return if (diff < 24 * 60 * 60 * 1000L)
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    else
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
}
