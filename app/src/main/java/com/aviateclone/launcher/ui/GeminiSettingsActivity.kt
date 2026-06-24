package com.aviateclone.launcher.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.aviateclone.launcher.databinding.ActivityGeminiSettingsBinding

class GeminiSettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivityGeminiSettingsBinding
    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityGeminiSettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Mostra chiave parzialmente mascherata se già salvata
        if (vm.hasGeminiKey()) {
            b.etApiKey.hint = "Chiave già salvata — incolla per aggiornare"
        }

        b.btnSaveKey.setOnClickListener {
            val key = b.etApiKey.text?.toString()?.trim() ?: ""
            if (key.length < 20) {
                Toast.makeText(this, "Chiave non valida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.saveGeminiKey(key)
            Toast.makeText(this, "✓ Chiave Gemini salvata", Toast.LENGTH_SHORT).show()
            finish()
        }

        b.btnGetKey.setOnClickListener {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://aistudio.google.com/app/apikey")
            })
        }

        b.btnClose.setOnClickListener { finish() }
    }
}
