package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.UwesToApplication
import com.example.data.BattleRivalEntity
import com.example.data.BlockRuleEntity
import com.example.data.DailyScrollLogEntity
import com.example.data.PermissionEventEntity
import com.example.data.ReelRepository
import com.example.data.UserSettings
import com.example.model.ActiveInterception
import com.example.model.BlockRuleType
import com.example.model.BrainStage
import com.example.model.PermissionHealth
import com.example.model.StreakSummary
import com.example.model.SupportedApp
import com.example.service.FloatingOverlayManager
import com.example.service.PermissionHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UwesToUiState(
    val settings: UserSettings = UserSettings(),
    val todayLog: DailyScrollLogEntity = DailyScrollLogEntity(dateKey = ReelRepository.todayDateKey()),
    val recentLogs: List<DailyScrollLogEntity> = emptyList(),
    val blockRules: List<BlockRuleEntity> = emptyList(),
    val battleRivals: List<BattleRivalEntity> = emptyList(),
    val permissionEvents: List<PermissionEventEntity> = emptyList(),
    val permissionHealth: PermissionHealth = PermissionHealth(),
    val selectedTestApp: SupportedApp = SupportedApp.INSTAGRAM,
    val isTestOverlayVisible: Boolean = false,
    val activeInterception: ActiveInterception = ActiveInterception.None,
    val streakSummary: StreakSummary = StreakSummary(),
    val isLoaded: Boolean = false
) {
    val todayTotal: Int
        get() = todayLog.totalCount

    val brainStage: BrainStage
        get() = BrainStage.fromCount(todayTotal)

    val remainingQuota: Int
        get() = (settings.dailyLimit - todayTotal).coerceAtLeast(0)

    val isOverLimit: Boolean
        get() = todayTotal >= settings.dailyLimit
}

class MainViewModel(
    application: Application,
    private val repository: ReelRepository
) : AndroidViewModel(application) {

    private val overlayManager = FloatingOverlayManager(application.applicationContext)

    private val _selectedTestApp = MutableStateFlow(SupportedApp.INSTAGRAM)
    private val _isTestOverlayVisible = MutableStateFlow(false)
    private val _activeInterception = MutableStateFlow<ActiveInterception>(ActiveInterception.None)
    private val _permissionHealth = MutableStateFlow(
        PermissionHelper.checkHealth(
            context = application.applicationContext,
            isServiceConnected = repository.isAccessibilityServiceConnected.value
        )
    )

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private val baseDataFlow = combine(
        repository.settingsFlow,
        repository.todayLogFlow,
        repository.recentLogsFlow,
        repository.blockRulesFlow,
        repository.battleRivalsFlow
    ) { settings, todayLog, recentLogs, rules, rivals ->
        BaseSnapshot(settings, todayLog, recentLogs, rules, rivals)
    }

    private val transientFlow = combine(
        repository.permissionEventsFlow,
        _permissionHealth,
        _selectedTestApp,
        _isTestOverlayVisible,
        _activeInterception
    ) { events, health, testApp, overlayShown, interception ->
        TransientSnapshot(events, health, testApp, overlayShown, interception)
    }

    val uiState: StateFlow<UwesToUiState> = combine(
        baseDataFlow,
        transientFlow,
        repository.isAccessibilityServiceConnected
    ) { base, transient, serviceConnected ->
        val updatedHealth = transient.health.copy(isServiceConnected = serviceConnected)
        val streak = repository.computeStreakSummary(
            logs = base.recentLogs,
            streakStartDate = base.settings.streakStartDate
        )
        UwesToUiState(
            settings = base.settings,
            todayLog = base.todayLog,
            recentLogs = base.recentLogs,
            blockRules = base.rules,
            battleRivals = base.rivals,
            permissionEvents = transient.events,
            permissionHealth = updatedHealth,
            selectedTestApp = transient.testApp,
            isTestOverlayVisible = transient.overlayShown,
            activeInterception = transient.interception,
            streakSummary = streak,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UwesToUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeededDefaults()
            refreshPermissionHealth(logTransitions = false)
        }
    }

    fun refreshPermissionHealth(logTransitions: Boolean = true) {
        val ctx = getApplication<Application>().applicationContext
        val previous = _permissionHealth.value
        val current = PermissionHelper.checkHealth(
            context = ctx,
            isServiceConnected = repository.isAccessibilityServiceConnected.value
        )
        _permissionHealth.value = current
        repository.refreshDateRollover()

        if (logTransitions) {
            viewModelScope.launch {
                if (previous.isAccessibilityEnabled && !current.isAccessibilityEnabled) {
                    repository.logPermissionEvent(
                        eventType = "ACCESSIBILITY_OFF",
                        detail = "Izin Accessibility mati — deteksi live mandek sik.",
                        markDayInterrupted = true
                    )
                } else if (!previous.isAccessibilityEnabled && current.isAccessibilityEnabled) {
                    repository.logPermissionEvent(
                        eventType = "ACCESSIBILITY_ON",
                        detail = "Izin Accessibility aktif, siap ngitung scroll live!"
                    )
                }
                if (previous.isOverlayEnabled && !current.isOverlayEnabled) {
                    repository.logPermissionEvent(
                        eventType = "OVERLAY_OFF",
                        detail = "Izin Overlay dipateni — pill angka ora iso tampil nang nduwur app."
                    )
                } else if (!previous.isOverlayEnabled && current.isOverlayEnabled) {
                    repository.logPermissionEvent(
                        eventType = "OVERLAY_ON",
                        detail = "Izin Overlay aktif, counter live siap melayang."
                    )
                }
            }
        }
    }

    fun completeOnboarding(initialLimit: Int) {
        viewModelScope.launch {
            repository.completeOnboarding(initialLimit)
            _snackbarMessages.tryEmit("Sip Ndes! Limit harian diset $initialLimit reels. Gas jaga otakmu!")
        }
    }

    fun selectTestApp(app: SupportedApp) {
        _selectedTestApp.value = app
    }

    fun simulateScroll(delta: Int = 1) {
        viewModelScope.launch {
            val app = _selectedTestApp.value
            val result = repository.incrementScroll(app = app, delta = delta, triggerInterceptions = true)
            if (_isTestOverlayVisible.value) {
                overlayManager.showOrUpdateCounterPill(
                    todayCount = result.newTotal,
                    dailyLimit = result.dailyLimit,
                    sourceApp = app
                )
            }
            if (result.nearLimitWarning) {
                _snackbarMessages.tryEmit(
                    "Awas Ndes! Wis ${result.newTotal}/${result.dailyLimit} reels, meh entek kuotane!"
                )
            }
            if (result.interception !is ActiveInterception.None) {
                _activeInterception.value = result.interception
            }
        }
    }

    fun resetTodayCount() {
        viewModelScope.launch {
            repository.resetTodayCount()
            _activeInterception.value = ActiveInterception.None
            if (_isTestOverlayVisible.value) {
                overlayManager.showOrUpdateCounterPill(
                    todayCount = 0,
                    dailyLimit = uiState.value.settings.dailyLimit,
                    sourceApp = _selectedTestApp.value
                )
            }
            _snackbarMessages.tryEmit("Hitungan dino iki wis direset dadi 0. Otak seger maneh, Ndes!")
        }
    }

    fun toggleTestOverlayPill() {
        val ctx = getApplication<Application>().applicationContext
        if (!PermissionHelper.isOverlayPermissionGranted(ctx)) {
            _snackbarMessages.tryEmit("Aktifke izin 'Tampil di atas aplikasi lain' sik ben pill iso melayang, Ndes.")
            PermissionHelper.openOverlaySettings(ctx)
            return
        }
        val next = !_isTestOverlayVisible.value
        _isTestOverlayVisible.value = next
        if (next) {
            overlayManager.showOrUpdateCounterPill(
                todayCount = uiState.value.todayTotal,
                dailyLimit = uiState.value.settings.dailyLimit,
                sourceApp = _selectedTestApp.value
            )
            _snackbarMessages.tryEmit("Pill overlay uji coba aktif! Iso mbok seret nang pinggir layar.")
        } else {
            overlayManager.hideCounterPill()
            _snackbarMessages.tryEmit("Pill overlay uji coba ditutup.")
        }
    }

    fun previewBlockerModal() {
        val state = uiState.value
        val activeAllDay = state.blockRules.firstOrNull {
            it.type == BlockRuleType.ALL_DAY.name && it.isEnabled
        }
        _activeInterception.value = if (activeAllDay != null) {
            ActiveInterception.RuleBlocked(
                ruleId = activeAllDay.id,
                ruleName = activeAllDay.name,
                ruleType = BlockRuleType.ALL_DAY,
                isStrict = activeAllDay.isStrict,
                timeWindowLabel = activeAllDay.formattedWindow(),
                sourceApp = _selectedTestApp.value,
                todayCount = state.todayTotal,
                dailyLimit = state.settings.dailyLimit
            )
        } else {
            ActiveInterception.LimitReached(
                todayCount = state.todayTotal.coerceAtLeast(state.settings.dailyLimit),
                dailyLimit = state.settings.dailyLimit,
                sourceApp = _selectedTestApp.value
            )
        }
    }

    fun previewBreakModal() {
        val state = uiState.value
        _activeInterception.value = ActiveInterception.BreakPause(
            triggerCount = state.todayTotal.coerceAtLeast(state.settings.breakEveryReels),
            durationSeconds = state.settings.breakDurationSeconds,
            sourceApp = _selectedTestApp.value
        )
    }

    fun dismissInterception(wasSkippedBreak: Boolean = false) {
        _activeInterception.value = ActiveInterception.None
        if (wasSkippedBreak) {
            viewModelScope.launch {
                repository.recordBreakSkipped()
                _snackbarMessages.tryEmit("Break dilewati — kecatet nang riwayat dino iki yo, Ndes.")
            }
        }
    }

    fun completeEmergencyUnlock() {
        viewModelScope.launch {
            repository.recordEmergencyUnlock(durationMinutes = 10)
            _activeInterception.value = ActiveInterception.None
            _snackbarMessages.tryEmit("Kebuka sementara 10 menit. Elingo streak-mu, ojo bablas maneh!")
        }
    }

    fun disableAllDayFromLockScreen(ruleId: Long) {
        viewModelScope.launch {
            repository.disableAllDayRuleFromLockScreen(ruleId)
            _activeInterception.value = ActiveInterception.None
            _snackbarMessages.tryEmit("Kunci Sak Dino dipateni soko layar blokir.")
        }
    }

    // Settings & Blocks
    fun updateDailyLimit(limit: Int) {
        viewModelScope.launch { repository.updateDailyLimit(limit) }
    }

    fun setLimitEnforcement(enabled: Boolean) {
        viewModelScope.launch { repository.setLimitEnforcementEnabled(enabled) }
    }

    fun setNearLimitWarning(enabled: Boolean) {
        viewModelScope.launch { repository.setNearLimitWarningEnabled(enabled) }
    }

    fun setBreakConfig(enabled: Boolean, everyReels: Int, durationSec: Int) {
        viewModelScope.launch { repository.setBreakConfig(enabled, everyReels, durationSec) }
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setFloatingOverlayEnabled(enabled) }
    }

    fun toggleBlockRule(rule: BlockRuleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleBlockRule(rule, enabled)
            val status = if (enabled) "diuripke" else "dipateni"
            _snackbarMessages.tryEmit("${rule.name} wis $status.")
        }
    }

    fun toggleBlockRuleStrict(rule: BlockRuleEntity, isStrict: Boolean) {
        viewModelScope.launch { repository.toggleBlockRuleStrict(rule, isStrict) }
    }

    fun addCustomBlockRule(
        name: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isStrict: Boolean
    ) {
        if (name.isBlank()) {
            _snackbarMessages.tryEmit("Jeneng jadwal ojo kosong, Ndes.")
            return
        }
        viewModelScope.launch {
            repository.addCustomBlockRule(name, startHour, startMinute, endHour, endMinute, isStrict)
            _snackbarMessages.tryEmit("Jadwal blokir '$name' wis ditambahke!")
        }
    }

    fun deleteBlockRule(rule: BlockRuleEntity) {
        viewModelScope.launch {
            repository.deleteBlockRule(rule.id)
            _snackbarMessages.tryEmit("Jadwal '${rule.name}' dihapus.")
        }
    }

    // Scroll Battles
    fun addBattleRival(name: String, neighborhood: String, initialCount: Int, isSimulated: Boolean) {
        if (name.isBlank()) {
            _snackbarMessages.tryEmit("Isi jeneng kancamu sik, Ndes.")
            return
        }
        viewModelScope.launch {
            repository.addBattleRival(name, neighborhood, initialCount, isSimulated)
            _snackbarMessages.tryEmit("$name mlebu daftar Scroll Battle!")
        }
    }

    fun updateRivalScore(rival: BattleRivalEntity, newCount: Int) {
        viewModelScope.launch {
            repository.updateRivalScore(rival, newCount)
            _snackbarMessages.tryEmit("Skor ${rival.name} diupdate dadi $newCount reels.")
        }
    }

    fun simulateRivalsActivity() {
        viewModelScope.launch {
            repository.simulateRivalsTick()
            _snackbarMessages.tryEmit("Rival lokal Semarang lagi gerak — cek siapa paling sedikit scroll-nya!")
        }
    }

    fun deleteBattleRival(rival: BattleRivalEntity) {
        viewModelScope.launch {
            repository.deleteBattleRival(rival.id)
            _snackbarMessages.tryEmit("${rival.name} dihapus soko daftar battle.")
        }
    }

    fun buildBattleShareSummary(): String {
        val state = uiState.value
        val myCount = state.todayTotal
        val stage = state.brainStage
        val wins = state.battleRivals.count { myCount < it.manualCountToday }
        val totalRivals = state.battleRivals.size
        return buildString {
            appendLine("UwesTo — Laporan Scroll Battle Dino Iki")
            appendLine("Skorku: $myCount reels (${stage.titleSemarang} / ${stage.standardLabel})")
            appendLine("Limit harian: ${state.settings.dailyLimit} reels | Streak: ${state.streakSummary.currentStreak} dino")
            if (totalRivals > 0) {
                appendLine("Menang lawan $wins soko $totalRivals kanca (skor paling sithik sing menang!).")
            }
            append("Uwes to, Ndes! Ayo adu paling sithik scroll-ne.")
        }
    }

    override fun onCleared() {
        overlayManager.releaseAll()
        super.onCleared()
    }

    private data class BaseSnapshot(
        val settings: UserSettings,
        val todayLog: DailyScrollLogEntity,
        val recentLogs: List<DailyScrollLogEntity>,
        val rules: List<BlockRuleEntity>,
        val rivals: List<BattleRivalEntity>
    )

    private data class TransientSnapshot(
        val events: List<PermissionEventEntity>,
        val health: PermissionHealth,
        val testApp: SupportedApp,
        val overlayShown: Boolean,
        val interception: ActiveInterception
    )

    companion object {
        fun provideFactory(application: UwesToApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(application, application.repository) as T
                }
            }
        }
    }
}
