package com.aviateclone.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

object AppLoader {
    fun loadApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val activities: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .map { info -> AppInfo(
                packageName = info.activityInfo.packageName,
                appName = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm),
                category = guessCategory(info.activityInfo.packageName)
            )}
            .filter { it.packageName != context.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun guessCategory(packageName: String): AppCategory {
        val pkg = packageName.lowercase()
        return when {
            pkg.containsAny("whatsapp","telegram","messenger","sms","message","signal","viber","skype","meet","teams","zoom","discord") -> AppCategory.COMMUNICATION
            pkg.containsAny("maps","navigation","waze","gps","transit","citymapper","moovit","here.") -> AppCategory.NAVIGATION
            pkg.containsAny("spotify","music","podcast","youtube","soundcloud","deezer","tidal","audible","radio","castbox") -> AppCategory.MEDIA
            pkg.containsAny("calendar","notion","evernote","todoist","tasks","slack","office","docs","sheets","gmail","outlook","mail","drive","dropbox","trello") -> AppCategory.PRODUCTIVITY
            pkg.containsAny("news","weather","rss","feedly","flipboard","meteo","ilmeteo","corriere","repubblica") -> AppCategory.NEWS
            pkg.containsAny("netflix","prime","disney","hulu","twitch","game","play.games","raiplay","dazn") -> AppCategory.ENTERTAINMENT
            pkg.containsAny("food","glovo","deliveroo","justeat","ubereats","tripadvisor","yelp","zomato","thefork") -> AppCategory.FOOD
            pkg.containsAny("bank","paypal","revolut","n26","satispay","fineco","intesa","unicredit","bnl","poste") -> AppCategory.FINANCE
            pkg.containsAny("camera","photo","gallery","gcam","instagram","snapseed","lightroom","vsco") -> AppCategory.PHOTO
            pkg.containsAny("strava","fitbit","health","fitness","garmin","runkeeper","myfitnesspal","training","sport") -> AppCategory.HEALTH
            else -> AppCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
}
