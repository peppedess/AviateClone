package com.aviateclone.launcher.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.aviateclone.launcher.R
import com.aviateclone.launcher.adapter.AppGridAdapter
import com.aviateclone.launcher.databinding.FragmentSmartStreamBinding
import com.aviateclone.launcher.engine.TimeContext
import java.text.SimpleDateFormat
import java.util.*

class SmartStreamFragment : Fragment() {

    private var _b: FragmentSmartStreamBinding? = null
    private val b get() = _b!!
    private val vm: LauncherViewModel by activityViewModels()

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN)
    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() { updateClock(); handler.postDelayed(this, 10_000) }
    }
    private var sysReceiver: BroadcastReceiver? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSmartStreamBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar spacer
        b.statusBarSpacer.layoutParams.height = getStatusBarHeight()
        b.statusBarSpacer.requestLayout()

        // App suggerite con animazione staggered
        val appsAdapter = AppGridAdapter(
            onAppClick = { app ->
                (activity as? MainActivity)?.launchApp(app)
            }
        )
        b.rvStreamApps.apply {
            adapter = appsAdapter
            layoutManager = GridLayoutManager(requireContext(), 4)
            isNestedScrollingEnabled = false
            layoutAnimation = android.view.animation.AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.layout_anim_grid)
        }

        vm.suggestedApps.observe(viewLifecycleOwner) { apps ->
            appsAdapter.submitList(apps.take(8))
            b.rvStreamApps.scheduleLayoutAnimation()
        }

        vm.currentContext.observe(viewLifecycleOwner) { ctx ->
            b.tvSuggestedTitle.text = getLabelFor(ctx)
            b.tvModeEmoji.text = ctx.emoji
            b.tvModeLabel.text = ctx.label
            buildModePills(ctx)
            // Animazione bounce sul FAB quando cambia contesto
            animateFab(b.btnModeSelector)
        }

        // FAB modalità: tap apre bottom sheet picker
        b.btnModeSelector.setOnClickListener {
            animateFab(it)
            showModePicker()
        }

        updateClock()
        updateBattery()
        updateWifi()
        checkHeadphones()
        handler.post(clockTick)
        registerReceivers()

        // Animazione di entrata per le info card
        animateInfoCards()
    }

    // ── Animazione entrata info card: staggered slide-up ─────────────────
    private fun animateInfoCards() {
        val cards = listOf(b.sectionBattery, b.sectionWifi)
        cards.forEachIndexed { i, card ->
            card.alpha = 0f
            card.translationY = 40f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((i * 80 + 200).toLong())
                .setDuration(400)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }
    }

    // ── Animazione spring/bounce sul FAB ──────────────────────────────────
    private fun animateFab(v: View) {
        val scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.88f, 1.06f, 1f)
        val scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.88f, 1.06f, 1f)
        scaleX.duration = 400; scaleY.duration = 400
        scaleX.interpolator = OvershootInterpolator(3f)
        scaleY.interpolator = OvershootInterpolator(3f)
        AnimatorSet().apply { playTogether(scaleX, scaleY); start() }
    }

    // ── Pills modalità orizzontali ────────────────────────────────────────
    private fun buildModePills(current: TimeContext) {
        b.llModePills.removeAllViews()
        val dp = resources.displayMetrics.density
        TimeContext.values().forEachIndexed { idx, ctx ->
            val isActive = ctx == current
            val pill = TextView(requireContext()).apply {
                text = "${ctx.emoji}  ${ctx.label}"
                textSize = 12f
                setTextColor(if (isActive)
                    ContextCompat.getColor(requireContext(), R.color.md_on_primary_container)
                else
                    ContextCompat.getColor(requireContext(), R.color.text_secondary))
                background = ContextCompat.getDrawable(requireContext(),
                    if (isActive) R.drawable.bg_mode_pill else R.drawable.bg_mode_pill_outline)
                val ph = (16 * dp).toInt(); val pv = (8 * dp).toInt()
                setPadding(ph, pv, ph, pv)
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (8 * dp).toInt() }
                setOnClickListener {
                    vm.setManualContext(if (isActive) null else ctx)
                }
                // Animazione entrata staggered
                alpha = 0f; translationX = 20f
                animate().alpha(1f).translationX(0f)
                    .setStartDelay((idx * 50).toLong()).setDuration(250)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            b.llModePills.addView(pill)
        }
    }

    // ── Bottom sheet picker modalità ─────────────────────────────────────
    private fun showModePicker() {
        val modes = TimeContext.values()
        val labels = modes.map { "${it.emoji}  ${it.label}" }.toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Modalità")
            .setItems(labels) { _, which ->
                val current = vm.currentContext.value
                vm.setManualContext(if (modes[which] == current) null else modes[which])
            }
            .setNeutralButton("🔄  Automatico") { _, _ -> vm.setManualContext(null) }
            .show()
    }

    // ── Orologio ──────────────────────────────────────────────────────────
    private fun updateClock() {
        val now = Date()
        _b?.tvStreamTime?.text = timeFmt.format(now)
        _b?.tvStreamDate?.text = dateFmt.format(now).replaceFirstChar { it.uppercase() }
    }

    // ── Batteria ─────────────────────────────────────────────────────────
    private fun updateBattery() {
        try {
            val i = requireContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scl = i?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (lvl >= 0 && scl > 0) {
                val pct = (lvl * 100f / scl).toInt()
                val chg = (i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1) ==
                        BatteryManager.BATTERY_STATUS_CHARGING
                _b?.tvBatteryLevel?.text = if (chg) "$pct% ⚡" else "$pct%"
            }
        } catch (_: Exception) { _b?.tvBatteryLevel?.text = "—" }
    }

    // ── WiFi ─────────────────────────────────────────────────────────────
    private fun updateWifi() {
        try {
            val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                when {
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> {
                        val wm = requireContext().applicationContext
                            .getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val ssid = wm.connectionInfo?.ssid?.replace("\"", "") ?: ""
                        _b?.tvWifiName?.text = if (ssid.isNotBlank() && ssid != "<unknown ssid>") ssid else "Wi-Fi connesso"
                    }
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true ->
                        _b?.tvWifiName?.text = "Dati mobili"
                    else -> _b?.tvWifiName?.text = "Non connesso"
                }
            }
        } catch (_: Exception) { _b?.tvWifiName?.text = "—" }
    }

    // ── Cuffie ────────────────────────────────────────────────────────────
    private fun checkHeadphones() {
        try {
            val am = requireContext().getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val on = am.isWiredHeadsetOn || am.isBluetoothA2dpOn
            val v = _b ?: return
            if (on && v.sectionHeadphones.visibility != View.VISIBLE) {
                v.sectionHeadphones.visibility = View.VISIBLE
                v.sectionHeadphones.alpha = 0f
                v.sectionHeadphones.animate().alpha(1f).setDuration(300).start()
            } else if (!on) {
                v.sectionHeadphones.visibility = View.GONE
            }
        } catch (_: Exception) {}
    }

    private fun getLabelFor(ctx: TimeContext) = when (ctx) {
        TimeContext.MORNING -> "BUONA MATTINA"; TimeContext.COMMUTE -> "IN VIAGGIO"
        TimeContext.WORK -> "AL LAVORO"; TimeContext.LUNCH -> "PAUSA PRANZO"
        TimeContext.EVENING -> "BUONA SERATA"; TimeContext.NIGHT -> "BUONANOTTE"
    }

    private fun registerReceivers() {
        sysReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                updateBattery(); checkHeadphones(); updateWifi()
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.a2dp.profile.action.CONNECTION_STATE_CHANGED")
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        try { requireContext().registerReceiver(sysReceiver, f) } catch (_: Exception) {}
    }

    private fun getStatusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 60
    }

    override fun onResume() {
        super.onResume()
        vm.refreshContext(); updateClock(); updateBattery(); updateWifi(); checkHeadphones()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(clockTick)
        sysReceiver?.let { try { requireContext().unregisterReceiver(it) } catch (_: Exception) {} }
        _b = null
    }
}
