package com.aviateclone.launcher.engine

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import java.util.Calendar

enum class TimeContext(val label: String, val emoji: String) {
    MORNING("Buongiorno",   "☀️"),
    COMMUTE("In viaggio",   "🚌"),
    WORK   ("Lavoro",       "💼"),
    LUNCH  ("Pausa pranzo", "🍽️"),
    EVENING("Buonasera",    "🌆"),
    NIGHT  ("Buonanotte",   "🌙")
}

class ContextEngine(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aviate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OVERRIDE = "context_override"
        private const val OVERRIDE_NONE = "AUTO"
    }

    // ── Contesto manuale o automatico ──────────────────────────────────────
    fun getCurrentContext(): TimeContext {
        val override = prefs.getString(KEY_OVERRIDE, OVERRIDE_NONE) ?: OVERRIDE_NONE
        if (override != OVERRIDE_NONE) {
            return try { TimeContext.valueOf(override) } catch (_: Exception) { getAutoContext() }
        }
        return getAutoContext()
    }

    /** Imposta override manuale (null = torna ad automatico) */
    fun setContextOverride(ctx: TimeContext?) {
        prefs.edit().putString(KEY_OVERRIDE, ctx?.name ?: OVERRIDE_NONE).apply()
    }

    fun isManualOverride(): Boolean =
        prefs.getString(KEY_OVERRIDE, OVERRIDE_NONE) != OVERRIDE_NONE

    private fun getAutoContext(): TimeContext {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..8   -> TimeContext.MORNING
            in 9..9   -> TimeContext.COMMUTE
            in 10..12 -> TimeContext.WORK
            in 13..14 -> TimeContext.LUNCH
            in 15..19 -> TimeContext.WORK
            in 20..22 -> TimeContext.EVENING
            else      -> TimeContext.NIGHT
        }
    }

    // ── App recenti via UsageStats (Android 5+) ───────────────────────────
    fun getRecentApps(allApps: List<AppInfo>, maxCount: Int = 8): List<AppInfo> {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
                return getFallbackRecent(allApps, maxCount)
            }
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return getFallbackRecent(allApps, maxCount)

            val end = System.currentTimeMillis()
            val start = end - 7L * 24 * 60 * 60 * 1000 // ultima settimana

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, start, end)
            if (stats.isNullOrEmpty()) return getFallbackRecent(allApps, maxCount)

            // Mappa packageName → lastTimeUsed
            val usageMap = stats
                .filter { it.totalTimeInForeground > 0 }
                .groupBy { it.packageName }
                .mapValues { (_, list) -> list.maxOf { it.lastTimeUsed } }

            // Ordina le app per lastTimeUsed decrescente
            allApps
                .filter { usageMap.containsKey(it.packageName) }
                .sortedByDescending { usageMap[it.packageName] ?: 0L }
                .take(maxCount)
                .ifEmpty { getFallbackRecent(allApps, maxCount) }

        } catch (_: SecurityException) {
            getFallbackRecent(allApps, maxCount)
        } catch (_: Exception) {
            getFallbackRecent(allApps, maxCount)
        }
    }

    /** Fallback se UsageStats non è disponibile: usa contatori interni */
    private fun getFallbackRecent(allApps: List<AppInfo>, maxCount: Int): List<AppInfo> =
        allApps
            .map { it.copy(launchCount = prefs.getInt("launch_${it.packageName}", 0),
                           lastUsed = prefs.getLong("last_${it.packageName}", 0L)) }
            .filter { it.lastUsed > 0 }
            .sortedByDescending { it.lastUsed }
            .take(maxCount)
            .ifEmpty { allApps.take(maxCount) }

    // ── App suggerite per contesto ─────────────────────────────────────────
    fun getSuggestedApps(allApps: List<AppInfo>, maxCount: Int = 8): List<AppInfo> {
        if (allApps.isEmpty()) return emptyList()
        val ctx = getCurrentContext()
        val priorityCategories = getCategoriesForContext(ctx)

        val recent = getRecentApps(allApps, maxCount / 2)
        val remainingSlots = maxCount - recent.size

        val contextApps = allApps
            .filter { it.category in priorityCategories }
            .filter { app -> recent.none { it.packageName == app.packageName } }
            .sortedByDescending { it.launchCount }
            .take(remainingSlots)

        val combined = (recent + contextApps).distinctBy { it.packageName }.take(maxCount)

        return if (combined.size < maxCount) {
            val extra = allApps
                .filter { app -> combined.none { it.packageName == app.packageName } }
                .take(maxCount - combined.size)
            combined + extra
        } else combined
    }

    private fun getCategoriesForContext(ctx: TimeContext) = when (ctx) {
        TimeContext.MORNING  -> listOf(AppCategory.NEWS, AppCategory.COMMUNICATION, AppCategory.PRODUCTIVITY, AppCategory.HEALTH)
        TimeContext.COMMUTE  -> listOf(AppCategory.NAVIGATION, AppCategory.MEDIA, AppCategory.COMMUNICATION, AppCategory.NEWS)
        TimeContext.WORK     -> listOf(AppCategory.PRODUCTIVITY, AppCategory.COMMUNICATION, AppCategory.FINANCE)
        TimeContext.LUNCH    -> listOf(AppCategory.FOOD, AppCategory.COMMUNICATION, AppCategory.NEWS, AppCategory.MEDIA)
        TimeContext.EVENING  -> listOf(AppCategory.ENTERTAINMENT, AppCategory.COMMUNICATION, AppCategory.MEDIA, AppCategory.FOOD)
        TimeContext.NIGHT    -> listOf(AppCategory.ENTERTAINMENT, AppCategory.MEDIA, AppCategory.COMMUNICATION)
    }

    // ── Contatori interni ──────────────────────────────────────────────────
    fun recordLaunch(packageName: String) {
        prefs.edit()
            .putInt("launch_$packageName", prefs.getInt("launch_$packageName", 0) + 1)
            .putLong("last_$packageName", System.currentTimeMillis())
            .apply()
    }

    fun getFrequentApps(allApps: List<AppInfo>, count: Int): List<AppInfo> =
        allApps.map { it.copy(launchCount = prefs.getInt("launch_${it.packageName}", 0),
                              lastUsed = prefs.getLong("last_${it.packageName}", 0L)) }
            .filter { it.launchCount > 0 }
            .sortedByDescending { it.launchCount }
            .take(count)

    fun enrichWithUsageData(apps: List<AppInfo>) =
        apps.map { it.copy(launchCount = prefs.getInt("launch_${it.packageName}", 0),
                           lastUsed = prefs.getLong("last_${it.packageName}", 0L)) }
}
