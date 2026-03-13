package com.nueng.translator.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

object BottomNavItems {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home, Screen.Home.route),
        BottomNavItem("Translate", Icons.Default.Translate, Screen.Translate.route),
        BottomNavItem("My Note", Icons.Default.NoteAlt, Screen.MyNote.route),
        BottomNavItem("Study", Icons.AutoMirrored.Filled.MenuBook, Screen.Study.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
    )
}
