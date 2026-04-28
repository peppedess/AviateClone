package com.aviateclone.launcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.data.AppLoader
import com.aviateclone.launcher.engine.ContextEngine
import com.aviateclone.launcher.engine.TimeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    val contextEngine = ContextEngine(application)

    private val _allApps = MutableLiveData<List<AppInfo>>()
    val allApps: LiveData<List<AppInfo>> = _allApps

    private val _suggestedApps = MutableLiveData<List<AppInfo>>()
    val suggestedApps: LiveData<List<AppInfo>> = _suggestedApps

    private val _currentContext = MutableLiveData<TimeContext>()
    val currentContext: LiveData<TimeContext> = _currentContext

    private val _appsByCategory = MutableLiveData<Map<AppCategory, List<AppInfo>>>()
    val appsByCategory: LiveData<Map<AppCategory, List<AppInfo>>> = _appsByCategory

    /** true se c'è un override manuale attivo */
    private val _isManualContext = MutableLiveData<Boolean>(false)
    val isManualContext: LiveData<Boolean> = _isManualContext

    init { loadApps() }

    fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { AppLoader.loadApps(getApplication()) }
            val enriched = contextEngine.enrichWithUsageData(apps)
            _allApps.value = enriched
            refreshContext(enriched)
            buildCollections(enriched)
        }
    }

    fun refreshContext(apps: List<AppInfo> = _allApps.value ?: emptyList()) {
        _currentContext.value = contextEngine.getCurrentContext()
        _isManualContext.value = contextEngine.isManualOverride()
        _suggestedApps.value = contextEngine.getSuggestedApps(apps, 8)
    }

    /** Imposta override manuale dal dialog context picker */
    fun setManualContext(ctx: TimeContext?) {
        contextEngine.setContextOverride(ctx)
        refreshContext()
    }

    private fun buildCollections(apps: List<AppInfo>) {
        val map = apps
            .groupBy { it.category }
            .filter { it.value.isNotEmpty() }
            .toSortedMap(compareBy { it.displayName })
        _appsByCategory.value = map
    }

    fun recordLaunch(packageName: String) = contextEngine.recordLaunch(packageName)
}
