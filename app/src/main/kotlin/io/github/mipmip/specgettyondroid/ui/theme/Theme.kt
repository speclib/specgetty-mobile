package io.github.mipmip.specgettyondroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF1B2A41)
private val Paper = Color(0xFFCFE3F2)
private val Mint = Color(0xFF2E7D5B)
private val MintLight = Color(0xFF7FC8A9)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = Color.White,
    primaryContainer = Paper,
    onPrimaryContainer = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    secondary = MintLight,
    onSecondary = Ink,
    primaryContainer = Ink,
    onPrimaryContainer = Paper,
)

@Composable
fun SpecgettyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
