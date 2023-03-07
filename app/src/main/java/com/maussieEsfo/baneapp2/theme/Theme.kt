package com.maussieEsfo.baneapp2.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import com.maussieEsfo.baneapp2.settings.Settings

private val DarkColorPalette = darkColors(
    primary = Purple200,
    primaryVariant = Purple700,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = Purple500,
    primaryVariant = Purple700,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun Baneapp2Theme(settings: Settings, content: @Composable () -> Unit) {
    val colors = darkColors(
        background = settings.bg_color.value,
        surface = settings.fg_color.value,
        onBackground = settings.text_color.value,
        onSurface = settings.text_color.value
    )

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}