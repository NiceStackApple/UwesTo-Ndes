package com.example.data

import com.example.model.ActiveInterception
import com.example.model.BlockRuleType
import com.example.model.StreakSummary
import com.example.model.SupportedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class IncrementResult(
    val newTotal: Int,
    val dailyLimit: Int,
    val nearLimitWarning: Boolean,
    val interception: ActiveInterception
)

class ReelRepository(
    private val database: UwesToDatabase,
    private val prefs: UserPreferencesDataStore
) {
    private val scrollDao = database.dailyScrollDao()
    private val blockRuleDao = database.blockRuleDao()
    private val battleRivalDao = database.battleRivalDao()
    private val permissionEventDao = database.permissionEventDao()

    private val writeMutex = Mutex()
    private val _currentDateKey = MutableStateFlow(todayDateKey())
    val currentDateKey: StateFlow<String> = _currentDateKey.asStateFlow()

    private val _isAccessibilityServiceConnected = MutableStateFlow(false)
    val isAccessibilityServiceConnected: StateFlow<Boolean> = _isAccessibilityServiceConnected.asStateFlow()

    val settingsFlow: Flow<UserSettings> = prefs.settingsFlow

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayLogFlow: Flow<DailyScrollLogEntity> = _currentDateKey.flatMapLatest { key ->
        scrollDao.observeDay(key).map { existing ->
            existing ?: DailyScrollLogEntity(dateKey = key)
        }
    }

    val recentLogsFlow: Flow<List<DailyScrollLogEntity>> = scrollDao.observeRecentDays(14)
    val blockRulesFlow: Flow<List<BlockRuleEntity>> = blockRuleDao.observeAllRules()
    val battleRivalsFlow: Flow<List<BattleRivalEntity>> = battleRivalDao.observeAllRivals()
    val permissionEventsFlow: Flow<List<PermissionEventEntity>> = permissionEventDao.observeRecentEvents()

    fun setAccessibilityServiceConnected(connected: Boolean) {
        _isAccessibilityServiceConnected.value = connected
    }

    fun refreshDateRollover(): String {
        val nowKey = todayDateKey()
        if (_currentDateKey.value != nowKey) {
            _currentDateKey.value = nowKey
        }
        return nowKey
    }

    suspend fun ensureSeededDefaults() {
        writeMutex.withLock {
            val today = refreshDateRollover()
            val settings = prefs.settingsFlow.first()

            if (scrollDao.getDay(today) == null) {
                scrollDao.upsert(
                    DailyScrollLogEntity(
                        dateKey = today,
                        dailyLimitSnapshot = settings.dailyLimit
                    )
                )
            }

            if (blockRuleDao.getAllRules().isEmpty()) {
                blockRuleDao.insert(
                    BlockRuleEntity(
                        name = "Kunci Bengi (Jam Turu)",
                        type = BlockRuleType.SCHEDULED_NIGHT.name,
                        startHour = 22,
                        startMinute = 0,
                        endHour = 6,
                        endMinute = 0,
                        isEnabled = true,
                        isStrict = false
                    )
                )
                blockRuleDao.insert(
                    BlockRuleEntity(
                        name = "Fokus 1 Jam",
                        type = BlockRuleType.FOCUS_DURATION.name,
                        startHour = 0,
                        startMinute = 0,
                        endHour = 1,
                        endMinute = 0,
                        isEnabled = false,
                        isStrict = true
                    )
                )
                blockRuleDao.insert(
                    BlockRuleEntity(
                        name = "Kunci Sak Dino (All-Day)",
                        type = BlockRuleType.ALL_DAY.name,
                        startHour = 0,
                        startMinute = 0,
                        endHour = 23,
                        endMinute = 59,
                        isEnabled = false,
                        isStrict = true
                    )
                )
            }

            if (battleRivalDao.getAllRivals().isEmpty()) {
                battleRivalDao.insert(
                    BattleRivalEntity(
                        name = "Mas Bagas",
                        neighborhood = "Pedurungan",
                        avatarInitials = "MB",
                        accentHex = 0xFF38BDF8,
                        isSimulated = true,
                        simulatedPacePerHour = 4,
                        manualCountToday = 36,
                        lastSyncedDate = today
                    )
                )
                battleRivalDao.insert(
                    BattleRivalEntity(
                        name = "Mbak Rina",
                        neighborhood = "Tlogosari",
                        avatarInitials = "MR",
                        accentHex = 0xFF2EE59D,
                        isSimulated = true,
                        simulatedPacePerHour = 2,
                        manualCountToday = 22,
                        lastSyncedDate = today
                    )
                )
                battleRivalDao.insert(
                    BattleRivalEntity(
                        name = "Mas Dimas",
                        neighborhood = "Majapahit",
                        avatarInitials = "MD",
                        accentHex = 0xFFFF6B4A,
                        isSimulated = true,
                        simulatedPacePerHour = 8,
                        manualCountToday = 93,
                        lastSyncedDate = today
                    )
                )
            }
        }
    }

    suspend fun incrementScroll(
        app: SupportedApp,
        delta: Int = 1,
        triggerInterceptions: Boolean = true
    ): IncrementResult = writeMutex.withLock {
        val today = refreshDateRollover()
        val settings = prefs.settingsFlow.first()
        val existing = scrollDao.getDay(today)
            ?: DailyScrollLogEntity(dateKey = today, dailyLimitSnapshot = settings.dailyLimit)

        val previousTotal = existing.totalCount
        val updated = when (app) {
            SupportedApp.INSTAGRAM -> existing.copy(instagramCount = existing.instagramCount + delta)
            SupportedApp.YOUTUBE -> existing.copy(youtubeCount = existing.youtubeCount + delta)
            SupportedApp.FACEBOOK -> existing.copy(facebookCount = existing.facebookCount + delta)
            SupportedApp.SNAPCHAT -> existing.copy(snapchatCount = existing.snapchatCount + delta)
        }.copy(
            dailyLimitSnapshot = settings.dailyLimit,
            updatedAt = System.currentTimeMillis()
        )
        scrollDao.upsert(updated)

        val newTotal = updated.totalCount
        val nowMs = System.currentTimeMillis()
        val isTempUnlocked = nowMs < settings.temporaryUnlockUntilMs

        val warningThreshold = (settings.dailyLimit * 0.8f).toInt().coerceAtLeast(1)
        val nearLimitWarning = settings.nearLimitWarningEnabled &&
            settings.limitEnforcementEnabled &&
            previousTotal < warningThreshold &&
            newTotal >= warningThreshold &&
            newTotal < settings.dailyLimit

        val interception: ActiveInterception = if (!triggerInterceptions) {
            ActiveInterception.None
        } else {
            evaluateActiveInterceptionInternal(
                app = app,
                previousTotal = previousTotal,
                newTotal = newTotal,
                settings = settings,
                isTempUnlocked = isTempUnlocked,
                nowMs = nowMs
            )
        }

        IncrementResult(
            newTotal = newTotal,
            dailyLimit = settings.dailyLimit,
            nearLimitWarning = nearLimitWarning,
            interception = interception
        )
    }

    suspend fun evaluateCurrentFeedBlock(app: SupportedApp): ActiveInterception {
        val today = refreshDateRollover()
        val settings = prefs.settingsFlow.first()
        val log = scrollDao.getDay(today)
            ?: DailyScrollLogEntity(dateKey = today, dailyLimitSnapshot = settings.dailyLimit)
        val nowMs = System.currentTimeMillis()
        val isTempUnlocked = nowMs < settings.temporaryUnlockUntilMs
        return evaluateActiveInterceptionInternal(
            app = app,
            previousTotal = log.totalCount,
            newTotal = log.totalCount,
            settings = settings,
            isTempUnlocked = isTempUnlocked,
            nowMs = nowMs,
            checkBreakTrigger = false
        )
    }

    private suspend fun evaluateActiveInterceptionInternal(
        app: SupportedApp,
        previousTotal: Int,
        newTotal: Int,
        settings: UserSettings,
        isTempUnlocked: Boolean,
        nowMs: Long,
        checkBreakTrigger: Boolean = true
    ): ActiveInterception {
        if (!isTempUnlocked) {
            // 1. Check active scheduled / all-day / focus Blocks
            val activeRule = findActiveBlockRule(app, nowMs)
            if (activeRule != null) {
                val parsedType = runCatching { BlockRuleType.valueOf(activeRule.type) }
                    .getOrDefault(BlockRuleType.CUSTOM)
                return ActiveInterception.RuleBlocked(
                    ruleId = activeRule.id,
                    ruleName = activeRule.name,
                    ruleType = parsedType,
                    isStrict = activeRule.isStrict,
                    timeWindowLabel = activeRule.formattedWindow(),
                    sourceApp = app,
                    todayCount = newTotal,
                    dailyLimit = settings.dailyLimit
                )
            }

            // 2. Check Daily Reel Limit
            if (settings.limitEnforcementEnabled && newTotal >= settings.dailyLimit) {
                return ActiveInterception.LimitReached(
                    todayCount = newTotal,
                    dailyLimit = settings.dailyLimit,
                    sourceApp = app
                )
            }
        }

        // 3. Check Breaks saat scrolling
        if (checkBreakTrigger && settings.breakEnabled && settings.breakEveryReels > 0) {
            val prevBucket = previousTotal / settings.breakEveryReels
            val newBucket = newTotal / settings.breakEveryReels
            if (newTotal > 0 && newBucket > prevBucket) {
                return ActiveInterception.BreakPause(
                    triggerCount = newTotal,
                    durationSeconds = settings.breakDurationSeconds,
                    sourceApp = app
                )
            }
        }

        return ActiveInterception.None
    }

    private suspend fun findActiveBlockRule(app: SupportedApp, nowMs: Long): BlockRuleEntity? {
        val rules = blockRuleDao.getAllRules().filter { it.isEnabled }
        if (rules.isEmpty()) return null

        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        for (rule in rules) {
            val targetedApps = rule.targetAppsCsv.split(",").map { it.trim() }
            if (app.id !in targetedApps) continue

            when (rule.type) {
                BlockRuleType.ALL_DAY.name -> return rule
                BlockRuleType.FOCUS_DURATION.name -> {
                    if (rule.focusExpiresAtMs > nowMs) {
                        return rule
                    } else if (rule.focusExpiresAtMs > 0L) {
                        // Auto-disable expired focus block
                        blockRuleDao.update(rule.copy(isEnabled = false, focusExpiresAtMs = 0L))
                    }
                }
                else -> {
                    val startMin = rule.startHour * 60 + rule.startMinute
                    val endMin = rule.endHour * 60 + rule.endMinute
                    val inWindow = if (startMin <= endMin) {
                        currentMinutes in startMin until endMin
                    } else {
                        // Overnight schedule e.g. 22:00 to 06:00
                        currentMinutes >= startMin || currentMinutes < endMin
                    }
                    if (inWindow) return rule
                }
            }
        }
        return null
    }

    suspend fun resetTodayCount() = writeMutex.withLock {
        val today = refreshDateRollover()
        val settings = prefs.settingsFlow.first()
        scrollDao.upsert(
            DailyScrollLogEntity(
                dateKey = today,
                dailyLimitSnapshot = settings.dailyLimit,
                updatedAt = System.currentTimeMillis()
            )
        )
        prefs.setTemporaryUnlockUntil(0L)
    }

    suspend fun recordEmergencyUnlock(durationMinutes: Int = 10) = writeMutex.withLock {
        val today = refreshDateRollover()
        val settings = prefs.settingsFlow.first()
        val current = scrollDao.getDay(today)
            ?: DailyScrollLogEntity(dateKey = today, dailyLimitSnapshot = settings.dailyLimit)
        scrollDao.upsert(
            current.copy(
                emergencyUnlocks = current.emergencyUnlocks + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
        prefs.setTemporaryUnlockUntil(System.currentTimeMillis() + durationMinutes * 60_000L)
    }

    suspend fun recordBreakSkipped() = writeMutex.withLock {
        val today = refreshDateRollover()
        val settings = prefs.settingsFlow.first()
        val current = scrollDao.getDay(today)
            ?: DailyScrollLogEntity(dateKey = today, dailyLimitSnapshot = settings.dailyLimit)
        scrollDao.upsert(
            current.copy(
                breaksSkipped = current.breaksSkipped + 1,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateDailyLimit(limit: Int) {
        prefs.setDailyLimit(limit)
        val today = refreshDateRollover()
        val existing = scrollDao.getDay(today) ?: DailyScrollLogEntity(dateKey = today)
        scrollDao.upsert(existing.copy(dailyLimitSnapshot = limit, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setLimitEnforcementEnabled(enabled: Boolean) = prefs.setLimitEnforcementEnabled(enabled)
    suspend fun setNearLimitWarningEnabled(enabled: Boolean) = prefs.setNearLimitWarningEnabled(enabled)
    suspend fun setBreakConfig(enabled: Boolean, everyReels: Int, durationSec: Int) =
        prefs.setBreakConfig(enabled, everyReels, durationSec)

    suspend fun setFloatingOverlayEnabled(enabled: Boolean) = prefs.setFloatingOverlayEnabled(enabled)

    suspend fun completeOnboarding(initialLimit: Int) {
        val today = refreshDateRollover()
        prefs.completeOnboarding(initialLimit, today)
        val existing = scrollDao.getDay(today) ?: DailyScrollLogEntity(dateKey = today)
        scrollDao.upsert(existing.copy(dailyLimitSnapshot = initialLimit, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleBlockRule(rule: BlockRuleEntity, enabled: Boolean) {
        val updated = if (rule.type == BlockRuleType.FOCUS_DURATION.name && enabled) {
            rule.copy(
                isEnabled = true,
                focusExpiresAtMs = System.currentTimeMillis() + 60 * 60_000L
            )
        } else {
            rule.copy(
                isEnabled = enabled,
                focusExpiresAtMs = if (enabled) rule.focusExpiresAtMs else 0L
            )
        }
        blockRuleDao.update(updated)
    }

    suspend fun toggleBlockRuleStrict(rule: BlockRuleEntity, isStrict: Boolean) {
        blockRuleDao.update(rule.copy(isStrict = isStrict))
    }

    suspend fun disableAllDayRuleFromLockScreen(ruleId: Long) {
        val rule = blockRuleDao.getAllRules().firstOrNull { it.id == ruleId } ?: return
        blockRuleDao.update(rule.copy(isEnabled = false))
    }

    suspend fun addCustomBlockRule(
        name: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        isStrict: Boolean
    ) {
        blockRuleDao.insert(
            BlockRuleEntity(
                name = name.trim(),
                type = BlockRuleType.CUSTOM.name,
                startHour = startHour,
                startMinute = startMinute,
                endHour = endHour,
                endMinute = endMinute,
                isEnabled = true,
                isStrict = isStrict
            )
        )
    }

    suspend fun deleteBlockRule(id: Long) = blockRuleDao.deleteById(id)

    suspend fun addBattleRival(
        name: String,
        neighborhood: String,
        initialCount: Int,
        isSimulated: Boolean
    ) {
        val initials = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "TM" }

        val palette = listOf(0xFF2EE59D, 0xFF38BDF8, 0xFFFFB547, 0xFFFF6B4A, 0xFFA78BFA)
        val color = palette[(name.hashCode() and 0x7FFFFFFF) % palette.size]

        battleRivalDao.insert(
            BattleRivalEntity(
                name = name.trim(),
                neighborhood = neighborhood.trim().ifEmpty { "Semarang Timur" },
                avatarInitials = initials,
                accentHex = color,
                isSimulated = isSimulated,
                simulatedPacePerHour = if (isSimulated) ((initialCount / 8).coerceIn(2, 10)) else 0,
                manualCountToday = initialCount.coerceAtLeast(0),
                lastSyncedDate = todayDateKey()
            )
        )
    }

    suspend fun updateRivalScore(rival: BattleRivalEntity, newCount: Int) {
        battleRivalDao.update(
            rival.copy(
                manualCountToday = newCount.coerceAtLeast(0),
                lastSyncedDate = todayDateKey()
            )
        )
    }

    suspend fun simulateRivalsTick() {
        val rivals = battleRivalDao.getAllRivals()
        val today = todayDateKey()
        for (rival in rivals) {
            if (rival.isSimulated) {
                val bump = (1..4).random()
                battleRivalDao.update(
                    rival.copy(
                        manualCountToday = rival.manualCountToday + bump,
                        lastSyncedDate = today
                    )
                )
            }
        }
    }

    suspend fun deleteBattleRival(id: Long) = battleRivalDao.deleteById(id)

    suspend fun logPermissionEvent(eventType: String, detail: String, markDayInterrupted: Boolean = false) {
        permissionEventDao.insert(
            PermissionEventEntity(
                eventType = eventType,
                detail = detail
            )
        )
        if (markDayInterrupted) {
            val today = refreshDateRollover()
            val existing = scrollDao.getDay(today)
            if (existing != null && !existing.permissionInterrupted) {
                scrollDao.upsert(existing.copy(permissionInterrupted = true))
            }
        }
    }

    fun computeStreakSummary(
        logs: List<DailyScrollLogEntity>,
        streakStartDate: String
    ): StreakSummary {
        if (logs.isEmpty()) {
            return StreakSummary(
                currentStreak = 1,
                bestStreak = 1,
                startDate = streakStartDate.ifEmpty { todayDateKey() }
            )
        }
        val byDate = logs.associateBy { it.dateKey }
        var currentStreak = 0
        var lastBreakReason: String? = null

        val cal = Calendar.getInstance()
        for (daysBack in 0 until 30) {
            val key = formatDateKey(cal.time)
            val entry = byDate[key]
            if (entry == null) {
                if (daysBack == 0) {
                    currentStreak += 1
                } else {
                    break
                }
            } else if (entry.isWithinLimit) {
                currentStreak += 1
            } else {
                lastBreakReason = if (entry.permissionInterrupted) {
                    "Izin Accessibility sempat mati pas $key, dudu salahmu tok, Ndes."
                } else {
                    "Kelewat limit harian (${entry.totalCount}/${entry.dailyLimitSnapshot} reels) pas $key."
                }
                break
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val sortedAsc = logs.sortedBy { it.dateKey }
        var best = currentStreak
        var running = 0
        for (log in sortedAsc) {
            if (log.isWithinLimit) {
                running += 1
                if (running > best) best = running
            } else {
                running = 0
            }
        }

        return StreakSummary(
            currentStreak = currentStreak,
            bestStreak = best.coerceAtLeast(currentStreak),
            startDate = streakStartDate.ifEmpty { sortedAsc.firstOrNull()?.dateKey ?: todayDateKey() },
            lastBreakReason = lastBreakReason
        )
    }

    companion object {
        fun todayDateKey(): String = formatDateKey(Date())

        fun formatDateKey(date: Date): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        }

        fun shortDayLabel(dateKey: String): String {
            return runCatching {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val parsed = parser.parse(dateKey) ?: return dateKey.takeLast(2)
                SimpleDateFormat("EEE", Locale("id", "ID")).format(parsed)
            }.getOrDefault(dateKey.takeLast(2))
        }
    }
}
