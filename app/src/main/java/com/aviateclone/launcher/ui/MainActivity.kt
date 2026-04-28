package com.aviateclone.launcher.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.viewpager2.widget.ViewPager2
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ActivityMainBinding
import com.aviateclone.launcher.receiver.AppChangeReceiver
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    val viewModel: LauncherViewModel by viewModels()
    private var appReceiver: AppChangeReceiver? = null

    companion object {
        const val HOME_PAGE = 1
        private const val TOTAL_PAGES = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Applica Dynamic Color M3 (Android 12+, fallback automatico su versioni precedenti)
        DynamicColors.applyToActivityIfAvailable(this)

        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Navigation bar inset per il dock
        ViewCompat.setOnApplyWindowInsetsListener(b.dockContainer) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            b.navBarSpacer.layoutParams.height = nav.bottom
            b.navBarSpacer.requestLayout()
            insets
        }

        setupViewPager()
        setupDock()
        setupBackHandler()
        registerAppReceiver()
        if (!hasUsageStatsPermission()) requestUsageStatsPermission()
    }

    private fun setupViewPager() {
        b.viewPager.apply {
            adapter = LauncherPagerAdapter(this@MainActivity)
            setCurrentItem(HOME_PAGE, false)
            offscreenPageLimit = 2
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) = updateDots(position)
            })
        }
        updateDots(HOME_PAGE)
    }

    private fun updateDots(current: Int) {
        b.pageIndicator.removeAllViews()
        val dp = resources.displayMetrics.density
        for (i in 0 until TOTAL_PAGES) {
            val active = i == current
            val dot = ImageView(this)
            val size = ((if (active) 8 else 5) * dp).toInt()
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            }
            dot.setBackgroundResource(
                if (active) android.R.drawable.presence_online
                else android.R.drawable.presence_invisible)
            dot.alpha = if (active) 1f else 0.4f

            // Spring animation sul dot attivo
            if (active) {
                dot.scaleX = 0.3f; dot.scaleY = 0.3f
                SpringAnimation(dot, SpringAnimation.SCALE_X, 1f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_LOW
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    start()
                }
                SpringAnimation(dot, SpringAnimation.SCALE_Y, 1f).apply {
                    spring.stiffness = SpringForce.STIFFNESS_LOW
                    spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                    start()
                }
            }
            b.pageIndicator.addView(dot)
        }
    }

    private fun setupDock() {
        val slots = listOf(
            b.dockIcon1 to b.dockSlot1, b.dockIcon2 to b.dockSlot2,
            b.dockIcon3 to b.dockSlot3, b.dockIcon4 to b.dockSlot4)

        viewModel.allApps.observe(this) { apps ->
            if (apps.isEmpty()) return@observe
            val priorities = listOf(
                listOf("dialer", "phone"), listOf("message", "sms"),
                listOf("browser", "chrome", "firefox"), listOf("camera", "cam"))
            val dock = mutableListOf<AppInfo>()
            for (kws in priorities)
                apps.firstOrNull { a -> kws.any { a.packageName.lowercase().contains(it) }
                        && dock.none { d -> d.packageName == a.packageName } }?.let { dock.add(it) }
            for (app in apps) {
                if (dock.size >= 4) break
                if (dock.none { it.packageName == app.packageName }) dock.add(app)
            }
            slots.forEachIndexed { i, (icon, slot) ->
                val app = dock.getOrNull(i) ?: return@forEachIndexed
                icon.setImageDrawable(app.icon)
                slot.setOnClickListener {
                    // Spring bounce al tap icona dock
                    SpringAnimation(icon, SpringAnimation.SCALE_X, 1f).apply {
                        setStartValue(0.85f)
                        spring.stiffness = SpringForce.STIFFNESS_LOW
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        start()
                    }
                    SpringAnimation(icon, SpringAnimation.SCALE_Y, 1f).apply {
                        setStartValue(0.85f)
                        spring.stiffness = SpringForce.STIFFNESS_LOW
                        spring.dampingRatio = SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY
                        start()
                    }
                    launchApp(app)
                }
                slot.setOnLongClickListener { showAppOptions(app); true }
            }
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (b.viewPager.currentItem != HOME_PAGE)
                    b.viewPager.setCurrentItem(HOME_PAGE, true)
            }
        })
    }

    fun launchApp(app: AppInfo) {
        try {
            viewModel.recordLaunch(app.packageName)
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                ?: run { Toast.makeText(this, "Impossibile aprire ${app.appName}", Toast.LENGTH_SHORT).show(); return }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Errore: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun showAppOptions(app: AppInfo) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
            else @Suppress("DEPRECATION") appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    private fun requestUsageStatsPermission() {
        AlertDialog.Builder(this)
            .setTitle("App recenti")
            .setMessage("Per mostrare le app più usate, serve il permesso di accesso alle statistiche di utilizzo.\n\nImpostazioni → Accesso utilizzo app → AviateClone → attiva.")
            .setPositiveButton("Apri impostazioni") { _, _ -> startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            .setNegativeButton("Dopo") { d, _ -> d.dismiss() }
            .show()
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

    override fun onResume() { super.onResume(); viewModel.refreshContext() }
    override fun onDestroy() {
        super.onDestroy()
        appReceiver?.let { try { unregisterReceiver(it) } catch (_: Exception) {} }
    }
}
