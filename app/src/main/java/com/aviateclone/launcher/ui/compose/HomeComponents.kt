package com.aviateclone.launcher.ui.compose

import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aviateclone.launcher.data.AppInfo

/**
 * Orologio + data. Testo chiaro/scuro deciso dal chiamante in base al
 * wallpaper (parametro [onWallpaper] = testo bianco con ombra).
 */
@Composable
fun HomeClock(
    time: String,
    date: String,
    onWallpaper: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (onWallpaper) Color.White else MaterialTheme.colorScheme.onSurface
    val subColor = if (onWallpaper) Color.White.copy(alpha = 0.85f)
                   else MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier) {
        Text(
            time,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            color = textColor,
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            date,
            style = MaterialTheme.typography.titleMedium,
            color = subColor
        )
    }
}

/** Barra di ricerca in stile pill, con icona lente e microfono. */
@Composable
fun HomeSearchBar(
    onSearchClick: () -> Unit,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .clickable { onSearchClick() },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Cerca",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Cerca",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.Mic,
                contentDescription = "Ricerca vocale",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onMicClick() }
                    .padding(2.dp)
            )
        }
    }
}

/**
 * Griglia delle app preferite (4 colonne). Click lancia l'app; il riordino
 * drag&drop verrà gestito a parte. Per ora supporta long-press → opzioni.
 */
@Composable
fun FavoritesGrid(
    favorites: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        favorites.chunked(4).forEach { rowApps ->
            Row(Modifier.fillMaxWidth()) {
                rowApps.forEach { app ->
                    AppCell(
                        name = app.appName,
                        icon = app.icon,
                        onClick = { onAppClick(app) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Sezione widget: contiene il FrameLayout host (gestito dal Fragment, dove
 * vivono AppWidgetHost e i launcher di binding) + il pulsante "aggiungi".
 * Il [widgetHostView] è la View Android creata e popolata dal Fragment.
 */
@Composable
fun WidgetSection(
    widgetHostView: View,
    hasWidgets: Boolean,
    onAddWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Contenitore dei widget reali (AndroidView fa da ponte verso la View)
        AndroidView(
            factory = { widgetHostView },
            modifier = Modifier.fillMaxWidth()
        )
        if (!hasWidgets) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onAddWidget() },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Aggiungi widget",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onAddWidget() },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Widget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
