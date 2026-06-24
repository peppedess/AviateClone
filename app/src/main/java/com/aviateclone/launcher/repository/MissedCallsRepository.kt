package com.aviateclone.launcher.repository

import android.content.Context
import android.provider.CallLog
import android.provider.Telephony
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MissedCall(val name: String, val number: String, val timeMs: Long, val count: Int)
data class UnreadSms(val sender: String, val snippet: String, val count: Int)

object MissedCallsRepository {

    suspend fun getMissedCalls(context: Context, max: Int = 5): List<MissedCall> =
        withContext(Dispatchers.IO) {
            try {
                val proj = arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE
                )
                val sel = "${CallLog.Calls.TYPE} = ${CallLog.Calls.MISSED_TYPE} " +
                          "AND ${CallLog.Calls.NEW} = 1"
                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI, proj, sel, null,
                    "${CallLog.Calls.DATE} DESC"
                ) ?: return@withContext emptyList()

                // FIX 5: usa grouped.size come limite, non calls.size (che era sempre 0)
                val grouped = mutableMapOf<String, MissedCall>()
                cursor.use {
                    while (it.moveToNext()) {
                        val name   = it.getString(0)?.takeIf { s -> s.isNotBlank() }
                                  ?: it.getString(1) ?: "Sconosciuto"
                        val number = it.getString(1) ?: ""
                        val time   = it.getLong(2)
                        val key    = number.ifBlank { name }
                        val existing = grouped[key]
                        grouped[key] = if (existing == null)
                            MissedCall(name, number, time, 1)
                        else
                            existing.copy(count = existing.count + 1)
                        // Limit: non serve più di max*3 entry uniche
                        if (grouped.size >= max * 3) break
                    }
                }
                grouped.values
                    .sortedByDescending { it.timeMs }
                    .take(max)
            } catch (_: SecurityException) { emptyList() }
              catch (_: Exception) { emptyList() }
        }

    suspend fun getUnreadSms(context: Context, max: Int = 3): List<UnreadSms> =
        withContext(Dispatchers.IO) {
            try {
                val proj = arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY
                )
                val sel = "${Telephony.Sms.READ} = 0 " +
                          "AND ${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}"
                val cursor = context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI, proj, sel, null,
                    "${Telephony.Sms.DATE} DESC"
                ) ?: return@withContext emptyList()

                val grouped = mutableMapOf<String, Pair<String, Int>>()
                cursor.use {
                    while (it.moveToNext()) {
                        val addr = it.getString(0)?.trim() ?: "Sconosciuto"
                        val body = it.getString(1)?.take(80) ?: ""
                        val prev = grouped[addr]
                        // Mantieni il corpo del primo (più recente), incrementa contatore
                        grouped[addr] = (prev?.first ?: body) to ((prev?.second ?: 0) + 1)
                    }
                }
                grouped.entries
                    .take(max)
                    .map { (addr, pair) -> UnreadSms(addr, pair.first, pair.second) }
            } catch (_: SecurityException) { emptyList() }
              catch (_: Exception) { emptyList() }
        }
}
