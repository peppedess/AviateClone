package com.aviateclone.launcher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary             = AviatePrimaryLight,
    onPrimary           = AviateOnPrimaryLight,
    primaryContainer    = AviatePrimaryContainerLight,
    onPrimaryContainer  = AviateOnPrimaryContainerLight,
    secondary           = AviateSecondaryLight,
    onSecondary         = AviateOnSecondaryLight,
    secondaryContainer  = AviateSecondaryContainerLight,
    onSecondaryContainer = AviateOnSecondaryContainerLight,
    tertiary            = AviateTertiaryLight,
    onTertiary          = AviateOnTertiaryLight,
    surface             = AviateSurfaceLight,
    surfaceVariant      = AviateSurfaceVariantLight,
    onSurface           = AviateOnSurfaceLight,
    onSurfaceVariant    = AviateOnSurfaceVariantLight,
    outline             = AviateOutlineLight,
    outlineVariant      = AviateOutlineVariantLight
)

private val DarkColors = darkColorScheme(
    primary             = AviatePrimaryDark,
    onPrimary           = AviateOnPrimaryDark,
    primaryContainer    = AviatePrimaryContainerDark,
    onPrimaryContainer  = AviateOnPrimaryContainerDark,
    secondary           = AviateSecondaryDark,
    onSecondary         = AviateOnSecondaryDark,
    secondaryContainer  = AviateSecondaryContainerDark,
    onSecondaryContainer = AviateOnSecondaryContainerDark,
    tertiary            = AviateTertiaryDark,
    onTertiary          = AviateOnTertiaryDark,
    surface             = AviateSurfaceDark,
    surfaceVariant      = AviateSurfaceVariantDark,
    onSurface           = AviateOnSurfaceDark,
    onSurfaceVariant    = AviateOnSurfaceVariantDark,
    outline             = AviateOutlineDark,
    outlineVariant      = AviateOutlineVariantDark
)

/**
 * Tema dell'app. Material You dinamico su Android 12+, palette di fallback
 * sotto. [dynamicColor] permette di disattivare il colore dinamico se serve.
 */
@Composable
fun AviateCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else      -> LightColors
    }
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(LocalAviateExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = AviateTypography,
            shapes      = AviateShapes,
            content     = content
        )
    }
}

/** Accesso comodo ai colori extra: AviateTheme.extra.weatherTint */
object AviateTheme {
    val extra: AviateExtraColors
        @Composable get() = LocalAviateExtraColors.current
}
