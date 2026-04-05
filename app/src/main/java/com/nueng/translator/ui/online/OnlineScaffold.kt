package com.nueng.translator.ui.online

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nueng.translator.ui.online.friend.OnlineFriendViewModel

@Composable
fun OnlineScaffold(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val friendVm: OnlineFriendViewModel = hiltViewModel()
    val friendState by friendVm.uiState.collectAsState()
    val friendBadge = friendState.friendUnreadTotal + friendState.pendingRequestCount
    val globalBadge = friendState.globalUnreadCount

    Scaffold(
        bottomBar = {
            NavigationBar {
                OnlineNavItems.items.forEach { item ->
                    val badgeCount = when (item.route) {
                        "online_friend" -> friendBadge
                        "online_global" -> globalBadge
                        else            -> 0
                    }
                    NavigationBarItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (badgeCount > 0) {
                                        Badge {
                                            Text(
                                                text     = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                                fontSize = 8.sp
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector        = item.icon,
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = {
                            Text(
                                text     = item.label,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        selected = currentRoute == item.route,
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo("online_home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
