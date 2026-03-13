package com.nueng.translator.ui.theme

import androidx.compose.ui.graphics.Color

// Dark theme colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2A2A2A)
val DarkOnBackground = Color.White
val DarkOnSurface = Color.White
val DarkOnSurfaceVariant = Color(0xFFB0B0B0)
val DarkPrimary = Color(0xFF00BCD4) // Aqua
val DarkOnPrimary = Color.Black
val DarkSecondary = Color(0xFF64B5F6)
val DarkError = Color(0xFFEF5350)

// Light theme colors
val LightBackground = Color(0xFFFAFAFA) // Snow
val LightSurface = Color.White
val LightSurfaceVariant = Color(0xFFF0F0F0)
val LightOnBackground = Color(0xFF1A1A1A)
val LightOnSurface = Color(0xFF1A1A1A)
val LightOnSurfaceVariant = Color(0xFF666666)
val LightPrimary = Color(0xFFC2185B) // Magenta
val LightOnPrimary = Color.White
val LightSecondary = Color(0xFF7B1FA2)
val LightError = Color(0xFFD32F2F)

// Word cards: ALWAYS dark bg + white text in both themes
// Only border color changes: aqua (dark) vs magenta (light)
val CardDarkBg = Color(0xFF1E1E1E)
val CardTextPrimary = Color.White
val CardTextSecondary = Color.White.copy(alpha = 0.7f)
val CardTextHint = Color.White.copy(alpha = 0.5f)

val DarkCardBorder = Color(0xFF00BCD4) // Aqua
val LightCardBorder = Color(0xFFC2185B) // Magenta

// My Note card: same dark style
val NoteCardDarkBg = Color(0xFF2A2A2A)
val DarkNoteBorder = Color(0xFF64B5F6) // Light blue
val LightNoteBorder = Color(0xFFC2185B) // Magenta

// Admin colors (fixed)
val AdminAqua = Color(0xFF00BCD4)
val AdminDarkBlue = Color(0xFF0A1929)
val AdminDarkBlueSurface = Color(0xFF0D47A1)
