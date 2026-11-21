package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainLostCrimson
import com.example.ui.theme.BrainTiredAmber
import com.example.ui.theme.BrainWitheredViolet
import com.example.ui.theme.FacebookAccent
import com.example.ui.theme.InstagramAccent
import com.example.ui.theme.SnapchatAccent
import com.example.ui.theme.TikTokAccent
import com.example.ui.theme.YouTubeAccent

enum class SupportedApp(
    val id: String,
    val displayName: String,
    val shortLabel: String,
    val feedName: String,
    val packages: List<String>,
    val accentColor: Color
) {
    INSTAGRAM(
        id = "instagram",
        displayName = "Instagram",
        shortLabel = "IG",
        feedName = "Reels",
        packages = listOf("com.instagram.android", "com.instagram.lite"),
        accentColor = InstagramAccent
    ),
    TIKTOK(
        id = "tiktok",
        displayName = "TikTok",
        shortLabel = "TT",
        feedName = "FYP",
        packages = listOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.zhiliaoapp.musically.go",
            "com.ss.android.ugc.trill.go"
        ),
        accentColor = TikTokAccent
    ),
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube",
        shortLabel = "YT",
        feedName = "Shorts",
        packages = listOf("com.google.android.youtube"),
        accentColor = YouTubeAccent
    ),
    FACEBOOK(
        id = "facebook",
        displayName = "Facebook",
        shortLabel = "FB",
        feedName = "Reels",
        packages = listOf("com.facebook.katana", "com.facebook.lite"),
        accentColor = FacebookAccent
    ),
    SNAPCHAT(
        id = "snapchat",
        displayName = "Snapchat",
        shortLabel = "SC",
        feedName = "Spotlight",
        packages = listOf("com.snapchat.android", "com.snapchat.lite"),
        accentColor = SnapchatAccent
    );

    companion object {
        fun fromPackageName(packageName: String): SupportedApp? {
            return entries.firstOrNull { app -> packageName in app.packages }
        }

        fun fromId(id: String): SupportedApp {
            return entries.firstOrNull { it.id == id } ?: INSTAGRAM
        }

        val allAllowedPackages: Set<String> = entries.flatMap { it.packages }.toSet()
    }
}

/**
 * Exact 5 visual brain states from PRD (thresholds: 0, 50, 100, 150, 200+).
 * Always paired with explicit Semarang Timur + Indonesian text labels for accessibility.
 */
enum class BrainStage(
    val minReels: Int,
    val badgeCode: String,
    val titleSemarang: String,
    val standardLabel: String,
    val remarkSemarang: String,
    val accentColor: Color,
    @DrawableRes val imageRes: Int?
) {
    SEGER_WARAS(
        minReels = 0,
        badgeCode = "0–49",
        titleSemarang = "Seger Waras",
        standardLabel = "Sehat",
        remarkSemarang = "Otak sik fresh banget, Ndes! Fokus aman, pertahanin terus yo.",
        accentColor = BrainHealthyEmerald,
        imageRes = R.drawable.img_brain_healthy_1795230570721
    ),
    MULAI_MUMET(
        minReels = 50,
        badgeCode = "50–99",
        titleSemarang = "Mulai Mumet",
        standardLabel = "Mulai Lelah",
        remarkSemarang = "Wis 50+ reels ik, matane mulai sepet to? Mbok ngaso sik sedelo.",
        accentColor = BrainTiredAmber,
        imageRes = R.drawable.img_brain_tired_1795230568902
    ),
    RETAK_RETAK(
        minReels = 100,
        badgeCode = "100–149",
        titleSemarang = "Rodo Ra Waras",
        standardLabel = "Retak",
        remarkSemarang = "Waduh tembus 100+ reels! Wis rodo ra waras iki, fokusmu mulai buyar, ojo ngeyel, Ndes.",
        accentColor = BrainCrackedCoral,
        imageRes = R.drawable.img_brain_cracked_1795230569029
    ),
    LAYU_KUYU(
        minReels = 150,
        badgeCode = "150–199",
        titleSemarang = "Wes Ra Waras",
        standardLabel = "Layu Abu-abu",
        remarkSemarang = "150+ reels?! Wes ra waras tenan kowe, Ndes! Uwes to mbok leren sik!",
        accentColor = BrainWitheredViolet,
        imageRes = R.drawable.img_brain_withered_1795230570532
    ),
    ILANG_BLASS(
        minReels = 200,
        badgeCode = "200+",
        titleSemarang = "Wes Edan Ki",
        standardLabel = "Hilang",
        remarkSemarang = "200+ reels bablas tenan! Wes edan ki, ndang selehke HP-ne saiki!",
        accentColor = BrainLostCrimson,
        imageRes = null // Rendered with custom animated dissolve/glitch Canvas
    );

    companion object {
        fun fromCount(reelCount: Int): BrainStage = when {
            reelCount >= 200 -> ILANG_BLASS
            reelCount >= 150 -> LAYU_KUYU
            reelCount >= 100 -> RETAK_RETAK
            reelCount >= 50 -> MULAI_MUMET
            else -> SEGER_WARAS
        }
    }
}

enum class BlockRuleType(val labelSemarang: String) {
    SCHEDULED_NIGHT("Kunci Bengi"),
    FOCUS_DURATION("Fokus Kilat"),
    ALL_DAY("Kunci Sak Dino"),
    CUSTOM("Jadwal Kustom")
}

data class PermissionHealth(
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayEnabled: Boolean = false,
    val isServiceConnected: Boolean = false,
    val lastCheckedAt: Long = System.currentTimeMillis()
) {
    val isFullyHealthy: Boolean
        get() = isAccessibilityEnabled && isOverlayEnabled
}

data class StreakSummary(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val startDate: String = "",
    val lastBreakReason: String? = null
)

sealed interface ActiveInterception {
    data object None : ActiveInterception

    data class BreakPause(
        val triggerCount: Int,
        val durationSeconds: Int,
        val sourceApp: SupportedApp
    ) : ActiveInterception

    data class LimitReached(
        val todayCount: Int,
        val dailyLimit: Int,
        val sourceApp: SupportedApp
    ) : ActiveInterception

    data class RuleBlocked(
        val ruleId: Long,
        val ruleName: String,
        val ruleType: BlockRuleType,
        val isStrict: Boolean,
        val timeWindowLabel: String,
        val sourceApp: SupportedApp,
        val todayCount: Int,
        val dailyLimit: Int
    ) : ActiveInterception
}
