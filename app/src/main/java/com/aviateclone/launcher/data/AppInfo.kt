package com.aviateclone.launcher.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val category: AppCategory = AppCategory.OTHER,
    var launchCount: Int = 0,
    var lastUsed: Long = 0L
) {
    val firstLetter: String get() = appName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
}

enum class AppCategory(val displayName: String, val emoji: String) {
    COMMUNICATION("Comunicazione", "💬"),
    NAVIGATION("Navigazione", "🗺️"),
    MEDIA("Musica & Video", "🎵"),
    PRODUCTIVITY("Produttività", "💼"),
    NEWS("Notizie", "📰"),
    ENTERTAINMENT("Intrattenimento", "🎮"),
    FOOD("Cibo & Ristoranti", "🍕"),
    FINANCE("Finanza", "💳"),
    PHOTO("Foto & Camera", "📷"),
    HEALTH("Salute & Sport", "🏃"),
    OTHER("Altre app", "📱")
}
