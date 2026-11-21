package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_scroll_logs")
data class DailyScrollLogEntity(
    @PrimaryKey val dateKey: String, // YYYY-MM-DD in device local timezone
    val instagramCount: Int = 0,
    val tiktokCount: Int = 0,
    val youtubeCount: Int = 0,
    val facebookCount: Int = 0,
    val snapchatCount: Int = 0,
    val dailyLimitSnapshot: Int = 50,
    val breaksSkipped: Int = 0,
    val emergencyUnlocks: Int = 0,
    val permissionInterrupted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCount: Int
        get() = instagramCount + tiktokCount + youtubeCount + facebookCount + snapchatCount

    val isWithinLimit: Boolean
        get() = totalCount <= dailyLimitSnapshot
}

@Entity(tableName = "block_rules")
data class BlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // BlockRuleType.name
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val targetAppsCsv: String = "instagram,tiktok,youtube,facebook,snapchat",
    val isEnabled: Boolean = false,
    val isStrict: Boolean = false,
    val focusExpiresAtMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedWindow(): String {
        return when (type) {
            "ALL_DAY" -> "00:00 – 23:59 (Sak Dino)"
            "FOCUS_DURATION" -> "Fokus 1 Jam"
            else -> "%02d:%02d – %02d:%02d".format(startHour, startMinute, endHour, endMinute)
        }
    }
}

@Entity(tableName = "battle_rivals")
data class BattleRivalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val neighborhood: String, // e.g., "Pedurungan", "Tlogosari"
    val avatarInitials: String,
    val accentHex: Long,
    val isSimulated: Boolean = true,
    val simulatedPacePerHour: Int = 5,
    val manualCountToday: Int = 0,
    val lastSyncedDate: String = ""
)

@Entity(tableName = "permission_events")
data class PermissionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // ACCESSIBILITY_ON, ACCESSIBILITY_OFF, OVERLAY_ON, OVERLAY_OFF, SERVICE_STOPPED
    val detail: String
)
