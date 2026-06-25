package com.aviateclone.launcher.ui

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.ui.compose.HomeScreen
import com.aviateclone.launcher.ui.theme.AviateCloneTheme
import com.aviateclone.launcher.widget.WallpaperPaletteHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home del launcher migrata a Compose.
 * La UI (orologio, ricerca, preferiti, pulsante widget) è Compose; la logica
 * di AppWidgetHost e i launcher di binding restano nel Fragment perché
 * richiedono registerForActivityResult e una View host nativa.
 */
class HomeFragment : Fragment() {

    private val vm: LauncherViewModel by activityViewModels()

    // ── Stato osservato da Compose ───────────────────────────────────────
    private var timeState by mutableStateOf("")
    private var dateState by mutableStateOf("")
    private var darkWallpaperState by mutableStateOf(true)
    private var hasWidgetsState by mutableStateOf(false)
    private val favorites: SnapshotStateList<AppInfo> = mutableStateListOf()

    // ── Widget ───────────────────────────────────────────────────────────
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost
    private lateinit var widgetContainer: FrameLayout
    private val HOST_ID = 1024
    private val PREFS_WIDGETS = "aviate_widgets"
    private val KEY_IDS = "widget_ids"

    // ── Orologio ─────────────────────────────────────────────────────────
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN)
    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() { updateClock(); handler.postDelayed(this, 10_000) }
    }

    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id == -1) return@registerForActivityResult
        val info: AppWidgetProviderInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            result.data?.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, AppWidgetProviderInfo::class.java)
        else @Suppress("DEPRECATION") result.data?.getParcelableExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER)
        if (info != null && !appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider))
            widgetBindLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            })
        else handlePostBind(id)
    }

    private val widgetBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == Activity.RESULT_OK && id != -1) handlePostBind(id)
        else if (id != -1) appWidgetHost.deleteAppWidgetId(id)
    }

    private val widgetConfigLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (id == -1) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) attachWidget(id) else appWidgetHost.deleteAppWidgetId(id)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Container nativo dei widget (gestito qui, mostrato in Compose via AndroidView)
        widgetContainer = FrameLayout(requireContext())

        appWidgetManager = AppWidgetManager.getInstance(requireContext())
        appWidgetHost = AppWidgetHost(requireContext(), HOST_ID)

        updateClock()
        applyWallpaperTheme()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AviateCloneTheme {
                    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    HomeScreen(
                        time = timeState,
                        date = dateState,
                        onDarkWallpaper = darkWallpaperState,
                        favorites = favorites,
                        widgetHostView = widgetContainer,
                        hasWidgets = hasWidgetsState,
                        statusBarPadding = statusBar,
                        navBarPadding = navBar,
                        onAppClick = { app ->
                            (activity as? MainActivity)?.launchAppFromView(app, null)
                        },
                        onSearchClick = { openSearch() },
                        onMicClick = { openVoiceSearch() },
                        onAddWidget = { pickWidget() },
                        onFavoritesReorder = { from, to -> onFavoritesReordered(from, to) }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Preferiti: ordine manuale salvato, con fallback alle app più usate
        vm.allApps.observe(viewLifecycleOwner) { apps -> applyFavoritesOrder(apps) }
        appWidgetHost.startListening()
        restoreWidgets()
        handler.post(clockRunnable)
    }

    // ── Ricerca ────────────────────────────────────────────────────────────
    private fun openSearch() {
        try {
            startActivity(Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", "")
                setPackage("com.google.android.googlequicksearchbox")
            })
        } catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_WEB_SEARCH)) } catch (_: Exception) {}
        }
    }

    private fun openVoiceSearch() {
        try { startActivity(Intent("android.speech.action.WEB_SEARCH")) } catch (_: Exception) {}
    }

    // ── Tema da wallpaper ──────────────────────────────────────────────────
    private fun applyWallpaperTheme() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) { darkWallpaperState = true; return }
        try { darkWallpaperState = WallpaperPaletteHelper.isDarkWallpaper(requireContext()) }
        catch (_: Exception) { darkWallpaperState = true }
    }

    // ── Widget ─────────────────────────────────────────────────────────────
    private fun pickWidget() {
        widgetPickLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetHost.allocateAppWidgetId())
        })
    }
    private fun handlePostBind(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        if (info?.configure != null)
            widgetConfigLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure; putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            })
        else attachWidget(id)
    }
    private fun attachWidget(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id) ?: return
        val density = resources.displayMetrics.density
        val wv = appWidgetHost.createView(requireContext().applicationContext, id, info)
        // Essenziale: associa esplicitamente id+info alla host view, altrimenti
        // il widget resta un riquadro vuoto.
        wv.setAppWidget(id, info)

        val widthPx = resources.displayMetrics.widthPixels - (40 * density).toInt()
        val minWidthDp = (info.minWidth / density).toInt().coerceAtLeast(40)
        val minHeightDp = (info.minHeight / density).toInt().coerceAtLeast(40)
        val widgetWidthDp = (widthPx / density).toInt().coerceAtLeast(minWidthDp)

        // Comunica al provider le dimensioni a cui renderizzare (senza questo
        // molti widget mostrano contenuto vuoto).
        try {
            wv.updateAppWidgetSize(android.os.Bundle(), minWidthDp, minHeightDp,
                widgetWidthDp, minHeightDp)
        } catch (_: Exception) { }

        wv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        wv.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            (info.minHeight.coerceAtLeast(80) * density).toInt()
        )
        wv.setOnLongClickListener { removeWidget(id, wv); true }
        widgetContainer.addView(wv)
        widgetContainer.requestLayout()
        hasWidgetsState = widgetContainer.childCount > 0
        saveWidgetId(id)
    }
    private fun removeWidget(id: Int, view: AppWidgetHostView) {
        widgetContainer.removeView(view); appWidgetHost.deleteAppWidgetId(id); deleteWidgetId(id)
        hasWidgetsState = widgetContainer.childCount > 0
        Toast.makeText(requireContext(), "Widget rimosso", Toast.LENGTH_SHORT).show()
    }
    private fun prefs() = requireContext().getSharedPreferences(PREFS_WIDGETS, 0)
    private fun saveWidgetId(id: Int) {
        val ids = prefs().getStringSet(KEY_IDS, mutableSetOf())!!.toMutableSet()
        ids.add(id.toString()); prefs().edit().putStringSet(KEY_IDS, ids).apply()
    }
    private fun deleteWidgetId(id: Int) {
        val ids = prefs().getStringSet(KEY_IDS, mutableSetOf())!!.toMutableSet()
        ids.remove(id.toString()); prefs().edit().putStringSet(KEY_IDS, ids).apply()
    }
    private fun restoreWidgets() {
        prefs().getStringSet(KEY_IDS, emptySet())?.forEach { str ->
            val id = str.toIntOrNull() ?: return@forEach
            if (appWidgetManager.getAppWidgetInfo(id) != null) attachWidget(id)
            else { appWidgetHost.deleteAppWidgetId(id); deleteWidgetId(id) }
        }
        hasWidgetsState = widgetContainer.childCount > 0
    }

    // ── Preferiti: ordine manuale persistito, fallback a top-per-lanci ────
    private val PREFS_FAVS = "aviate_favorites"
    private val KEY_ORDER = "favorites_order"

    private fun applyFavoritesOrder(apps: List<com.aviateclone.launcher.data.AppInfo>) {
        val savedOrder = prefsFavorites().getString(KEY_ORDER, null)
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        val byPkg = apps.associateBy { it.packageName }
        val ordered = savedOrder.mapNotNull { byPkg[it] }.toMutableList()

        // Eventuali app non ancora in classifica: in coda, ordinate per lanci,
        // fino a un massimo di 8 preferiti totali.
        val remaining = apps.filterNot { it.packageName in savedOrder }
            .sortedByDescending { it.launchCount }
        ordered.addAll(remaining)

        val top8 = ordered.take(8)
        favorites.clear(); favorites.addAll(top8)
    }

    private fun saveFavoritesOrder() {
        val order = favorites.joinToString(",") { it.packageName }
        prefsFavorites().edit().putString(KEY_ORDER, order).apply()
    }

    private fun prefsFavorites() = requireContext().getSharedPreferences(PREFS_FAVS, 0)

    /** Chiamato dalla UI Compose al termine di un riordino via drag&drop. */
    fun onFavoritesReordered(from: Int, to: Int) {
        if (from == to || from !in favorites.indices || to !in favorites.indices) return
        // Scambio diretto delle due posizioni (non shift): coerente con
        // l'interazione "trascina sopra un'altra icona per scambiarle".
        val tmp = favorites[from]
        favorites[from] = favorites[to]
        favorites[to] = tmp
        saveFavoritesOrder()
    }

    // ── Orologio ───────────────────────────────────────────────────────────
    private fun updateClock() {
        val now = Date()
        timeState = timeFmt.format(now)
        dateState = dateFmt.format(now).replaceFirstChar { it.uppercase() }
    }

    override fun onResume() {
        super.onResume()
        updateClock()
        applyWallpaperTheme()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(clockRunnable)
        if (::appWidgetHost.isInitialized) {
            try { appWidgetHost.stopListening() } catch (_: Exception) {}
        }
    }
}
