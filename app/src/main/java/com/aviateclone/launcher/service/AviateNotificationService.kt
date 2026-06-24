package com.aviateclone.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

class AviateNotificationService : NotificationListenerService() {

    data class NotifSnapshot(val pkg: String, val title: String, val text: String)

    companion object {
        val badgeCounts = ConcurrentHashMap<String, Int>()
        // Ultime notifiche per NotificationRanker (max 20, thread-safe)
        val lastNotifications = java.util.concurrent.CopyOnWriteArrayList<NotifSnapshot>()
        @Volatile var listener: (() -> Unit)? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.isOngoing || sbn.notification == null) return
        val pkg = sbn.packageName ?: return
        badgeCounts[pkg] = (badgeCounts[pkg] ?: 0) + 1
        // Cattura title+text per NotificationRanker
        val extras = sbn.notification?.extras
        val title  = extras?.getCharSequence("android.title")?.toString() ?: ""
        val text   = extras?.getCharSequence("android.text")?.toString() ?: ""
        if (title.isNotBlank()) {
            if (lastNotifications.size >= 20) lastNotifications.removeAt(0)
            lastNotifications.add(NotifSnapshot(pkg, title, text))
        }
        listener?.invoke()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val active = try { activeNotifications?.count { it.packageName == pkg } ?: 0 }
                     catch (_: Exception) { 0 }
        if (active == 0) badgeCounts.remove(pkg) else badgeCounts[pkg] = active
        listener?.invoke()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        badgeCounts.clear()
        try {
            activeNotifications?.forEach { sbn ->
                if (!sbn.isOngoing) {
                    val pkg = sbn.packageName ?: return@forEach
                    badgeCounts[pkg] = (badgeCounts[pkg] ?: 0) + 1
                }
            }
        } catch (_: Exception) {}
        listener?.invoke()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        badgeCounts.clear()
        listener?.invoke()
    }
}
