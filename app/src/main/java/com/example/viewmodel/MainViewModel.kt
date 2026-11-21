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
import com.example.model.BrainStage
import com.example.model.PermissionHealth
import com.example.model.StreakSummary
import com.example.service.PermissionHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    val uiState: StateFlow<UwesToUiState> = combine(
        baseDataFlow,
        repository.permissionEventsFlow,
        _permissionHealth,
        _activeInterception,
        repository.isAccessibilityServiceConnected
    ) { base, events, health, interception, serviceConnected ->
        val updatedHealth = health.copy(isServiceConnected = serviceConnected)
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
            permissionEvents = events,
            permissionHealth = updatedHealth,
            activeInterception = interception,
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
        if (!_permissionHealth.value.isFullyHealthy) {
            _snackbarMessages.tryEmit("Aktifke izin Accessibility lan Overlay sik baru iso mlebu, Ndes!")
            return
        }
        viewModelScope.launch {
            repository.completeOnboarding(initialLimit)
            _snackbarMessages.tryEmit("Sip Ndes! Izin lengkap & limit diset $initialLimit reels. Gas jaga otakmu!")
        }
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

    private data class BaseSnapshot(
        val settings: UserSettings,
        val todayLog: DailyScrollLogEntity,
        val recentLogs: List<DailyScrollLogEntity>,
        val rules: List<BlockRuleEntity>,
        val rivals: List<BattleRivalEntity>
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
