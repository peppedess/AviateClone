package com.aviateclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppLoader {

    private val COMMUNICATION_PKGS = setOf(
        "com.whatsapp", "org.telegram.messenger", "com.facebook.orca", "com.discord",
        "com.instagram.android", "com.google.android.apps.messaging", "com.android.mms",
        "com.samsung.android.messaging", "com.skype.raider", "com.viber.voip",
        "com.snapchat.android", "com.twitter.android", "com.linkedin.android",
        "com.google.android.gm", "com.microsoft.teams", "com.slack", "com.zoom.videomeetings"
    )
    private val NAVIGATION_PKGS = setOf(
        "com.google.android.apps.maps", "com.waze", "it.paynter.app",
        "com.ubercab", "it.trenord", "com.trenitalia.app", "com.atm.traspMilano"
    )
    private val MEDIA_PKGS = setOf(
        "com.spotify.music", "com.google.android.music", "com.amazon.mp3",
        "com.soundcloud.android", "tv.twitch.android.app", "com.youtube.music",
        "com.deezer.android.app", "it.mediaset.android.newmediaset",
        "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
        "com.google.android.youtube"
    )
    private val PRODUCTIVITY_PKGS = setOf(
        "com.google.android.apps.docs", "com.microsoft.office.word",
        "com.microsoft.office.excel", "com.microsoft.office.powerpoint",
        "com.google.android.apps.sheets", "com.google.android.apps.slides",
        "com.notion.id", "com.evernote", "com.todoist", "com.ticktick.task",
        "com.microsoft.todos", "com.google.android.calendar",
        "com.google.android.keep", "com.adobe.reader", "com.dropbox.android",
        "com.box.android"
    )
    private val NEWS_PKGS = setOf(
        "com.google.android.apps.magazines", "com.flipboard.app",
        "com.bbc.news", "it.quotidiano.corriere", "it.gelocal.la_stampa",
        "com.reddit.frontpage", "net.feedly.android"
    )
    private val ENTERTAINMENT_PKGS = setOf(
        "com.netflix.mediaclient", "com.amazon.avod.thirdpartyclient",
        "com.disneyplus", "it.raiplay", "com.google.android.youtube",
        "com.twitch.android.app", "com.epicgames.fortnite", "com.roblox.client"
    )
    private val FOOD_PKGS = setOf(
        "it.justeat.app.android", "com.deliveroo.orderapp", "it.glovo",
        "it.ubereats", "com.yelp.android", "it.tripadvisor.tripadvisor"
    )
    private val FINANCE_PKGS = setOf(
        "com.paypal.android.p2pmobile", "com.satispay.android",
        "it.bnl.apps.banking", "com.n26.android", "it.hype",
        "com.revolut.revolut", "com.coinbase.android"
    )
    private val PHOTO_PKGS = setOf(
        "com.google.android.apps.photos", "com.instagram.android",
        "com.snapchat.android", "com.adobe.lrmobile", "com.vsco.cam",
        "com.google.android.GoogleCamera", "com.sec.android.app.camera"
    )
    private val HEALTH_PKGS = setOf(
        "com.google.android.apps.fitness", "com.fitbit.FitbitMobile",
        "com.samsung.android.shealth", "com.nike.plusgps", "com.strava",
        "com.calm.android", "com.headspace.android"
    )

    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        return pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val pkg = info.activityInfo.packageName
                AppInfo(
                    packageName = pkg,
                    appName     = info.loadLabel(pm).toString(),
                    icon        = info.loadIcon(pm),
                    category    = classifyPackage(pkg, pm)
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }
            .toList()
    }

    private fun classifyPackage(pkg: String, pm: PackageManager): AppCategory {
        if (pkg in COMMUNICATION_PKGS)  return AppCategory.COMMUNICATION
        if (pkg in NAVIGATION_PKGS)     return AppCategory.NAVIGATION
        if (pkg in MEDIA_PKGS)          return AppCategory.MEDIA
        if (pkg in PRODUCTIVITY_PKGS)   return AppCategory.PRODUCTIVITY
        if (pkg in NEWS_PKGS)           return AppCategory.NEWS
        if (pkg in ENTERTAINMENT_PKGS)  return AppCategory.ENTERTAINMENT
        if (pkg in FOOD_PKGS)           return AppCategory.FOOD
        if (pkg in FINANCE_PKGS)        return AppCategory.FINANCE
        if (pkg in PHOTO_PKGS)          return AppCategory.PHOTO
        if (pkg in HEALTH_PKGS)         return AppCategory.HEALTH

        // Fallback: leggi category dal manifest dell'app
        return try {
            val appInfo = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            when (appInfo.category) {
                android.content.pm.ApplicationInfo.CATEGORY_SOCIAL       -> AppCategory.COMMUNICATION
                android.content.pm.ApplicationInfo.CATEGORY_MAPS         -> AppCategory.NAVIGATION
                android.content.pm.ApplicationInfo.CATEGORY_AUDIO        -> AppCategory.MEDIA
                android.content.pm.ApplicationInfo.CATEGORY_VIDEO        -> AppCategory.MEDIA
                android.content.pm.ApplicationInfo.CATEGORY_IMAGE        -> AppCategory.PHOTO
                android.content.pm.ApplicationInfo.CATEGORY_GAME         -> AppCategory.ENTERTAINMENT
                android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
                android.content.pm.ApplicationInfo.CATEGORY_NEWS         -> AppCategory.NEWS
                else -> AppCategory.OTHER
            }
        } catch (_: Exception) { AppCategory.OTHER }
    }
}
