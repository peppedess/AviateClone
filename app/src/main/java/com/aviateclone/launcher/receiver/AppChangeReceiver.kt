package com.aviateclone.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppChangeReceiver(private val onAppsChanged: (() -> Unit)? = null) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REMOVED, Intent.ACTION_PACKAGE_CHANGED -> onAppsChanged?.invoke()
        }
    }
}
