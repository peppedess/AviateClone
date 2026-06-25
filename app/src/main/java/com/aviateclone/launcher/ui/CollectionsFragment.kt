package com.aviateclone.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aviateclone.launcher.ui.compose.CollectionsScreen
import com.aviateclone.launcher.ui.theme.AviateCloneTheme

/**
 * Collezioni migrate a Compose: app raggruppate per categoria con ricerca.
 */
class CollectionsFragment : Fragment() {

    private val vm: LauncherViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AviateCloneTheme {
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val categories by vm.appsByCategory.observeAsState(emptyMap())
                val badges by vm.badgeCounts.observeAsState(emptyMap())
                CollectionsScreen(
                    categories = categories,
                    badgeCounts = badges,
                    statusBarPadding = statusBar,
                    onAppClick = { app ->
                        (activity as? MainActivity)?.launchApp(app)
                    }
                )
            }
        }
    }
}
