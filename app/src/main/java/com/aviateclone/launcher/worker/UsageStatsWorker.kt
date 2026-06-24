package com.aviateclone.launcher.worker

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UsageStatsWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    companion object {
        private const val WORK_NAME = "aviate_usage_stats_v2"
        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<UsageStatsWorker>(20, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return Result.success()
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences("aviate_prefs", 0)

        return try {
            val end   = System.currentTimeMillis()
            val start = end - TimeUnit.MINUTES.toMillis(25) // finestra leggermente più larga del periodo
            val events = usm.queryEvents(start, end)
            val event  = UsageEvents.Event()
            val editor = prefs.edit()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
                val pkg = event.packageName ?: continue

                val cal  = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                val dow  = cal.get(Calendar.DAY_OF_WEEK)
                val h    = cal.get(Calendar.HOUR_OF_DAY)
                val half = if (cal.get(Calendar.MINUTE) >= 30) 1 else 0
                val key  = "slot_${pkg}_${dow}_${h}_${half}"

                val cur = prefs.getFloat(key, 0f)
                editor.putFloat(key, cur * 0.92f + 1f)

                val lastKey = "last_$pkg"
                if (event.timeStamp > prefs.getLong(lastKey, 0))
                    editor.putLong(lastKey, event.timeStamp)
            }
            editor.apply()
            Result.success()
        } catch (_: SecurityException) { Result.success() }
          catch (_: Exception) { Result.retry() }
    }
}
