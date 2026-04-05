package com.nueng.translator.ui.online

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

data class OnlineNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

object OnlineNavItems {
    val items = listOf(
        OnlineNavItem("OHome",     Icons.Default.Home,        "online_home"),
        OnlineNavItem("OGlobal",   Icons.Default.ChatBubble,  "online_global"),
        OnlineNavItem("OFriend",   Icons.Default.Groups,      "online_friend"),
        OnlineNavItem("OProfile",  Icons.Default.Person,      "online_profile"),
        OnlineNavItem("OSettings", Icons.Default.Settings,    "online_settings")
    )
}
