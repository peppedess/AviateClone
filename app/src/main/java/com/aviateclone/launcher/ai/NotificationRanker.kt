package com.aviateclone.launcher.ai

import android.content.Context
import android.provider.ContactsContract
import com.aviateclone.launcher.service.AviateNotificationService

data class RankedNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val score: Float,
    val reason: String   // "Contatto frequente", "Risposta richiesta", ecc.
)

object NotificationRanker {

    private var contactCache: Set<String>? = null

    /**
     * Ordina le notifiche attive per importanza usando euristiche:
     * contatto in rubrica, domanda richiesta, recency, categoria app.
     */
    fun getRankedNotifications(
        context: Context,
        max: Int = 3
    ): List<RankedNotification> {
        return try {
            val service = AviateNotificationService.lastNotifications
            if (service.isEmpty()) return emptyList()

            val contacts = getContactNames(context)
            service.take(10).map { (pkg, title, text) ->
                var score = 0f
                var reason = ""

                // +3: mittente nella rubrica
                val inContacts = contacts.any {
                    title.contains(it, ignoreCase = true) ||
                    text.contains(it, ignoreCase = true)
                }
                if (inContacts) { score += 3f; reason = "Contatto" }

                // +2: domanda nel testo
                if (text.contains("?")) { score += 2f; reason += " · Risposta richiesta" }

                // +1.5: comunicazione diretta (WhatsApp, Telegram, SMS)
                val commApps = listOf("whatsapp", "telegram", "message", "sms", "signal")
                if (commApps.any { pkg.contains(it) }) { score += 1.5f }

                // +1: email
                if (pkg.contains("gmail") || pkg.contains("mail")) score += 1f

                // -1: app di sistema / pubblicità
                if (pkg.contains("android") || pkg.contains("google.android.gms")) score -= 1f

                // Punteggio base per badge count
                val badge = AviateNotificationService.badgeCounts[pkg] ?: 0
                score += badge * 0.1f

                RankedNotification(pkg, title, text, score,
                    reason.trimStart(' ', '·').trim())
            }
                .filter { it.score > 0 }
                .sortedByDescending { it.score }
                .take(max)
        } catch (_: Exception) { emptyList() }
    }

    private fun getContactNames(context: Context): Set<String> {
        contactCache?.let { return it }
        return try {
            val names = mutableSetOf<String>()
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
                null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    it.getString(0)?.takeIf { n -> n.length > 1 }?.let { n -> names.add(n) }
                }
            }
            contactCache = names
            names
        } catch (_: Exception) { emptySet() }
    }
}
