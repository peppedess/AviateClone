package com.aviateclone.launcher.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build

object WallpaperPaletteHelper {

    /** Ritorna true se il wallpaper è scuro (testo bianco ok) */
    fun isDarkWallpaper(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return true
        return try {
            val wm = WallpaperManager.getInstance(context)
            val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return true
            val primary = colors.primaryColor.toArgb()
            luminance(primary) < 0.4f
        } catch (_: Exception) { true }
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.2126f * linearize(r) + 0.7152f * linearize(g) + 0.0722f * linearize(b)
    }

    private fun linearize(c: Float) = if (c <= 0.04045f) c / 12.92f
        else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
}
