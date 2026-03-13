package com.nueng.translator.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Translate : Screen("translate")
    object MyNote : Screen("my_note")
    object Study : Screen("study")
    object Settings : Screen("settings")
    object Chat : Screen("chat")
    object AdminPanel : Screen("admin_panel")
}
