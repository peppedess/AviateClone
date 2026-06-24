package com.aviateclone.launcher.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.engine.TimeContext
import com.aviateclone.launcher.ui.LauncherViewModel
import com.aviateclone.launcher.ui.theme.AviateTheme

/**
 * Smart Stream in Compose. Riusa il LauncherViewModel esistente (LiveData
 * osservati con observeAsState) senza modificarne la logica.
 *
 * @param onAppClick lancio app (gestito da MainActivity)
 * @param onModeClick apertura del context picker
 * @param onModeLongClick apertura impostazioni Gemini
 * @param onPillClick selezione manuale di un contesto
 */
@Composable
fun SmartStreamScreen(
    vm: LauncherViewModel,
    statusBarPadding: Dp = 0.dp,
    onAppClick: (AppInfo) -> Unit,
    onModeClick: () -> Unit,
    onModeLongClick: () -> Unit,
    onPillClick: (TimeContext?) -> Unit
) {
    val ctx        by vm.currentContext.observeAsState()
    val suggested  by vm.suggestedApps.observeAsState(emptyList())
    val weather    by vm.weather.observeAsState()
    val events     by vm.events.observeAsState(emptyList())
    val missed     by vm.missedCalls.observeAsState(emptyList())
    val sms        by vm.unreadSms.observeAsState(emptyList())
    val headphones by vm.headphones.observeAsState(false to "")
    val media      by vm.mediaPlaying.observeAsState()
    val briefing   by vm.aiBriefing.observeAsState()
    val topNotif   by vm.topRankedNotif.observeAsState()
    val digest     by vm.weeklyDigest.observeAsState()
    val isCarBt    by vm.isCarBluetooth.observeAsState(false)

    val extra = AviateTheme.extra
    val currentCtx = ctx ?: TimeContext.MORNING

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = statusBarPadding + 16.dp, bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header: contesto + saluto ────────────────────────────────────
        item {
            ModeHeader(
                ctx = currentCtx,
                isManual = vm.contextEngine.isManualOverride(),
                onClick = onModeClick,
                onLongClick = onModeLongClick
            )
        }

        // ── Pills di selezione contesto ──────────────────────────────────
        item {
            ModePills(
                current = currentCtx,
                isManual = vm.contextEngine.isManualOverride(),
                onPillClick = onPillClick
            )
        }

        // ── Briefing AI (se presente) ────────────────────────────────────
        briefing?.takeIf { it.isNotBlank() }?.let { text ->
            item {
                InfoCard("✨", "Briefing", text, MaterialTheme.colorScheme.primary)
            }
        }

        // ── Meteo ────────────────────────────────────────────────────────
        weather?.let { w ->
            item {
                InfoCard(
                    emoji = w.emoji,
                    title = w.description,
                    subtitle = if (w.tempC != w.feelsLike) "Percepita ${w.feelsLike}°" else "",
                    tint = extra.weatherTint,
                    trailing = "${w.tempC}°"
                )
            }
        }

        // ── Notifica prioritaria ─────────────────────────────────────────
        topNotif?.takeIf { it.score > 1.5f }?.let { n ->
            item {
                InfoCard(
                    emoji = "🔔",
                    title = n.title,
                    subtitle = if (n.reason.isNotBlank()) "${n.text.take(60)} · ${n.reason}"
                               else n.text.take(80),
                    tint = extra.notifTint
                )
            }
        }

        // ── Calendario ───────────────────────────────────────────────────
        events.firstOrNull { it.isSoon || it.isNow }?.let { ev ->
            item {
                InfoCard(
                    emoji = "📅",
                    title = ev.title,
                    subtitle = when {
                        ev.isNow  -> "● In corso"
                        ev.isSoon -> "Tra poco — ${ev.timeLabel}"
                        else      -> ev.timeLabel
                    },
                    tint = extra.calendarTint
                )
            }
        }

        // ── Suggerimenti app ─────────────────────────────────────────────
        item {
            Text(
                "PER ${currentCtx.label.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        item {
            AppSuggestionGrid(apps = suggested.take(8), onAppClick = onAppClick)
        }

        // ── Musica ───────────────────────────────────────────────────────
        media?.let { m ->
            item {
                InfoCard(
                    emoji = if (m.isPlaying) "⏸" else "▶",
                    title = m.title,
                    subtitle = m.artist,
                    tint = extra.mediaTint
                )
            }
        }

        // ── Cuffie ───────────────────────────────────────────────────────
        if (headphones.first) {
            item {
                InfoCard("🎧", "Cuffie", headphones.second.ifBlank { "Connesse" }, extra.mediaTint)
            }
        }

        // ── Auto / Bluetooth car ─────────────────────────────────────────
        if (isCarBt) {
            item {
                InfoCard("🚗", "Modalità auto", "Bluetooth veicolo connesso", extra.info)
            }
        }

        // ── Chiamate perse / SMS ─────────────────────────────────────────
        if (missed.isNotEmpty()) {
            item {
                InfoCard(
                    emoji = "📞",
                    title = if (missed.size == 1) missed[0].name else "${missed.size} chiamate perse",
                    subtitle = "",
                    tint = extra.warning
                )
            }
        }
        if (sms.isNotEmpty()) {
            item {
                InfoCard(
                    emoji = "💬",
                    title = if (sms.size == 1) sms[0].sender
                            else "${sms.sumOf { it.count }} messaggi non letti",
                    subtitle = if (sms.size == 1) sms[0].snippet else "",
                    tint = extra.info
                )
            }
        }

        // ── Digest settimanale ───────────────────────────────────────────
        digest?.let { d ->
            item {
                InfoCard("📊", "Riepilogo settimana", d.summary, MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun ModeHeader(
    ctx: TimeContext,
    isManual: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val greeting = when (ctx) {
        TimeContext.MORNING   -> "Buongiorno"
        TimeContext.COMMUTE   -> "Buon viaggio"
        TimeContext.WORK      -> "Buon lavoro"
        TimeContext.LUNCH     -> "Buona pausa"
        TimeContext.AFTERNOON -> "Buon pomeriggio"
        TimeContext.EVENING   -> "Buonasera"
        TimeContext.NIGHT     -> "Buonanotte"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (isManual) "${ctx.label} ✦" else ctx.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onClick() }
        ) {
            Text(
                "${ctx.emoji}  Modalità",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun ModePills(
    current: TimeContext,
    isManual: Boolean,
    onPillClick: (TimeContext?) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(TimeContext.values().toList()) { ctx ->
            val active = ctx == current
            Surface(
                shape = RoundedCornerShape(50),
                color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onPillClick(if (active && isManual) null else ctx) }
            ) {
                Text(
                    "${ctx.emoji} ${ctx.label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AppSuggestionGrid(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(8.dp)) {
            apps.chunked(4).forEach { rowApps ->
                Row(Modifier.fillMaxWidth()) {
                    rowApps.forEach { app ->
                        AppCell(
                            name = app.appName,
                            icon = app.icon,
                            onClick = { onAppClick(app) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // riempi le celle mancanti per mantenere la griglia allineata
                    repeat(4 - rowApps.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
