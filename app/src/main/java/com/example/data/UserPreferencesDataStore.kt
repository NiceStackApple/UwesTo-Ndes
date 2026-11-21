package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.uwestoDataStore: DataStore<Preferences> by preferencesDataStore(name = "uwesto_prefs")

data class UserSettings(
    val dailyLimit: Int = 50,
    val limitEnforcementEnabled: Boolean = true,
    val nearLimitWarningEnabled: Boolean = true,
    val breakEnabled: Boolean = true,
    val breakEveryReels: Int = 20,
    val breakDurationSeconds: Int = 15,
    val floatingOverlayEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val temporaryUnlockUntilMs: Long = 0L,
    val streakStartDate: String = ""
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val DAILY_LIMIT = intPreferencesKey("daily_limit")
        val LIMIT_ENFORCEMENT = booleanPreferencesKey("limit_enforcement")
        val NEAR_LIMIT_WARNING = booleanPreferencesKey("near_limit_warning")
        val BREAK_ENABLED = booleanPreferencesKey("break_enabled")
        val BREAK_EVERY_REELS = intPreferencesKey("break_every_reels")
        val BREAK_DURATION_SEC = intPreferencesKey("break_duration_sec")
        val FLOATING_OVERLAY_ENABLED = booleanPreferencesKey("floating_overlay_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val TEMP_UNLOCK_UNTIL_MS = longPreferencesKey("temp_unlock_until_ms")
        val STREAK_START_DATE = stringPreferencesKey("streak_start_date")
    }

    val settingsFlow: Flow<UserSettings> = context.uwestoDataStore.data.map { prefs ->
        UserSettings(
            dailyLimit = prefs[Keys.DAILY_LIMIT] ?: 50,
            limitEnforcementEnabled = prefs[Keys.LIMIT_ENFORCEMENT] ?: true,
            nearLimitWarningEnabled = prefs[Keys.NEAR_LIMIT_WARNING] ?: true,
            breakEnabled = prefs[Keys.BREAK_ENABLED] ?: true,
            breakEveryReels = prefs[Keys.BREAK_EVERY_REELS] ?: 20,
            breakDurationSeconds = prefs[Keys.BREAK_DURATION_SEC] ?: 15,
            floatingOverlayEnabled = prefs[Keys.FLOATING_OVERLAY_ENABLED] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            temporaryUnlockUntilMs = prefs[Keys.TEMP_UNLOCK_UNTIL_MS] ?: 0L,
            streakStartDate = prefs[Keys.STREAK_START_DATE] ?: ""
        )
    }

    suspend fun setDailyLimit(limit: Int) {
        context.uwestoDataStore.edit { it[Keys.DAILY_LIMIT] = limit.coerceIn(10, 500) }
    }

    suspend fun setLimitEnforcementEnabled(enabled: Boolean) {
        context.uwestoDataStore.edit { it[Keys.LIMIT_ENFORCEMENT] = enabled }
    }

    suspend fun setNearLimitWarningEnabled(enabled: Boolean) {
        context.uwestoDataStore.edit { it[Keys.NEAR_LIMIT_WARNING] = enabled }
    }

    suspend fun setBreakConfig(enabled: Boolean, everyReels: Int, durationSec: Int) {
        context.uwestoDataStore.edit {
            it[Keys.BREAK_ENABLED] = enabled
            it[Keys.BREAK_EVERY_REELS] = everyReels.coerceIn(5, 100)
            it[Keys.BREAK_DURATION_SEC] = durationSec.coerceIn(5, 60)
        }
    }

    suspend fun setFloatingOverlayEnabled(enabled: Boolean) {
        context.uwestoDataStore.edit { it[Keys.FLOATING_OVERLAY_ENABLED] = enabled }
    }

    suspend fun completeOnboarding(initialLimit: Int, todayDateKey: String) {
        context.uwestoDataStore.edit {
            it[Keys.DAILY_LIMIT] = initialLimit.coerceIn(10, 500)
            it[Keys.ONBOARDING_COMPLETED] = true
            if (it[Keys.STREAK_START_DATE].isNullOrEmpty()) {
                it[Keys.STREAK_START_DATE] = todayDateKey
            }
        }
    }

    suspend fun setTemporaryUnlockUntil(untilMs: Long) {
        context.uwestoDataStore.edit { it[Keys.TEMP_UNLOCK_UNTIL_MS] = untilMs }
    }
}
