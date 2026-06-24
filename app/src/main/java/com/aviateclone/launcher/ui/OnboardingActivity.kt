package com.aviateclone.launcher.ui

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.aviateclone.launcher.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var b: ActivityOnboardingBinding
    private val vm: LauncherViewModel by viewModels()
    private var step = 0

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val gotLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                          results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val gotCalendar = results[Manifest.permission.READ_CALENDAR] == true

        if (gotLocation) vm.refreshLocation()
        // FIX 11: refreshCalendar chiamato subito dopo il grant
        if (gotCalendar) vm.refreshCalendar()

        showStep(step + 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(b.root)
        showStep(0)
    }

    private fun showStep(s: Int) {
        step = s
        b.onboardCard.animate().cancel()
        b.onboardCard.alpha = 0f
        b.onboardCard.translationY = 48f
        b.onboardCard.animate().alpha(1f).translationY(0f)
            .setDuration(450).setInterpolator(OvershootInterpolator(0.9f)).start()

        when (step) {
            0 -> config("👋", "Benvenuto in AviateClone",
                "Un launcher intelligente che si adatta alla tua giornata — proprio come l'originale Aviate.",
                "Inizia", showSkip = false) { showStep(1) }

            1 -> config("📍", "Posizione intelligente",
                "AviateClone rileva quando sei a casa o al lavoro e cambia le app suggerite di conseguenza.",
                "Abilita posizione", showSkip = true,
                skip = { showStep(2) }) {
                permLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION))
            }

            2 -> config("🏠", "Dov'è casa?",
                "Sei a casa adesso? Salvala per ricevere suggerimenti personalizzati la mattina e la sera.",
                "Salva come Casa", showSkip = true,
                skip = { showStep(3) }) {
                vm.saveHomeLocation()
                Toast.makeText(this, "🏠 Casa salvata!", Toast.LENGTH_SHORT).show()
                showStep(3)
            }

            3 -> config("💼", "Dov'è il lavoro?",
                "Sei al lavoro adesso? Salvala per avere le app di produttività sempre in primo piano.",
                "Salva come Lavoro", showSkip = true,
                skip = { showStep(4) }) {
                vm.saveWorkLocation()
                Toast.makeText(this, "💼 Lavoro salvato!", Toast.LENGTH_SHORT).show()
                showStep(4)
            }

            4 -> config("📅", "Calendario",
                "Possiamo mostrare i tuoi prossimi eventi direttamente nello Smart Stream?",
                "Abilita Calendario", showSkip = true,
                skip = { showStep(5) }) {
                permLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR))
            }

            5 -> config("🔔", "Badge notifiche",
                "Vuoi vedere il contatore delle notifiche sulle icone delle app?",
                "Abilita Badge", showSkip = true,
                skip = { showStep(6) }) {
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                showStep(6)
            }

            6 -> {
                // Avvia bootstrap ML da UsageStats in background
                Thread {
                    com.aviateclone.launcher.engine.ContextEngine(this@OnboardingActivity)
                        .bootstrapFromUsageStats()
                }.start()
                config("🚀", "Tutto pronto!",
                    "Swipe a sinistra per lo Smart Stream, a destra per Collezioni e Cassetto App.\n\nTieni premuto la home per cambiare sfondo, pizzica per aprire tutte le app.",
                    "Inizia ad usare AviateClone", showSkip = false) {
                    getSharedPreferences("aviate_prefs", 0).edit()
                        .putBoolean("onboarding_done", true).apply()
                    finish()
                }
            }

            else -> finish()
        }

        buildDots(step, 7)
    }

    private fun config(
        emoji: String, title: String, desc: String,
        btnText: String, showSkip: Boolean,
        skip: (() -> Unit)? = null,
        action: () -> Unit
    ) {
        b.onboardEmoji.text = emoji
        b.onboardTitle.text = title
        b.onboardDesc.text = desc
        b.btnOnboardMain.text = btnText
        b.btnOnboardMain.setOnClickListener { action() }
        b.btnOnboardSkip.visibility = if (showSkip) View.VISIBLE else View.GONE
        skip?.let { b.btnOnboardSkip.setOnClickListener { it() } }
    }

    private fun buildDots(current: Int, total: Int) {
        b.onboardDots.removeAllViews()
        val dp = resources.displayMetrics.density
        for (i in 0 until total) {
            val dot = View(this)
            val w = if (i == current) (20 * dp).toInt() else (6 * dp).toInt()
            dot.layoutParams = LinearLayout.LayoutParams(w, (6 * dp).toInt()).apply {
                setMargins((4 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            }
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = 10 * dp
                setColor(if (i == current) Color.parseColor("#4D5BBF")
                         else Color.parseColor("#C7C5D0"))
            }
            b.onboardDots.addView(dot)
        }
    }
}
