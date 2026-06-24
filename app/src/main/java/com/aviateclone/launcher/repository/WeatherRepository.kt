package com.aviateclone.launcher.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class WeatherData(
    val tempC: Int,
    val description: String,
    val emoji: String,
    val feelsLike: Int = tempC,
    val isRealData: Boolean = true
)

object WeatherRepository {
    private const val BASE = "https://api.open-meteo.com/v1/forecast"
    private const val CACHE_KEY = "weather_cache"
    private const val CACHE_TS  = "weather_ts"
    private const val CACHE_TTL = 30 * 60 * 1000L

    suspend fun get(context: Context, lat: Double, lon: Double): WeatherData? =
        withContext(Dispatchers.IO) {
            val p = prefs(context)
            val now = System.currentTimeMillis()
            // FIX 6: valida cache prima di usarla
            val cached = p.getString(CACHE_KEY, null)
            if (cached != null && now - p.getLong(CACHE_TS, 0) < CACHE_TTL) {
                val parsed = safeParseWithCleanup(p, cached)
                if (parsed != null) return@withContext parsed
                // cache corrotta → continua con fetch
            }
            try {
                val url = "$BASE?latitude=$lat&longitude=$lon" +
                    "&current_weather=true&hourly=apparent_temperature" +
                    "&forecast_days=1&timezone=auto"
                val json = URL(url).readText()
                val parsed = safeParseWithCleanup(p, json)
                if (parsed != null) {
                    p.edit().putString(CACHE_KEY, json).putLong(CACHE_TS, now).apply()
                }
                parsed
            } catch (_: Exception) { null }
        }

    private fun safeParseWithCleanup(prefs: SharedPreferences, json: String): WeatherData? {
        return try {
            parse(JSONObject(json))
        } catch (_: Exception) {
            // FIX 6: pulisci cache corrotta
            prefs.edit().remove(CACHE_KEY).remove(CACHE_TS).apply()
            null
        }
    }

    private fun parse(json: JSONObject): WeatherData {
        val cw = json.getJSONObject("current_weather")
        val temp = cw.getDouble("temperature").toInt()
        val code = cw.getInt("weathercode")
        val (desc, emoji) = wmoDescription(code)
        val feelsLike = try {
            json.getJSONObject("hourly").getJSONArray("apparent_temperature").getDouble(0).toInt()
        } catch (_: Exception) { temp }
        return WeatherData(temp, desc, emoji, feelsLike, isRealData = true)
    }

    private fun wmoDescription(code: Int): Pair<String, String> = when (code) {
        0         -> "Sereno"                   to "☀️"
        1         -> "Prevalentemente sereno"   to "🌤️"
        2         -> "Parzialmente nuvoloso"    to "⛅"
        3         -> "Nuvoloso"                 to "☁️"
        45, 48    -> "Nebbia"                   to "🌫️"
        51, 53, 55 -> "Pioggerella"             to "🌦️"
        61, 63, 65 -> "Pioggia"                 to "🌧️"
        71, 73, 75 -> "Neve"                    to "❄️"
        77        -> "Granelli di neve"         to "🌨️"
        80, 81, 82 -> "Rovesci"                 to "🌧️"
        85, 86    -> "Nevicate"                 to "❄️"
        95        -> "Temporale"                to "⛈️"
        96, 99    -> "Temporale con grandine"   to "⛈️"
        else      -> "Variabile"                to "🌡️"
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("aviate_weather", 0)
}
