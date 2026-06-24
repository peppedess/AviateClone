package com.aviateclone.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aviateclone.launcher.engine.TimeContext
import com.aviateclone.launcher.ui.compose.SmartStreamScreen
import com.aviateclone.launcher.ui.theme.AviateCloneTheme

/**
 * Smart Stream migrato a Jetpack Compose.
 * Ospita la UI in una ComposeView ma resta un Fragment, così convive con il
 * ViewPager2 esistente e con le altre schermate ancora in XML.
 */
class SmartStreamFragment : Fragment() {

    private val vm: LauncherViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AviateCloneTheme {
                val statusBar = WindowInsets.statusBars.asPaddingValues()
                    .calculateTopPadding()
                SmartStreamScreen(
                    vm = vm,
                    statusBarPadding = statusBar,
                    onAppClick = { app ->
                        (activity as? MainActivity)?.launchAppFromView(app, null)
                    },
                    onModeClick = { showContextPicker() },
                    onModeLongClick = {
                        startActivity(android.content.Intent(requireContext(),
                            GeminiSettingsActivity::class.java))
                    },
                    onPillClick = { ctx -> vm.setManualContext(ctx) }
                )
            }
        }
    }

    private fun showContextPicker() {
        val ctxs = TimeContext.values()
        val labels = (ctxs.map { "${it.emoji}  ${it.label}" } + "✦  Automatico").toTypedArray()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Scegli modalità")
            .setItems(labels) { _, which ->
                vm.setManualContext(if (which < ctxs.size) ctxs[which] else null)
            }
            .show()
    }
}
