package com.nueng.translator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    secondary        = DarkSecondary,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurfaceVariant,
    onBackground     = DarkOnBackground,
    onSurface        = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error            = DarkError
)

private val LightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    secondary        = LightSecondary,
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightSurfaceVariant,
    onBackground     = LightOnBackground,
    onSurface        = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    error            = LightError
)

// 0L means "use default"
fun Long.toCustomColor(): Color? = if (this == 0L) null else Color(this.toULong().toInt())

@Composable
fun NuengTranslatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customFg:      Long = 0L,
    customBg:      Long = 0L,
    customText:    Long = 0L,
    customAppText: Long = 0L,
    content: @Composable () -> Unit
) {
    val base = if (darkTheme) DarkColorScheme else LightColorScheme

    // Apply custom overrides if set
    val colorScheme = base.copy(
        background       = customBg.toCustomColor()      ?: base.background,
        surface          = customBg.toCustomColor()      ?: base.surface,
        primary          = customFg.toCustomColor()      ?: base.primary,
        onBackground     = customText.toCustomColor()    ?: base.onBackground,
        onSurface        = customText.toCustomColor()    ?: base.onSurface,
        onSurfaceVariant = customAppText.toCustomColor() ?: base.onSurfaceVariant
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
