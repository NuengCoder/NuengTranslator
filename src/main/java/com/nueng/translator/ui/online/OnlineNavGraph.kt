package com.nueng.translator.ui.online

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nueng.translator.ui.online.friend.OnlineFriendScreen
import com.nueng.translator.ui.online.global.OnlineGlobalScreen
import com.nueng.translator.ui.online.home.OnlineHomeScreen
import com.nueng.translator.ui.online.profile.OnlineProfileScreen
import com.nueng.translator.ui.online.settings.OnlineSettingsScreen

@Composable
fun OnlineNavGraph(
    onlineNavController: NavHostController,
    onNavigateBackToHome: () -> Unit
) {
    NavHost(
        navController      = onlineNavController,
        startDestination   = "online_home"
    ) {
        composable("online_home") {
            OnlineScaffold(navController = onlineNavController) { paddingModifier ->
                OnlineHomeScreen(
                    modifier       = paddingModifier,
                    onNavigateBack = onNavigateBackToHome
                )
            }
        }

        composable("online_global") {
            OnlineScaffold(navController = onlineNavController) { paddingModifier ->
                OnlineGlobalScreen(modifier = paddingModifier)
            }
        }

        composable("online_friend") {
            OnlineScaffold(navController = onlineNavController) { paddingModifier ->
                OnlineFriendScreen(modifier = paddingModifier)
            }
        }

        composable("online_profile") {
            OnlineScaffold(navController = onlineNavController) { paddingModifier ->
                OnlineProfileScreen(modifier = paddingModifier)
            }
        }

        composable("online_settings") {
            OnlineScaffold(navController = onlineNavController) { paddingModifier ->
                OnlineSettingsScreen(modifier = paddingModifier)
            }
        }
    }
}
