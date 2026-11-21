package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.SupportedApp
import com.example.service.PermissionHelper
import com.example.ui.components.BrainVisualCard
import com.example.ui.components.CounterRing
import com.example.ui.theme.BrainHealthyEmerald
import com.example.viewmodel.UwesToUiState

@Composable
fun HomeScreen(
    uiState: UwesToUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                HomeHeaderRow()
            }

            // Giant Counter Ring + Streak badge
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CounterRing(
                        todayCount = uiState.todayTotal,
                        dailyLimit = uiState.settings.dailyLimit,
                        streakDays = uiState.streakSummary.currentStreak,
                        stage = uiState.brainStage
                    )
                }
            }

            // 5-Stage Brain State Visual Card
            item {
                BrainVisualCard(
                    stage = uiState.brainStage,
                    todayCount = uiState.todayTotal
                )
            }

            // Per-App Breakdown Card (Instagram, TikTok, YouTube, Facebook, Snapchat — unified total)
            item {
                PerAppBreakdownCard(
                    instagramCount = uiState.todayLog.instagramCount,
                    tiktokCount = uiState.todayLog.tiktokCount,
                    youtubeCount = uiState.todayLog.youtubeCount,
                    facebookCount = uiState.todayLog.facebookCount,
                    snapchatCount = uiState.todayLog.snapchatCount,
                    totalCount = uiState.todayTotal
                )
            }

            // Live Detection Info & Battery Protection Card
            item {
                LiveMonitoringInfoCard(
                    onOpenBatterySettings = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HomeHeaderRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "UwesTo",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Hitung scroll-mu live, Ndes!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = BrainHealthyEmerald.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, BrainHealthyEmerald.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("detection_health_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Deteksi otomatis aktif",
                    tint = BrainHealthyEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Deteksi Aktif",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrainHealthyEmerald
                )
            }
        }
    }
}

@Composable
private fun PerAppBreakdownCard(
    instagramCount: Int,
    tiktokCount: Int,
    youtubeCount: Int,
    facebookCount: Int,
    snapchatCount: Int,
    totalCount: Int
) {
    val rows = listOf(
        SupportedApp.INSTAGRAM to instagramCount,
        SupportedApp.TIKTOK to tiktokCount,
        SupportedApp.YOUTUBE to youtubeCount,
        SupportedApp.FACEBOOK to facebookCount,
        SupportedApp.SNAPCHAT to snapchatCount
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("per_app_breakdown_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gabungan Per Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Gabungan: $totalCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            rows.forEach { (app, count) ->
                val share = if (totalCount > 0) count.toFloat() / totalCount.toFloat() else 0f
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(app.accentColor)
                            )
                            Text(
                                text = "${app.displayName} ${app.feedName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "$count reels",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LinearProgressIndicator(
                        progress = { share },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = app.accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveMonitoringInfoCard(
    onOpenBatterySettings: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrainHealthyEmerald.copy(alpha = 0.30f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_monitoring_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Sensors,
                    contentDescription = null,
                    tint = BrainHealthyEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Pemantauan Live Siap Mlaku",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Bukak Instagram Reels, TikTok, YouTube Shorts, Facebook Reels, utawa Snapchat Spotlight koyo biasa. Kabeh jumlah scroll bakal digabung dadi siji total harian otomatis (chat & komentar aman ora keitung).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cegah Mati Otomatis (Penghemat Baterai)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
