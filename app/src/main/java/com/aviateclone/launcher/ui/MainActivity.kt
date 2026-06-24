package com.aviateclone.launcher.ui

import android.Manifest
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ActivityMainBinding
import com.aviateclone.launcher.receiver.AppChangeReceiver
import com.aviateclone.launcher.receiver.HeadphonesReceiver
import com.aviateclone.launcher.service.AviateNotificationService
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    lateinit var b: ActivityMainBinding   // internal per HomeFragment
    val viewModel: LauncherViewModel by viewModels()
    private var appReceiver: AppChangeReceiver? = null

    companion object {
        const val HOME_PAGE = 1
        private const val TOTAL_PAGES = 4
        private const val PREF_ONBOARDING = "onboarding_done"
        // Parole chiave app telefono per dock centrale
        private val PHONE_KEYWORDS = listOf("dialer", "phone", "call", "telefon")
    }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) viewModel.refreshLocation()
        if (grants[Manifest.permission.READ_CALENDAR] == true) viewModel.refreshCalendar()
        if (grants[Manifest.permission.READ_CALL_LOG] == true ||
            grants[Manifest.permission.READ_SMS] == true) viewModel.refreshMissedCalls()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            b.navBarSpacer.layoutParams.height = nav.bottom
            b.navBarSpacer.requestLayout()
            insets
        }

        if (!getSharedPreferences("aviate_prefs", 0).getBoolean(PREF_ONBOARDING, false))
            startActivity(Intent(this, OnboardingActivity::class.java))

        setupViewPager()
        setupDock()
        setupDockSwipeUp()  // Swipe up dock → AppList
        setupBackHandler()
        setupHeadphonesCallback()
        setupMediaSession()
        registerAppReceiver()
        requestPermissionsIfNeeded()
        if (!hasUsageStatsPermission()) requestUsageStatsPermission()
    }

    private fun setupViewPager() {
        b.viewPager.apply {
            adapter = LauncherPagerAdapter(this@MainActivity)
            setCurrentItem(HOME_PAGE, false)
            offscreenPageLimit = 3
            // Transizione: fade + leggero scale, NESSUN translationX
            // (translationX != 0 causa compenetrazione tra pagine adiacenti)
            setPageTransformer { page, position ->
                val abs = kotlin.math.abs(position)
                page.alpha = when {
                    position < -1f || position > 1f -> 0f
                    else -> 1f - abs * 0.25f
                }
                page.scaleX = 1f - abs * 0.04f
                page.scaleY = 1f - abs * 0.04f
                page.translationX = 0f   // niente spostamento extra: evita overlap
                page.translationZ = -abs // z-order: pagina corrente sopra le altre
            }
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(p: Int) = updateDots(p)
            })
        }
        updateDots(HOME_PAGE)
    }

    private fun updateDots(current: Int) {
        b.pageIndicator.removeAllViews()
        val dp = resources.displayMetrics.density
        for (i in 0 until TOTAL_PAGES) {
            val active = i == current
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(
                if (active) (22 * dp).toInt() else (6 * dp).toInt(), (6 * dp).toInt()
            ).apply { setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0) }
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = 10 * dp
                setColor(if (active) Color.WHITE else Color.parseColor("#80FFFFFF"))
            }
            if (active) { dot.scaleX = 0.3f; dot.animate().scaleX(1f).setDuration(350)
                .setInterpolator(OvershootInterpolator(1.5f)).start() }
            b.pageIndicator.addView(dot)
        }
    }

    // ── Swipe up sul dock → AppList ─────────────────────────────────────────
    private fun setupDockSwipeUp() {
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (vy < -600) {
                    b.viewPager.setCurrentItem(3, true)
                    return true
                }
                return false
            }
            // onDown NON ritorna true qui: lasciamo i child ricevere i click
            override fun onDown(e: MotionEvent) = false
        })
        // FIX 1: ritorna false per non consumare i touch — i child (dockSlot) ricevono i click
        b.dockContainer.setOnTouchListener { _, e ->
            gd.onTouchEvent(e)
            false  // mai consumare: permette ai child di ricevere click
        }
    }


    private fun setupDock() {
        val iconViews  = listOf(b.dockIcon1,  b.dockIcon2,  b.dockIcon3,  b.dockIcon4)
        val slotViews  = listOf(b.dockSlot1,  b.dockSlot2,  b.dockSlot3,  b.dockSlot4)
        val badgeViews = listOf(b.dockBadge1, b.dockBadge2, b.dockBadge3, b.dockBadge4)

        viewModel.allApps.observe(this) { apps ->
            if (apps.isEmpty()) return@observe
            refreshDock(apps, iconViews, slotViews, badgeViews)
        }

        // Dock predittivo AI: se AppPredictionManager disponibile, il dock si aggiorna
        viewModel.aiDockPkgs.observe(this) { aiPkgs ->
            if (aiPkgs.isEmpty()) return@observe
            val apps = viewModel.allApps.value ?: return@observe
            refreshDock(apps, iconViews, slotViews, badgeViews)
        }

        viewModel.badgeCounts.observe(this) { badges ->
            iconViews.zip(badgeViews).forEach { (icon, badge) ->
                val pkg = icon.tag as? String ?: return@forEach
                val count = badges[pkg] ?: 0
                badge.visibility = if (count > 0) View.VISIBLE else View.GONE
                badge.text = if (count > 99) "99+" else count.toString()
            }
        }
    }

    private fun refreshDock(
        apps: List<AppInfo>,
        iconViews: List<ImageView>, slotViews: List<LinearLayout>, badgeViews: List<TextView>
    ) {
        val dockPrefs = getSharedPreferences("aviate_dock", 0)
        val dock = arrayOfNulls<AppInfo>(4)

        // Slot salvati mantengono posizione
        for (i in 0..3) {
            val pkg = dockPrefs.getString("slot_$i", null) ?: continue
            dock[i] = apps.firstOrNull { it.packageName == pkg }
        }

        val taken = dock.filterNotNull().map { it.packageName }.toMutableSet()

        // ── Telefono sempre al centro (slot 1) se libero ──────────────────
        if (dock[1] == null) {
            val phone = apps.firstOrNull { a ->
                PHONE_KEYWORDS.any { a.packageName.lowercase().contains(it) } && a.packageName !in taken
            }
            phone?.let { dock[1] = it; taken.add(it.packageName) }
        }

        // Riempi restanti con priorità
        val priorities = listOf(
            listOf("message", "sms"), listOf("browser", "chrome", "firefox"), listOf("camera"))
        val fallbacks = mutableListOf<AppInfo>()
        for (kws in priorities)
            apps.firstOrNull { a -> kws.any { a.packageName.lowercase().contains(it) } && a.packageName !in taken }
                ?.also { taken.add(it.packageName) }?.let { fallbacks.add(it) }
        for (app in apps.sortedByDescending { it.launchCount }) {
            if (fallbacks.size >= 4) break
            if (app.packageName !in taken) { fallbacks.add(app); taken.add(app.packageName) }
        }
        var fi = 0
        for (i in 0..3) { if (dock[i] == null && fi < fallbacks.size) dock[i] = fallbacks[fi++] }

        dock.forEachIndexed { i, app ->
            app ?: return@forEachIndexed
            iconViews[i].setImageDrawable(app.icon)
            iconViews[i].tag = app.packageName
            slotViews[i].setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                launchAppFromView(app, iconViews[i])
            }
            slotViews[i].setOnLongClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showDockOptions(i, app, apps, iconViews, slotViews, badgeViews); true
            }
        }
    }

    private fun showDockOptions(
        slotIndex: Int, current: AppInfo, apps: List<AppInfo>,
        iconViews: List<ImageView>, slotViews: List<LinearLayout>, badgeViews: List<TextView>
    ) {
        AlertDialog.Builder(this)
            .setTitle("Slot ${slotIndex + 1} — ${current.appName}")
            .setItems(arrayOf("Cambia app", "Info app", "Re-salva Casa qui", "Re-salva Lavoro qui")) { _, which ->
                when (which) {
                    0 -> AlertDialog.Builder(this)
                        .setTitle("Scegli app")
                        .setItems(apps.map { it.appName }.toTypedArray()) { _, w ->
                            getSharedPreferences("aviate_dock", 0).edit()
                                .putString("slot_$slotIndex", apps[w].packageName).apply()
                            refreshDock(apps, iconViews, slotViews, badgeViews)
                        }.show()
                    1 -> showAppOptions(current)
                    2 -> { viewModel.saveHomeLocation(); Toast.makeText(this, "🏠 Casa aggiornata", Toast.LENGTH_SHORT).show() }
                    3 -> { viewModel.saveWorkLocation(); Toast.makeText(this, "💼 Lavoro aggiornato", Toast.LENGTH_SHORT).show() }
                }
            }.show()
    }

    // ── Zoom-from-icon animation ────────────────────────────────────────────
    fun launchAppFromView(app: AppInfo, sourceView: View?) {
        try {
            viewModel.recordLaunch(app.packageName)
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                ?: run { Toast.makeText(this, "Impossibile aprire ${app.appName}", Toast.LENGTH_SHORT).show(); return }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

            val opts = if (sourceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Aviate-style zoom from icon
                ActivityOptions.makeScaleUpAnimation(
                    sourceView, 0, 0, sourceView.width, sourceView.height
                ).toBundle()
            } else null
            startActivity(intent, opts)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchApp(app: AppInfo) = launchAppFromView(app, null)

    fun showAppOptions(app: AppInfo) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    fun openWallpaperPicker() {
        try { startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Scegli sfondo")) }
        catch (_: Exception) { Toast.makeText(this, "Vai in Impostazioni → Sfondo", Toast.LENGTH_SHORT).show() }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (b.viewPager.currentItem != HOME_PAGE) b.viewPager.setCurrentItem(HOME_PAGE, true)
            }
        })
    }

    private fun setupHeadphonesCallback() {
        HeadphonesReceiver.onHeadphonesChanged = { connected, name ->
            runOnUiThread { viewModel.setHeadphones(connected, name) }
        }
    }

    private fun setupMediaSession() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val sessions = msm.getActiveSessions(
                android.content.ComponentName(this, AviateNotificationService::class.java))
            sessions.firstOrNull()?.let { ctrl ->
                val meta = ctrl.metadata
                val t = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val a = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                val playing = ctrl.playbackState?.state == PlaybackState.STATE_PLAYING
                viewModel.updateMediaInfo(if (t.isNotBlank()) LauncherViewModel.MediaInfo(t, a, playing) else null)
                ctrl.registerCallback(object : android.media.session.MediaController.Callback() {
                    override fun onMetadataChanged(m: MediaMetadata?) {
                        val tt = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                        val aa = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                        viewModel.updateMediaInfo(if (tt.isNotBlank()) LauncherViewModel.MediaInfo(tt, aa, true) else null)
                    }
                    override fun onPlaybackStateChanged(s: PlaybackState?) {
                        val cur = viewModel.mediaPlaying.value ?: return
                        viewModel.updateMediaInfo(cur.copy(isPlaying = s?.state == PlaybackState.STATE_PLAYING))
                    }
                    override fun onSessionDestroyed() { viewModel.updateMediaInfo(null) }
                })
            } ?: viewModel.updateMediaInfo(null)
        } catch (_: SecurityException) {} catch (_: Exception) {}
    }

    private fun requestPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_CALENDAR,
               Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_SMS).forEach { p ->
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed.add(p)
        }
        if (needed.isNotEmpty()) permLauncher.launch(needed.toTypedArray())
        else { viewModel.refreshLocation(); viewModel.refreshCalendar(); viewModel.refreshMissedCalls() }
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val ao = getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ao.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), packageName)
            else @Suppress("DEPRECATION") ao.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), packageName)
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun requestUsageStatsPermission() {
        AlertDialog.Builder(this)
            .setTitle("App recenti")
            .setMessage("Concedi l'accesso alle statistiche per suggerire le app più usate.")
            .setPositiveButton("Apri") { _, _ -> startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            .setNegativeButton("Dopo", null).show()
    }

    private fun registerAppReceiver() {
        appReceiver = AppChangeReceiver { viewModel.loadApps() }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            registerReceiver(appReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else @Suppress("UnspecifiedRegisterReceiverFlag") registerReceiver(appReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshContext()
        viewModel.refreshLocation()
        viewModel.refreshCalendar()
        viewModel.refreshMissedCalls()
        viewModel.refreshPatterns()
        viewModel.refreshWeeklyDigest()
        setupMediaSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        HeadphonesReceiver.onHeadphonesChanged = null
        AviateNotificationService.listener = null
        appReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
    }
}
