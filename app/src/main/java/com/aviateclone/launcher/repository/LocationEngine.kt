package com.aviateclone.launcher.repository

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LatLon(val lat: Double, val lon: Double)
enum class PlaceType { HOME, WORK, OTHER }

class LocationEngine(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("aviate_location", 0)
    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    companion object {
        private const val HOME_LAT = "home_lat"; private const val HOME_LON = "home_lon"
        private const val WORK_LAT = "work_lat"; private const val WORK_LON = "work_lon"
        // FIX 5: usare un valore fuori range GPS (±90/±180) come sentinel
        private const val NO_COORD = 999f
        private const val RADIUS_M = 300.0
    }

    suspend fun getLastKnown(): LatLon? = try {
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc -> cont.resume(loc?.let { LatLon(it.latitude, it.longitude) }) }
                .addOnFailureListener { cont.resume(null) }
        }
    } catch (_: SecurityException) { null } catch (_: Exception) { null }

    suspend fun requestSingleUpdate(): LatLon? = try {
        suspendCancellableCoroutine { cont ->
            val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0)
                .setMaxUpdates(1).build()
            val cb = object : LocationCallback() {
                override fun onLocationResult(r: LocationResult) {
                    fusedClient.removeLocationUpdates(this)
                    cont.resume(r.lastLocation?.let { LatLon(it.latitude, it.longitude) })
                }
            }
            fusedClient.requestLocationUpdates(req, cb, Looper.getMainLooper())
            cont.invokeOnCancellation { fusedClient.removeLocationUpdates(cb) }
        }
    } catch (_: SecurityException) { null } catch (_: Exception) { null }

    fun classifyPlace(pos: LatLon): PlaceType {
        if (hasHome()) {
            val lat = prefs.getFloat(HOME_LAT, NO_COORD)
            val lon = prefs.getFloat(HOME_LON, NO_COORD)
            if (distM(pos, LatLon(lat.toDouble(), lon.toDouble())) < RADIUS_M) return PlaceType.HOME
        }
        if (hasWork()) {
            val lat = prefs.getFloat(WORK_LAT, NO_COORD)
            val lon = prefs.getFloat(WORK_LON, NO_COORD)
            if (distM(pos, LatLon(lat.toDouble(), lon.toDouble())) < RADIUS_M) return PlaceType.WORK
        }
        return PlaceType.OTHER
    }

    fun saveHome(pos: LatLon) {
        prefs.edit().putFloat(HOME_LAT, pos.lat.toFloat()).putFloat(HOME_LON, pos.lon.toFloat()).apply()
    }
    fun saveWork(pos: LatLon) {
        prefs.edit().putFloat(WORK_LAT, pos.lat.toFloat()).putFloat(WORK_LON, pos.lon.toFloat()).apply()
    }
    // FIX 5: usa contains() invece di confrontare con sentinel
    fun hasHome() = prefs.contains(HOME_LAT)
    fun hasWork() = prefs.contains(WORK_LAT)

    private fun distM(a: LatLon, b: LatLon): Float {
        val r = FloatArray(1)
        Location.distanceBetween(a.lat, a.lon, b.lat, b.lon, r)
        return r[0]
    }
}
