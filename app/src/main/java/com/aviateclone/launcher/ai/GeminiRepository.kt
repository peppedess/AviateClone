package com.aviateclone.launcher.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class AiBriefing(
    val text: String,
    val isAiGenerated: Boolean = true
)

object GeminiRepository {

    private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val MODEL = "gemini-1.5-flash"
    private const val PREFS = "aviate_ai"
    private const val KEY_API = "gemini_api_key"

    // ── Chiave API ──────────────────────────────────────────────────────────
    fun getApiKey(context: Context): String? =
        context.getSharedPreferences(PREFS, 0).getString(KEY_API, null)
            ?.takeIf { it.isNotBlank() }

    fun saveApiKey(context: Context, key: String) =
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY_API, key).apply()

    fun hasApiKey(context: Context) = getApiKey(context) != null

    // ── Briefing mattutino ──────────────────────────────────────────────────
    suspend fun generateMorningBriefing(
        context: Context,
        weatherDesc: String,
        tempC: Int,
        nextEvent: String?,
        missedCalls: Int,
        unreadSms: Int,
        greeting: String
    ): AiBriefing? = withContext(Dispatchers.IO) {
        val key = getApiKey(context) ?: return@withContext null
        val eventLine = if (nextEvent != null) "Hai un evento: $nextEvent." else ""
        val callsLine = if (missedCalls > 0) "$missedCalls chiamate perse." else ""
        val smsLine   = if (unreadSms > 0) "$unreadSms messaggi da leggere." else ""

        val prompt = """
            Sei l'assistente del launcher Aviate. L'ora è $greeting.
            Meteo: $weatherDesc, ${tempC}°C. $eventLine $callsLine $smsLine
            Scrivi UN briefing di massimo 2 frasi, naturale e diretto, in italiano.
            Non usare emoji. Non aggiungere titoli. Solo il testo del briefing.
        """.trimIndent()

        val result = callGemini(key, prompt, maxTokens = 80) ?: return@withContext null
        AiBriefing(result.trim())
    }

    // ── Summarizzazione notifiche ───────────────────────────────────────────
    suspend fun summarizeNotifications(
        context: Context,
        notifications: List<Pair<String, String>> // (app, testo)
    ): String? = withContext(Dispatchers.IO) {
        val key = getApiKey(context) ?: return@withContext null
        if (notifications.isEmpty()) return@withContext null

        val list = notifications.take(5).joinToString("\n") { (app, text) ->
            "• $app: ${text.take(80)}"
        }
        val prompt = """
            Riassumi queste notifiche in UNA frase concisa in italiano.
            Non usare emoji. Solo la frase, niente altro.
            Notifiche:
            $list
        """.trimIndent()

        callGemini(key, prompt, maxTokens = 60)?.trim()
    }

    // ── Classificazione automatica app ─────────────────────────────────────
    suspend fun classifyApp(
        context: Context,
        appName: String,
        packageName: String
    ): String? = withContext(Dispatchers.IO) {
        val key = getApiKey(context) ?: return@withContext null
        val prompt = """
            Classifica questa app Android in UNA categoria tra:
            COMUNICAZIONE, NAVIGAZIONE, MEDIA, PRODUTTIVITÀ, NOTIZIE,
            INTRATTENIMENTO, CIBO, FINANZA, FOTO, SALUTE, ALTRO
            
            App: "$appName" (package: $packageName)
            Rispondi con solo il nome della categoria, niente altro.
        """.trimIndent()

        callGemini(key, prompt, maxTokens = 10)?.trim()?.uppercase()
    }

    // ── Core HTTP ───────────────────────────────────────────────────────────
    private fun callGemini(apiKey: String, prompt: String, maxTokens: Int = 150): String? {
        return try {
            val url = URL("$BASE/$MODEL:generateContent?key=$apiKey")
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 12000

            val body = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", maxTokens)
                    put("topP", 0.9)
                })
                put("safetySettings", JSONArray().apply {
                    // Disable overly aggressive safety filters for launcher context
                    listOf("HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH",
                           "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT")
                        .forEach { cat ->
                            put(JSONObject().apply {
                                put("category", cat)
                                put("threshold", "BLOCK_ONLY_HIGH")
                            })
                        }
                })
            }

            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode != 200) return null

            val resp = JSONObject(conn.inputStream.bufferedReader().readText())
            resp.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } catch (_: Exception) { null }
    }
}
