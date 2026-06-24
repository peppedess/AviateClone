package com.aviateclone.launcher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colori semantici extra non coperti dallo schema Material3 standard.
 * Stesso pattern di TreniExtraColors in Treni Tracker: una data class
 * Immutable esposta tramite CompositionLocal.
 */
@Immutable
data class AviateExtraColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    // tinte per le card contestuali dello Smart Stream
    val weatherTint: Color,
    val calendarTint: Color,
    val notifTint: Color,
    val mediaTint: Color
)

val LightExtraColors = AviateExtraColors(
    success    = Color(0xFF1B7B45),
    onSuccess  = Color(0xFFFFFFFF),
    warning    = Color(0xFFC84B00),
    onWarning  = Color(0xFFFFFFFF),
    info       = Color(0xFF1565C0),
    onInfo     = Color(0xFFFFFFFF),
    weatherTint  = Color(0xFF1565C0),
    calendarTint = Color(0xFF7B2FBE),
    notifTint    = Color(0xFFC2185B),
    mediaTint    = Color(0xFF00695C)
)

val DarkExtraColors = AviateExtraColors(
    success    = Color(0xFF7BDBA2),
    onSuccess  = Color(0xFF00391C),
    warning    = Color(0xFFFFB68A),
    onWarning  = Color(0xFF4A1F00),
    info       = Color(0xFFA8C8FF),
    onInfo     = Color(0xFF002F66),
    weatherTint  = Color(0xFFA8C8FF),
    calendarTint = Color(0xFFE5BAD9),
    notifTint    = Color(0xFFFFB1C8),
    mediaTint    = Color(0xFF7BD8C7)
)

val LocalAviateExtraColors = staticCompositionLocalOf { LightExtraColors }
