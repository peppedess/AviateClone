package com.aviateclone.launcher.ai

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class AppPattern(
    val packageName: String,
    val triggerDescription: String,  // "Ogni lunedì alle 9:00"
    val confidence: Float            // 0.0 – 1.0
)

object PatternLearner {

    private const val PREFS = "aviate_patterns"
    private const val WEEKS = 4
    private const val MIN_OCCURRENCES = 3      // soglia minima per considerare un pattern

    /**
     * Analizza le ultime 4 settimane di UsageEvents e ritorna
     * i pattern più forti per l'ora corrente.
     */
    suspend fun getPatternsForNow(context: Context, max: Int = 3): List<AppPattern> =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
                return@withContext emptyList()

            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as? UsageStatsManager ?: return@withContext emptyList()

                val now = System.currentTimeMillis()
                val start = now - TimeUnit.DAYS.toMillis(WEEKS * 7L)
                val events = usm.queryEvents(start, now)
                val event = UsageEvents.Event()

                // Mappa: pkg → mappa(slotKey → conteggio)
                // slotKey = "dow_slot" dove dow=giorno settimana, slot=30min bucket
                val counts = mutableMapOf<String, MutableMap<String, Int>>()

                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                    val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                    val dow  = cal.get(Calendar.DAY_OF_WEEK)   // 1=Dom
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val half = if (cal.get(Calendar.MINUTE) >= 30) 1 else 0
                    val key = "${dow}_${hour}_${half}"
                    counts.getOrPut(event.packageName) { mutableMapOf() }
                        .merge(key, 1, Int::plus)
                }

                // Slot corrente
                val nowCal = Calendar.getInstance()
                val nowDow  = nowCal.get(Calendar.DAY_OF_WEEK)
                val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
                val nowHalf = if (nowCal.get(Calendar.MINUTE) >= 30) 1 else 0
                // Guarda ±1 slot per tolleranza
                val targetSlots = setOf(
                    "${nowDow}_${nowHour}_${nowHalf}",
                    "${nowDow}_${(nowHour - 1).coerceAtLeast(0)}_${if (nowHalf==0) 1 else 0}",
                    "${nowDow}_${(nowHour + 1).coerceAtMost(23)}_${if (nowHalf==1) 0 else 1}"
                )

                val patterns = mutableListOf<AppPattern>()
                for ((pkg, slotMap) in counts) {
                    val matchCount = targetSlots.sumOf { slotMap[it] ?: 0 }
                    if (matchCount < MIN_OCCURRENCES) continue

                    val confidence = (matchCount.toFloat() / (WEEKS * 2f)).coerceAtMost(1f)
                    val desc = buildDescription(nowDow, nowHour, nowHalf)
                    patterns.add(AppPattern(pkg, desc, confidence))
                }

                patterns.sortedByDescending { it.confidence }.take(max)
            } catch (_: SecurityException) { emptyList() }
              catch (_: Exception) { emptyList() }
        }

    private fun buildDescription(dow: Int, hour: Int, half: Int): String {
        val days = listOf("", "domenica", "lunedì", "martedì", "mercoledì",
                          "giovedì", "venerdì", "sabato")
        val m = if (half == 1) 30 else 0
        val time = "%02d:%02d".format(hour, m)
        return "Ogni ${days.getOrElse(dow) { "giorno" }} alle $time"
    }

    /** Salva il fatto che l'utente ha IGNORATO un suggerimento (feedback negativo) */
    fun recordDismiss(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREFS, 0)
        val key = "dismiss_$packageName"
        prefs.edit().putInt(key, (prefs.getInt(key, 0) + 1)).apply()
    }

    fun getDismissCount(context: Context, packageName: String) =
        context.getSharedPreferences(PREFS, 0).getInt("dismiss_$packageName", 0)
}
