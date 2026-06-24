package com.aviateclone.launcher.ai

import android.app.prediction.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.aviateclone.launcher.data.AppInfo
import java.util.concurrent.Executors

/**
 * Wrapper attorno a AppPredictionManager (API 29+).
 * Google addestra questo modello su miliardi di dispositivi —
 * è molto più accurato del nostro scoring EMA.
 * Fallback automatico al nostro ML se non disponibile.
 */
class AiPredictionManager(private val context: Context) {

    private var homeSession: AppPredictionSession? = null
    private var dockSession: AppPredictionSession? = null
    private val executor = Executors.newSingleThreadExecutor()

    val isAvailable: Boolean get() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_APP_PREDICTION_API)

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startHomePredictions(count: Int = 8, onResult: (List<String>) -> Unit) {
        try {
            val mgr = context.getSystemService(AppPredictionManager::class.java) ?: return
            val ctx = AppPredictionContext.Builder(context)
                .setUiSurface("home")
                .setPredictedTargetCount(count)
                .build()
            homeSession = mgr.createAppPredictionSession(ctx)
            homeSession?.registerPredictionUpdates(executor) { targets ->
                onResult(targets.map { it.packageName })
            }
            homeSession?.requestPredictionUpdate()
        } catch (_: Exception) { /* sistema non supporta → fallback */ }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startDockPredictions(count: Int = 4, onResult: (List<String>) -> Unit) {
        try {
            val mgr = context.getSystemService(AppPredictionManager::class.java) ?: return
            val ctx = AppPredictionContext.Builder(context)
                .setUiSurface("dock")
                .setPredictedTargetCount(count)
                .build()
            dockSession = mgr.createAppPredictionSession(ctx)
            dockSession?.registerPredictionUpdates(executor) { targets ->
                onResult(targets.map { it.packageName })
            }
            dockSession?.requestPredictionUpdate()
        } catch (_: Exception) {}
    }

    /** Notifica al sistema di un lancio app → migliora il modello ML */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun notifyLaunch(packageName: String, surface: String = "home") {
        try {
            val target = AppTarget.Builder(
                AppTargetId("$packageName@${System.currentTimeMillis()}"),
                packageName,
                Process.myUserHandle()
            ).build()
            val event = AppTargetEvent.Builder(target, AppTargetEvent.ACTION_LAUNCH)
                .setLaunchLocation(surface)
                .build()
            homeSession?.notifyAppTargetEvent(event)
            dockSession?.notifyAppTargetEvent(event)
        } catch (_: Exception) {}
    }

    fun destroy() {
        try { homeSession?.destroy(); dockSession?.destroy() }
        catch (_: Exception) {}
        executor.shutdown()
    }
}
