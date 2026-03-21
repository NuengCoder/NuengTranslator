package com.nueng.translator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nueng.translator.data.local.dao.UserDao
import com.nueng.translator.ui.auth.LoginScreen
import com.nueng.translator.ui.auth.RegisterScreen
import com.nueng.translator.ui.chat.ChatScreen
import com.nueng.translator.ui.components.MainScaffold
import com.nueng.translator.ui.home.HomeScreen
import com.nueng.translator.ui.mynote.DirectoryWordsScreen
import com.nueng.translator.ui.mynote.MyNoteScreen
import com.nueng.translator.ui.settings.AdminPanelScreen
import com.nueng.translator.ui.settings.SettingsScreen
import com.nueng.translator.ui.settings.SettingsViewModel
import com.nueng.translator.ui.splash.SplashScreen
import com.nueng.translator.ui.study.GenericStudyScreen
import com.nueng.translator.ui.study.StudyScreen
import com.nueng.translator.ui.translate.CameraOcrScreen
import com.nueng.translator.ui.translate.StrokeDrawScreen
import com.nueng.translator.ui.translate.TranslateScreen
import com.nueng.translator.ui.translate.TranslateViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun NavGraph(navController: NavHostController, userDao: UserDao) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Splash.route) { inclusive = true } } },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Splash.route) { inclusive = true } } }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Login.route) { inclusive = true } } }
            )
        }

        composable(Screen.Home.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                HomeScreen(modifier = paddingModifier, onNavigateToChat = { navController.navigate(Screen.Chat.route) })
            }
        }

        composable(Screen.Translate.route) { backStackEntry ->
            MainScaffold(navController = navController) { paddingModifier ->
                val translateVm: TranslateViewModel = hiltViewModel()
                val strokeResult = backStackEntry.savedStateHandle.get<String>("stroke_result")
                val cameraResult = backStackEntry.savedStateHandle.get<String>("camera_result")
                LaunchedEffect(strokeResult) { strokeResult?.let { translateVm.onSearchQueryChange(it); backStackEntry.savedStateHandle.remove<String>("stroke_result") } }
                LaunchedEffect(cameraResult) { cameraResult?.let { translateVm.onSearchQueryChange(it); backStackEntry.savedStateHandle.remove<String>("camera_result") } }
                TranslateScreen(
                    modifier = paddingModifier,
                    onNavigateToStrokeDraw = { lang -> navController.navigate("stroke_draw/$lang") },
                    onNavigateToCamera = { lang -> navController.navigate("camera_ocr/$lang") }
                )
            }
        }

        composable(Screen.MyNote.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                MyNoteScreen(
                    modifier = paddingModifier,
                    onNavigateToDirectory = { dirId, dirName ->
                        val encoded = URLEncoder.encode(dirName, StandardCharsets.UTF_8.toString())
                        navController.navigate("directory_words/$dirId/$encoded")
                    }
                )
            }
        }

        composable(
            route = "directory_words/{directoryId}/{directoryName}",
            arguments = listOf(
                navArgument("directoryId") { type = NavType.LongType },
                navArgument("directoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val dirId = backStackEntry.arguments?.getLong("directoryId") ?: 0L
            val dirName = URLDecoder.decode(
                backStackEntry.arguments?.getString("directoryName") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            DirectoryWordsScreen(
                directoryId = dirId,
                directoryName = dirName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Study.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                StudyScreen(
                    modifier = paddingModifier,
                    onNavigateToStudyPack = { pack, file, type ->
                        navController.navigate("study_pack/$pack/$file/$type")
                    }
                )
            }
        }

        composable(Screen.Settings.route) {
            MainScaffold(navController = navController) { paddingModifier ->
                SettingsScreen(
                    modifier = paddingModifier,
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) { popUpTo(Screen.Home.route) { inclusive = true } } },
                    onNavigateToAdminPanel = { navController.navigate(Screen.AdminPanel.route) }
                )
            }
        }

        composable(Screen.AdminPanel.route) {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val uiState by settingsVm.uiState.collectAsState()
            AdminPanelScreen(onNavigateBack = { navController.popBackStack() }, adminUserId = uiState.user?.id ?: 0L)
        }

        composable(Screen.Chat.route) {
            ChatScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = "study_pack/{pack}/{file}/{type}",
            arguments = listOf(
                navArgument("pack") { type = NavType.StringType },
                navArgument("file") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pack = backStackEntry.arguments?.getString("pack") ?: ""
            val file = backStackEntry.arguments?.getString("file") ?: ""
            val type = backStackEntry.arguments?.getString("type") ?: "hsk"
            val isHsk = type == "hsk"

            val displayName = buildString {
                for (i in pack.indices) {
                    if (i > 0 && pack[i].isDigit() && !pack[i - 1].isDigit()) append(" ")
                    append(pack[i])
                }
            }

            GenericStudyScreen(
                onNavigateBack = { navController.popBackStack() },
                packName = displayName,
                jsonFileName = file,
                availableLangs = if (isHsk) listOf("en", "th", "lo", "vi", "id") else listOf("zh", "th", "lo", "vi", "id"),
                isSourceChinese = isHsk
            )
        }

        composable(
            route = "stroke_draw/{lang}",
            arguments = listOf(navArgument("lang") { type = NavType.StringType; defaultValue = "zh" })
        ) { entry ->
            StrokeDrawScreen(
                onNavigateBack = { navController.popBackStack() },
                onCharacterSelected = { navController.previousBackStackEntry?.savedStateHandle?.set("stroke_result", it); navController.popBackStack() },
                lang1 = entry.arguments?.getString("lang") ?: "zh"
            )
        }

        composable(
            route = "camera_ocr/{lang}",
            arguments = listOf(navArgument("lang") { type = NavType.StringType; defaultValue = "en" })
        ) { entry ->
            CameraOcrScreen(
                onNavigateBack = { navController.popBackStack() },
                onTextExtracted = { navController.previousBackStackEntry?.savedStateHandle?.set("camera_result", it); navController.popBackStack() },
                lang1 = entry.arguments?.getString("lang") ?: "en"
            )
        }
    }
}