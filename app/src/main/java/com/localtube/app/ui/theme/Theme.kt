package com.localtube.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF7ED)
val SettingsBg = Color(0xFFF4F1EA)
val KidBg = Color(0xFFE3F3F6)
val Coral = Color(0xFFFF6B6B)
val Ink = Color(0xFF2B2B2B)
val Blue = Color(0xFF4D96FF)
val Green = Color(0xFF6BCB77)
val Purple = Color(0xFF9B5DE5)
val Teal = Color(0xFF2EC4B6)
val Orange = Color(0xFFFF9F45)

val CategoryColors = listOf(Coral, Blue, Green, Purple, Orange, Teal)

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

@Composable
fun LocalTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content,
    )
}
