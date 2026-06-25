package com.aviateclone.launcher.ui.theme

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveDarkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Tema dell'app in Material 3 Expressive: Material You dinamico su Android 12+,
 * fallback alla palette expressive sotto, motion fisico a molla di default.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AviateCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Forza una ricomposizione quando il wallpaper cambia, così la palette
    // dynamicColor viene ricalcolata anche se al primo avvio il sistema non
    // aveva ancora pronta l'estrazione colori per lo sfondo corrente.
    var wallpaperTick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) { wallpaperTick++ }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val colorScheme = remember(darkTheme, dynamicColor, wallpaperTick) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            darkTheme -> expressiveDarkColorScheme()
            else      -> expressiveLightColorScheme()
        }
    }
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalAviateExtraColors provides extraColors) {
        MaterialExpressiveTheme(
            colorScheme  = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography   = AviateTypography,
            shapes       = AviateShapes,
            content      = content
        )
    }
}

/** Accesso comodo ai colori extra: AviateTheme.extra.weatherTint */
object AviateTheme {
    val extra: AviateExtraColors
        @Composable get() = LocalAviateExtraColors.current
}
