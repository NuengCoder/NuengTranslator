package com.nueng.translator.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.ui.auth.LoginScreen
import com.nueng.translator.ui.auth.RegisterScreen
import com.nueng.translator.ui.chat.ChatScreen
import com.nueng.translator.ui.components.MainScaffold
import com.nueng.translator.ui.home.HomeScreen
import com.nueng.translator.ui.mynote.MyNoteScreen
import com.nueng.translator.ui.settings.AdminPanelScreen
import com.nueng.translator.ui.settings.SettingsScreen
import com.nueng.translator.ui.settings.SettingsViewModel
import com.nueng.translator.ui.splash.SplashScreen
import com.nueng.translator.ui.study.StudyScreen
import com.nueng.translator.ui.translate.TranslateScreen
import com.nueng.translator.ui.translate.TranslateViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    userDao: UserDao
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.Home.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                HomeScreen(
                    modifier = paddingModifier,
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }
        }

        composable(Screen.Translate.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                val settingsVm: SettingsViewModel = hiltViewModel()
                val settingsState by settingsVm.uiState.collectAsState()
                TranslateScreen(modifier = paddingModifier, isAdmin = settingsState.isAdmin)
            }
        }

        composable(Screen.MyNote.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                val translateVm: TranslateViewModel = hiltViewModel()
                val lang1 by translateVm.lang1.collectAsState()
                val lang2 by translateVm.lang2.collectAsState()
                MyNoteScreen(modifier = paddingModifier, lang1 = lang1, lang2 = lang2)
            }
        }

        composable(Screen.Study.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                StudyScreen(modifier = paddingModifier)
            }
        }

        composable(Screen.Settings.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                SettingsScreen(
                    modifier = paddingModifier,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) { popUpTo(Screen.Home.route) { inclusive = true } }
                    },
                    onNavigateToAdminPanel = { navController.navigate(Screen.AdminPanel.route) }
                )
            }
        }

        composable(Screen.AdminPanel.route) {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val uiState by settingsVm.uiState.collectAsState()
            AdminPanelScreen(
                onNavigateBack = { navController.popBackStack() },
                adminUserId = uiState.user?.id ?: 0L,
                userDao = userDao
            )
        }

        composable(Screen.Chat.route) {
            ChatScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
