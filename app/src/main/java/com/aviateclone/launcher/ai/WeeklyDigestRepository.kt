package com.aviateclone.launcher.ai

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class WeeklyDigest(
    val topApp: String,
    val topCategory: String,
    val totalHours: Float,
    val unusedApps: Int,
    val summary: String       // testo leggibile
)

object WeeklyDigestRepository {

    suspend fun getWeeklyDigest(
        context: Context,
        appNameMap: Map<String, String>  // packageName → appName
    ): WeeklyDigest? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return@withContext null
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? UsageStatsManager ?: return@withContext null

            val end   = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(7)
            val stats = usm.queryAndAggregateUsageStats(start, end)
                .values.filter { it.totalTimeInForeground > 60_000 } // >1 min

            if (stats.isEmpty()) return@withContext null

            val topStat = stats.maxByOrNull { it.totalTimeInForeground } ?: return@withContext null
            val topAppName = appNameMap[topStat.packageName] ?: topStat.packageName

            val totalMs = stats.sumOf { it.totalTimeInForeground }
            val totalH  = totalMs / 3_600_000f

            // App installate ma mai aperte questa settimana
            val unusedCount = (appNameMap.size - stats.size).coerceAtLeast(0)

            val topH = topStat.totalTimeInForeground / 3_600_000f
            val summary = buildString {
                append("Questa settimana hai usato il telefono ")
                append("%.1f".format(totalH))
                append(" ore. L'app più usata è $topAppName")
                if (topH > 0.5f) append(" (%.1f h)".format(topH))
                append(".")
                if (unusedCount > 3) append(" Hai $unusedCount app che non apri da 7+ giorni.")
            }

            WeeklyDigest(topAppName, "", totalH, unusedCount, summary)
        } catch (_: SecurityException) { null }
          catch (_: Exception) { null }
    }

    /** True se oggi è lunedì mattina (momento ideale per il digest) */
    fun shouldShowDigest(context: Context): Boolean {
        val cal = java.util.Calendar.getInstance()
        val isMonday = cal.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.MONDAY
        val isMorning = cal.get(java.util.Calendar.HOUR_OF_DAY) in 6..11
        val prefs = context.getSharedPreferences("aviate_digest", 0)
        val lastShownWeek = prefs.getInt("last_week", -1)
        val currentWeek = cal.get(java.util.Calendar.WEEK_OF_YEAR)
        return isMonday && isMorning && lastShownWeek != currentWeek
    }

    fun markDigestShown(context: Context) {
        val week = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)
        context.getSharedPreferences("aviate_digest", 0).edit()
            .putInt("last_week", week).apply()
    }
}
