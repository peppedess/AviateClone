package com.aviateclone.launcher.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aviateclone.launcher.ai.*
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.data.AppLoader
import com.aviateclone.launcher.engine.ContextEngine
import com.aviateclone.launcher.engine.TimeContext
import com.aviateclone.launcher.repository.*
import com.aviateclone.launcher.service.AviateNotificationService
import com.aviateclone.launcher.worker.UsageStatsWorker
import kotlinx.coroutines.*
import java.util.Calendar

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val contextEngine   = ContextEngine(application)
    private val locEng  = LocationEngine(application)
    val aiPredictor     = AiPredictionManager(application)

    // ── LiveData ───────────────────────────────────────────────────────────
    private val _allApps        = MutableLiveData<List<AppInfo>>()
    val allApps: LiveData<List<AppInfo>> = _allApps

    private val _suggestedApps  = MutableLiveData<List<AppInfo>>()
    val suggestedApps: LiveData<List<AppInfo>> = _suggestedApps

    private val _aiSuggestedPkgs = MutableLiveData<List<String>>(emptyList())
    val aiSuggestedPkgs: LiveData<List<String>> = _aiSuggestedPkgs

    private val _aiDockPkgs     = MutableLiveData<List<String>>(emptyList())
    val aiDockPkgs: LiveData<List<String>> = _aiDockPkgs

    private val _appsByCategory = MutableLiveData<Map<AppCategory, List<AppInfo>>>()
    val appsByCategory: LiveData<Map<AppCategory, List<AppInfo>>> = _appsByCategory

    private val _currentContext = MutableLiveData<TimeContext>()
    val currentContext: LiveData<TimeContext> = _currentContext

    private val _currentPlace   = MutableLiveData<PlaceType>(PlaceType.OTHER)
    val currentPlace: LiveData<PlaceType> = _currentPlace

    private val _weather        = MutableLiveData<WeatherData?>()
    val weather: LiveData<WeatherData?> = _weather

    private val _events         = MutableLiveData<List<CalendarEvent>>(emptyList())
    val events: LiveData<List<CalendarEvent>> = _events

    private val _missedCalls    = MutableLiveData<List<MissedCall>>(emptyList())
    val missedCalls: LiveData<List<MissedCall>> = _missedCalls

    private val _unreadSms      = MutableLiveData<List<UnreadSms>>(emptyList())
    val unreadSms: LiveData<List<UnreadSms>> = _unreadSms

    private val _headphones     = MutableLiveData<Pair<Boolean, String>>(false to "")
    val headphones: LiveData<Pair<Boolean, String>> = _headphones

    private val _isCarBluetooth = MutableLiveData<Boolean>(false)
    val isCarBluetooth: LiveData<Boolean> = _isCarBluetooth

    private val _badgeCounts    = MutableLiveData<Map<String, Int>>(emptyMap())
    val badgeCounts: LiveData<Map<String, Int>> = _badgeCounts

    private val _currentLocation = MutableLiveData<LatLon?>()
    val currentLocation: LiveData<LatLon?> = _currentLocation

    private val _mediaPlaying   = MutableLiveData<MediaInfo?>()
    val mediaPlaying: LiveData<MediaInfo?> = _mediaPlaying

    // ── AI LiveData ────────────────────────────────────────────────────────
    private val _aiBriefing     = MutableLiveData<String?>()
    val aiBriefing: LiveData<String?> = _aiBriefing

    private val _aiNotifSummary = MutableLiveData<String?>()
    val aiNotifSummary: LiveData<String?> = _aiNotifSummary

    private val _patterns       = MutableLiveData<List<AppPattern>>(emptyList())
    val patterns: LiveData<List<AppPattern>> = _patterns

    private val _weeklyDigest   = MutableLiveData<WeeklyDigest?>()
    val weeklyDigest: LiveData<WeeklyDigest?> = _weeklyDigest

    private val _topRankedNotif = MutableLiveData<RankedNotification?>()
    val topRankedNotif: LiveData<RankedNotification?> = _topRankedNotif

    data class MediaInfo(val title: String, val artist: String, val isPlaying: Boolean)

    companion object {
        private val CAR_KEYWORDS = listOf("car","auto","bmw","audi","volkswagen","vw",
            "mercedes","fiat","ford","opel","renault","peugeot","toyota","honda",
            "android auto","carplay","parrot","kenwood","pioneer","jvc")
    }

    // ── Init ───────────────────────────────────────────────────────────────
    init {
        loadApps()
        connectNotificationBadges()
        UsageStatsWorker.schedule(application)
        startAiPredictions()
    }

    private fun startAiPredictions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && aiPredictor.isAvailable) {
            aiPredictor.startHomePredictions(8) { pkgs ->
                _aiSuggestedPkgs.postValue(pkgs)
                // Aggiorna suggestedApps con l'ordine AI
                mergeAiWithApps(pkgs)
            }
            aiPredictor.startDockPredictions(4) { pkgs ->
                _aiDockPkgs.postValue(pkgs)
            }
        }
    }

    private fun mergeAiWithApps(aiPkgs: List<String>) {
        val apps = _allApps.value ?: return
        val aiOrdered = aiPkgs.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        val rest = apps.filter { it.packageName !in aiPkgs }
            .sortedByDescending { it.launchCount }
        _suggestedApps.postValue((aiOrdered + rest).take(8))
    }

    // ── App loading ────────────────────────────────────────────────────────
    fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { AppLoader.loadApps(getApplication()) }
            val enriched = contextEngine.enrichWithUsageData(apps)
            _allApps.value = enriched
            _appsByCategory.value = enriched.groupBy { it.category }.filter { it.value.isNotEmpty() }
            refreshContext(enriched)
        }
    }

    // ── Context ────────────────────────────────────────────────────────────
    fun refreshContext(apps: List<AppInfo> = _allApps.value ?: emptyList()) {
        viewModelScope.launch {
            val place = _currentPlace.value ?: PlaceType.OTHER
            _currentContext.value = contextEngine.getCurrentContext(place)
            // Se AI predictions disponibili, usa quelle; altrimenti scoring nostro
            if (_aiSuggestedPkgs.value?.isNotEmpty() == true) {
                mergeAiWithApps(_aiSuggestedPkgs.value!!)
            } else {
                _suggestedApps.value = contextEngine.getSuggestedApps(apps, 8, place)
            }
        }
    }

    // ── AI Briefing ────────────────────────────────────────────────────────
    fun refreshAiBriefing() {
        val ctx = getApplication<Application>()
        if (!GeminiRepository.hasApiKey(ctx)) return
        viewModelScope.launch {
            val w = _weather.value
            val nextEvent = _events.value?.firstOrNull()?.title
            val greeting = _currentContext.value?.label ?: "Ciao"
            val result = GeminiRepository.generateMorningBriefing(
                context     = ctx,
                weatherDesc = w?.description ?: "variabile",
                tempC       = w?.tempC ?: 18,
                nextEvent   = nextEvent,
                missedCalls = _missedCalls.value?.size ?: 0,
                unreadSms   = _unreadSms.value?.sumOf { it.count } ?: 0,
                greeting    = greeting
            )
            _aiBriefing.value = result?.text
        }
    }

    // ── Notification summarization ─────────────────────────────────────────
    fun refreshNotifSummary() {
        val ctx = getApplication<Application>()
        if (!GeminiRepository.hasApiKey(ctx)) {
            // Fallback: usa NotificationRanker
            val top = NotificationRanker.getRankedNotifications(ctx, 1).firstOrNull()
            _topRankedNotif.value = top
            return
        }
        viewModelScope.launch {
            val notifs = AviateNotificationService.lastNotifications
                .takeLast(5).map { it.pkg to "${it.title}: ${it.text}" }
            val summary = GeminiRepository.summarizeNotifications(ctx, notifs)
            _aiNotifSummary.value = summary
            // Anche il top ranked
            _topRankedNotif.value = NotificationRanker.getRankedNotifications(ctx, 1).firstOrNull()
        }
    }

    // ── Pattern learning ───────────────────────────────────────────────────
    fun refreshPatterns() {
        viewModelScope.launch {
            val raw = PatternLearner.getPatternsForNow(getApplication(), 3)
            val apps = _allApps.value ?: return@launch
            // Filtra pattern di app che esistono ancora e non sono state ignorate troppo
            val ctx = getApplication<Application>()
            val valid = raw.filter { p ->
                apps.any { it.packageName == p.packageName } &&
                PatternLearner.getDismissCount(ctx, p.packageName) < 5
            }
            _patterns.value = valid
        }
    }

    // ── Weekly Digest ──────────────────────────────────────────────────────
    fun refreshWeeklyDigest() {
        val ctx = getApplication<Application>()
        if (!WeeklyDigestRepository.shouldShowDigest(ctx)) return
        viewModelScope.launch {
            val nameMap = _allApps.value?.associate { it.packageName to it.appName } ?: return@launch
            val digest = WeeklyDigestRepository.getWeeklyDigest(ctx, nameMap)
            _weeklyDigest.value = digest
            if (digest != null) WeeklyDigestRepository.markDigestShown(ctx)
        }
    }

    // ── Location ───────────────────────────────────────────────────────────
    fun refreshLocation() {
        val ctx = getApplication<Application>()
        val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                 PackageManager.PERMISSION_GRANTED ||
                 ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                 PackageManager.PERMISSION_GRANTED
        if (!ok) return
        viewModelScope.launch {
            val pos = locEng.getLastKnown() ?: locEng.requestSingleUpdate()
            pos?.let {
                _currentLocation.value = it
                _currentPlace.value = locEng.classifyPlace(it)
                refreshContext()
                WeatherRepository.get(getApplication(), it.lat, it.lon)
                    ?.let { w -> _weather.value = w }
                refreshAiBriefing()
            } ?: run { _weather.value = weatherFallback() }
        }
    }

    fun refreshCalendar() {
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED) return
        viewModelScope.launch {
            _events.value = CalendarRepository.getUpcomingEvents(getApplication(), 3)
        }
    }

    fun refreshMissedCalls() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED)
                _missedCalls.value = MissedCallsRepository.getMissedCalls(getApplication())
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_SMS) ==
                PackageManager.PERMISSION_GRANTED)
                _unreadSms.value = MissedCallsRepository.getUnreadSms(getApplication())
        }
    }

    private fun connectNotificationBadges() {
        AviateNotificationService.listener = {
            _badgeCounts.postValue(AviateNotificationService.badgeCounts.toMap())
            // Aggiorna ranking notifiche ad ogni cambiamento
            viewModelScope.launch { refreshNotifSummary() }
        }
    }

    fun setHeadphones(connected: Boolean, name: String) {
        _headphones.value = connected to name
        val isCar = connected && CAR_KEYWORDS.any { name.lowercase().contains(it) }
        _isCarBluetooth.value = isCar
        if (connected) boostMediaApps(isCar)
    }

    private fun boostMediaApps(includeNav: Boolean) {
        val mediaApps = _allApps.value?.filter {
            it.category == AppCategory.MEDIA ||
            (includeNav && it.category == AppCategory.NAVIGATION)
        } ?: return
        val current = _suggestedApps.value?.toMutableList() ?: mutableListOf()
        mediaApps.take(if (includeNav) 4 else 2).reversed().forEach { app ->
            current.removeAll { it.packageName == app.packageName }
            current.add(0, app)
        }
        _suggestedApps.value = current.take(8)
    }

    fun updateMediaInfo(info: MediaInfo?) { _mediaPlaying.value = info }

    fun setManualContext(ctx: TimeContext?) {
        contextEngine.setContextOverride(ctx)
        refreshContext()
    }

    fun recordLaunch(packageName: String) {
        val place = _currentPlace.value ?: PlaceType.OTHER
        contextEngine.recordLaunch(packageName, place)
        // Notifica AppPredictionManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            aiPredictor.notifyLaunch(packageName)
    }

    fun saveHomeLocation() = viewModelScope.launch {
        _currentLocation.value?.let { locEng.saveHome(it) }
    }
    fun saveWorkLocation() = viewModelScope.launch {
        _currentLocation.value?.let { locEng.saveWork(it) }
    }
    fun hasHome() = locEng.hasHome()
    fun hasWork() = locEng.hasWork()
    fun saveGeminiKey(key: String) =
        GeminiRepository.saveApiKey(getApplication(), key)
    fun hasGeminiKey() = GeminiRepository.hasApiKey(getApplication())

    private fun weatherFallback(): WeatherData {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..10  -> WeatherData(16, "Mattina fresca", "🌤️")
            in 11..14 -> WeatherData(24, "Soleggiato",    "☀️")
            in 15..19 -> WeatherData(21, "Pomeriggio",    "⛅")
            else      -> WeatherData(15, "Serata",        "🌙")
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiPredictor.destroy()
    }
}
