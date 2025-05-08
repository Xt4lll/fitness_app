package com.example.fitness_app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Aqua = Color(0xFF708D81)
val Green = Color(0xFF417040)
val Red = Color(0xFFF01611)
val GreenishCyan = Color(0xFF407060)

private val LightColorScheme = lightColorScheme(
    primary = GreenishCyan,         // Основной акцент
    onPrimary = Color.White,
    secondary = Green,        // Вторичный
    onSecondary = Color.White,
    tertiary = Aqua,        // Акцент
    onTertiary = Color.White,
    background = Color.White, // Белый фон
    surface = Color.White,    // Карточки, панели
    error = Red,          // Ошибки
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenishCyan,             // Основной акцент
    onPrimary = Color.White,
    secondary = Green,            // Вторичный
    onSecondary = Color.White,
    tertiary = Aqua,            // Акцент
    onTertiary = Color.White,
    background = Color(0xFF181818), // Почти чёрный фон
    onBackground = Color.White,
    onSurface = Color.White,
    error = Red,
    onError = Color.White
)

@Composable
fun FitnessAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}