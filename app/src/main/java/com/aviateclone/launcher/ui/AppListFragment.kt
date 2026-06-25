package com.aviateclone.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aviateclone.launcher.ui.compose.AppListScreen
import com.aviateclone.launcher.ui.theme.AviateCloneTheme

/**
 * Cassetto app migrato a Compose: ricerca, lista raggruppata per lettera,
 * side alphabet con tap/drag per saltare a una sezione.
 */
class AppListFragment : Fragment() {

    private val vm: LauncherViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AviateCloneTheme {
                val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val apps by vm.allApps.observeAsState(emptyList())
                val badges by vm.badgeCounts.observeAsState(emptyMap())

                AppListScreen(
                    apps = apps,
                    badgeCounts = badges,
                    statusBarPadding = statusBar,
                    navBarPadding = navBar,
                    onAppClick = { app ->
                        (activity as? MainActivity)?.launchAppFromView(app, null)
                    },
                    onAppLongClick = { app ->
                        (activity as? MainActivity)?.showAppOptions(app)
                    }
                )
            }
        }
    }
}
