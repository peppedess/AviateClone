package com.aviateclone.launcher.repository

import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class CalendarEvent(
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val allDay: Boolean = false
) {
    val timeLabel: String get() {
        if (allDay) return "Tutto il giorno"
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMs }
        return "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }
    val isNow: Boolean get() {
        val now = System.currentTimeMillis()
        return now in startMs..endMs
    }
    val isSoon: Boolean get() {
        val diff = startMs - System.currentTimeMillis()
        return diff in 0..TimeUnit.HOURS.toMillis(2)
    }
}

object CalendarRepository {
    suspend fun getUpcomingEvents(context: Context, maxCount: Int = 3): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val end = now + TimeUnit.HOURS.toMillis(24)
                val proj = arrayOf(
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY
                )
                val sel = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
                val cursor = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI, proj, sel,
                    arrayOf(now.toString(), end.toString()),
                    "${CalendarContract.Events.DTSTART} ASC"
                ) ?: return@withContext emptyList()

                val events = mutableListOf<CalendarEvent>()
                cursor.use {
                    while (it.moveToNext() && events.size < maxCount) {
                        val title = it.getString(0) ?: continue
                        val start = it.getLong(1)
                        val end2  = it.getLong(2)
                        val allDay = it.getInt(3) == 1
                        events.add(CalendarEvent(title, start, end2, allDay))
                    }
                }
                events
            } catch (_: SecurityException) { emptyList() }
              catch (_: Exception) { emptyList() }
        }
}
