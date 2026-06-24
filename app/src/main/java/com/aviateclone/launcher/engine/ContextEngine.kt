package com.aviateclone.launcher.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.repository.PlaceType
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class TimeContext(val label: String, val emoji: String) {
    MORNING("Buongiorno",   "☀️"),
    COMMUTE("In viaggio",   "🚌"),
    WORK   ("Lavoro",       "💼"),
    LUNCH  ("Pausa pranzo", "🍽️"),
    AFTERNOON("Pomeriggio", "🌤️"),
    EVENING("Buonasera",    "🌆"),
    NIGHT  ("Buonanotte",   "🌙")
}

/**
 * Fascia oraria granulare — 30 minuti di risoluzione.
 * Key: "dow_hour_half" dove half=0 (ore :00) o 1 (ore :30)
 * Esempio: lunedì alle 14:30 → "2_14_1"
 */
private fun slotKey(pkg: String, cal: Calendar): String {
    val dow  = cal.get(Calendar.DAY_OF_WEEK)
    val h    = cal.get(Calendar.HOUR_OF_DAY)
    val half = if (cal.get(Calendar.MINUTE) >= 30) 1 else 0
    return "slot_${pkg}_${dow}_${h}_${half}"
}

class ContextEngine(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aviate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OVERRIDE       = "context_override"
        private const val OVERRIDE_NONE      = "AUTO"
        private const val KEY_BOOTSTRAPPED   = "ml_bootstrapped_v2"
        private const val BOOTSTRAP_DAYS     = 30L
        private const val MIN_SLOT_SCORE     = 0.5f
    }

    // ── Contesto corrente ──────────────────────────────────────────────────
    fun getCurrentContext(place: PlaceType = PlaceType.OTHER): TimeContext {
        val override = prefs.getString(KEY_OVERRIDE, OVERRIDE_NONE) ?: OVERRIDE_NONE
        if (override != OVERRIDE_NONE)
            return try { TimeContext.valueOf(override) }
                   catch (_: Exception) { autoContext(place) }
        return autoContext(place)
    }

    fun setContextOverride(ctx: TimeContext?) =
        prefs.edit().putString(KEY_OVERRIDE, ctx?.name ?: OVERRIDE_NONE).apply()

    fun isManualOverride() = prefs.getString(KEY_OVERRIDE, OVERRIDE_NONE) != OVERRIDE_NONE

    private fun autoContext(place: PlaceType): TimeContext {
        val cal = Calendar.getInstance()
        val h   = cal.get(Calendar.HOUR_OF_DAY)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY

        return when {
            isWeekend -> when (h) {
                in 5..9   -> TimeContext.MORNING
                in 10..12 -> TimeContext.MORNING
                in 13..14 -> TimeContext.LUNCH
                in 15..19 -> TimeContext.AFTERNOON
                in 20..22 -> TimeContext.EVENING
                else      -> TimeContext.NIGHT
            }
            place == PlaceType.WORK -> when (h) {
                in 12..13 -> TimeContext.LUNCH
                in 14..17 -> TimeContext.AFTERNOON
                else      -> TimeContext.WORK
            }
            place == PlaceType.HOME -> when (h) {
                in 5..8   -> TimeContext.MORNING
                in 9..11  -> TimeContext.COMMUTE
                in 12..13 -> TimeContext.LUNCH
                in 14..18 -> TimeContext.AFTERNOON
                in 19..22 -> TimeContext.EVENING
                else      -> TimeContext.NIGHT
            }
            else -> when (h) {
                in 5..8   -> TimeContext.MORNING
                9         -> TimeContext.COMMUTE
                in 10..11 -> TimeContext.WORK
                in 12..13 -> TimeContext.LUNCH
                in 14..17 -> TimeContext.AFTERNOON
                in 18..19 -> TimeContext.WORK
                in 20..22 -> TimeContext.EVENING
                else      -> TimeContext.NIGHT
            }
        }
    }

    // ── ML Scoring ─────────────────────────────────────────────────────────
    fun getSuggestedApps(
        apps: List<AppInfo>,
        maxCount: Int = 8,
        place: PlaceType = PlaceType.OTHER
    ): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()

        // Bootstrap automatico al primo avvio (popola slot da UsageStats reale)
        if (!prefs.getBoolean(KEY_BOOTSTRAPPED, false)) bootstrapFromUsageStats(apps)

        val ctx  = getCurrentContext(place)
        val cal  = Calendar.getInstance()
        val h    = cal.get(Calendar.HOUR_OF_DAY)
        val half = if (cal.get(Calendar.MINUTE) >= 30) 1 else 0
        val dow  = cal.get(Calendar.DAY_OF_WEEK)
        val catPriority = catForContext(ctx)

        return apps.map { app ->
            val score = computeScore(app, ctx, catPriority, dow, h, half, place)
            app to score
        }
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
    }

    private fun computeScore(
        app: AppInfo,
        ctx: TimeContext,
        catPriority: List<AppCategory>,
        dow: Int,
        h: Int,
        half: Int,
        place: PlaceType
    ): Double {
        // 1. Categoria per contesto (forte segnale)
        val catScore = when (catPriority.indexOf(app.category)) {
            0    -> 4.0
            1    -> 2.5
            2    -> 1.5
            3    -> 0.8
            else -> if (app.category in catPriority) 0.5 else 0.0
        }

        // 2. Contatore lanci (uso storico)
        val usageScore = app.launchCount.coerceAtMost(200).toDouble() * 0.03

        // 3. Recency (ultima apertura)
        val recencyScore = if (app.lastUsed > 0) {
            val hoursAgo = (System.currentTimeMillis() - app.lastUsed) / 3_600_000.0
            (6.0 / (1.0 + hoursAgo * 0.5)).coerceAtMost(4.0)
        } else 0.0

        // 4. Slot orario esatto (stessa ora, stesso giorno — il più forte segnale)
        val exactKey = "slot_${app.packageName}_${dow}_${h}_${half}"
        val exactSlot = prefs.getFloat(exactKey, 0f).toDouble()

        // 5. Slot orario vicino (±1 slot = ±30 min) per tolleranza
        val prevHalf = if (half == 0) 1 else 0
        val prevH    = if (half == 0) (h - 1).coerceAtLeast(0) else h
        val nextHalf = if (half == 1) 0 else 1
        val nextH    = if (half == 1) (h + 1).coerceAtMost(23) else h
        val nearSlot = maxOf(
            prefs.getFloat("slot_${app.packageName}_${dow}_${prevH}_${prevHalf}", 0f),
            prefs.getFloat("slot_${app.packageName}_${dow}_${nextH}_${nextHalf}", 0f)
        ).toDouble() * 0.6 // peso minore rispetto all'esatto

        // 6. Stesso slot altri giorni della settimana (pattern settimanale)
        val weeklySlot = (1..7).filter { it != dow }.map { otherDow ->
            prefs.getFloat("slot_${app.packageName}_${otherDow}_${h}_${half}", 0f)
        }.average() * 0.4

        // 7. Place score
        val placeScore = when (place) {
            PlaceType.HOME  -> prefs.getFloat("place_home_${app.packageName}", 0f).toDouble() * 1.5
            PlaceType.WORK  -> prefs.getFloat("place_work_${app.packageName}", 0f).toDouble() * 1.5
            PlaceType.OTHER -> 0.0
        }

        return catScore + usageScore + recencyScore +
               (exactSlot * 3.0) + nearSlot + weeklySlot + placeScore
    }

    // ── Bootstrap: popola slot da UsageEvents reali al primo avvio ─────────
    fun bootstrapFromUsageStats(apps: List<AppInfo> = emptyList()) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            prefs.edit().putBoolean(KEY_BOOTSTRAPPED, true).apply()
            return
        }
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? UsageStatsManager ?: return
            val end   = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(BOOTSTRAP_DAYS)
            val events = usm.queryEvents(start, end)
            val event  = UsageEvents.Event()
            val editor = prefs.edit()
            var count  = 0

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                val pkg = event.packageName ?: continue
                val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                val key = slotKey(pkg, cal)
                val cur = prefs.getFloat(key, 0f)
                // EMA più pesante per il bootstrap (dati storici affidabili)
                editor.putFloat(key, cur * 0.90f + 1f)
                // Aggiorna anche launchCount e lastUsed
                val lc = prefs.getInt("launch_$pkg", 0)
                editor.putInt("launch_$pkg", lc + 1)
                val lu = prefs.getLong("last_$pkg", 0L)
                if (event.timeStamp > lu) editor.putLong("last_$pkg", event.timeStamp)
                count++
                if (count % 500 == 0) editor.apply() // flush periodico
            }
            editor.putBoolean(KEY_BOOTSTRAPPED, true).apply()
        } catch (_: SecurityException) {
            prefs.edit().putBoolean(KEY_BOOTSTRAPPED, true).apply()
        } catch (_: Exception) {
            prefs.edit().putBoolean(KEY_BOOTSTRAPPED, true).apply()
        }
    }

    // ── Registra apertura con slot granulare ───────────────────────────────
    fun recordLaunch(packageName: String, place: PlaceType = PlaceType.OTHER) {
        val cal = Calendar.getInstance()
        val key = slotKey(packageName, cal)

        prefs.edit().apply {
            putInt("launch_$packageName", prefs.getInt("launch_$packageName", 0) + 1)
            putLong("last_$packageName", System.currentTimeMillis())
            // EMA slot esatto
            val cur = prefs.getFloat(key, 0f)
            putFloat(key, cur * 0.88f + 1f)
            // Place
            when (place) {
                PlaceType.HOME -> {
                    val v = prefs.getFloat("place_home_$packageName", 0f)
                    putFloat("place_home_$packageName", v * 0.88f + 1f)
                }
                PlaceType.WORK -> {
                    val v = prefs.getFloat("place_work_$packageName", 0f)
                    putFloat("place_work_$packageName", v * 0.88f + 1f)
                }
                else -> {}
            }
        }.apply()
    }

    // ── Merge con UsageStats (solo lastUsed, launchCount è il nostro) ──────
    fun enrichWithUsageData(apps: List<AppInfo>): List<AppInfo> {
        val enriched = apps.map {
            it.copy(
                launchCount = prefs.getInt("launch_${it.packageName}", 0),
                lastUsed    = prefs.getLong("last_${it.packageName}", 0L)
            )
        }
        // Bootstrap se non ancora fatto
        if (!prefs.getBoolean(KEY_BOOTSTRAPPED, false)) bootstrapFromUsageStats(enriched)
        return mergeLastUsed(enriched)
    }

    private fun mergeLastUsed(apps: List<AppInfo>): List<AppInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return apps
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as? UsageStatsManager ?: return apps
            val end   = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(30)
            val stats = usm.queryAndAggregateUsageStats(start, end)
            apps.map { app ->
                val s = stats[app.packageName]
                if (s != null && s.lastTimeUsed > app.lastUsed)
                    app.copy(lastUsed = s.lastTimeUsed)
                else app
            }
        } catch (_: SecurityException) { apps }
          catch (_: Exception) { apps }
    }

    private fun catForContext(ctx: TimeContext) = when (ctx) {
        TimeContext.MORNING   -> listOf(AppCategory.NEWS, AppCategory.COMMUNICATION, AppCategory.HEALTH)
        TimeContext.COMMUTE   -> listOf(AppCategory.NAVIGATION, AppCategory.MEDIA, AppCategory.NEWS, AppCategory.COMMUNICATION)
        TimeContext.WORK      -> listOf(AppCategory.PRODUCTIVITY, AppCategory.COMMUNICATION, AppCategory.FINANCE)
        TimeContext.LUNCH     -> listOf(AppCategory.FOOD, AppCategory.COMMUNICATION, AppCategory.NEWS, AppCategory.MEDIA)
        TimeContext.AFTERNOON -> listOf(AppCategory.PRODUCTIVITY, AppCategory.COMMUNICATION, AppCategory.ENTERTAINMENT, AppCategory.MEDIA)
        TimeContext.EVENING   -> listOf(AppCategory.ENTERTAINMENT, AppCategory.COMMUNICATION, AppCategory.MEDIA, AppCategory.FOOD)
        TimeContext.NIGHT     -> listOf(AppCategory.ENTERTAINMENT, AppCategory.MEDIA)
    }

    fun getFrequentApps(apps: List<AppInfo>, count: Int) =
        apps.sortedByDescending { it.launchCount }.take(count)
}
