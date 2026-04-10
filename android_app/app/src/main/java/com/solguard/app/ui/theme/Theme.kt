package com.solguard.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SolGuardColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCC80),
    onPrimaryContainer = Color(0xFFBF360C),
    secondary = Color(0xFFFFA000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFFFF6F00),
    tertiary = Color(0xFF7B1FA2),
    onTertiary = Color.White,
    background = Color(0xFFFFF8E1),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    error = Color(0xFFD32F2F),
    onError = Color.White,
)

@Composable
fun SolGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SolGuardColorScheme,
        typography = Typography(),
        content = content
    )
}
