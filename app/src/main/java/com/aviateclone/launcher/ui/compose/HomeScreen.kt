package com.aviateclone.launcher.ui.compose

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aviateclone.launcher.data.AppInfo

/**
 * Home del launcher in Compose. Sfondo trasparente: il wallpaper di sistema
 * resta visibile dietro (windowShowWallpaper nel tema XML dell'Activity).
 *
 * Orologio, ricerca, preferiti e pulsante widget sono Compose; il contenitore
 * dei widget reali è una View Android passata da [widgetHostView] (gestita dal
 * Fragment, dove vivono AppWidgetHost e i launcher di binding).
 */
@Composable
fun HomeScreen(
    time: String,
    date: String,
    onDarkWallpaper: Boolean,
    favorites: List<AppInfo>,
    widgetHostView: View,
    hasWidgets: Boolean,
    statusBarPadding: Dp = 0.dp,
    navBarPadding: Dp = 0.dp,
    onAppClick: (AppInfo) -> Unit,
    onSearchClick: () -> Unit,
    onMicClick: () -> Unit,
    onAddWidget: () -> Unit,
    onFavoritesReorder: (from: Int, to: Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp, end = 20.dp,
                top = statusBarPadding + 24.dp,
                bottom = navBarPadding + 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HomeClock(
            time = time,
            date = date,
            onWallpaper = onDarkWallpaper
        )

        HomeSearchBar(
            onSearchClick = onSearchClick,
            onMicClick = onMicClick
        )

        FavoritesGrid(
            favorites = favorites,
            onAppClick = onAppClick,
            onReorder = onFavoritesReorder
        )

        WidgetSection(
            widgetHostView = widgetHostView,
            hasWidgets = hasWidgets,
            onAddWidget = onAddWidget
        )

        Spacer(Modifier.height(24.dp))
    }
}
