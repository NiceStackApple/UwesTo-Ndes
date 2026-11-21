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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PermissionHealth
import com.example.model.SupportedApp
import com.example.service.PermissionHelper
import com.example.ui.components.BrainVisualCard
import com.example.ui.components.CounterRing
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainTiredAmber
import com.example.viewmodel.UwesToUiState

@Composable
fun HomeScreen(
    uiState: UwesToUiState,
    onSelectTestApp: (SupportedApp) -> Unit,
    onSimulateScroll: (Int) -> Unit,
    onResetToday: () -> Unit,
    onToggleTestOverlay: () -> Unit,
    onPreviewBlocker: () -> Unit,
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
                HomeHeaderRow(permissionHealth = uiState.permissionHealth)
            }

            // Honest Detection Health Banner (PRD 6.1 & 9: never silent when permissions are off)
            if (!uiState.permissionHealth.isFullyHealthy) {
                item {
                    PermissionHealthBanner(
                        health = uiState.permissionHealth,
                        onOpenAccessibility = { PermissionHelper.openAccessibilitySettings(context) },
                        onOpenOverlay = { PermissionHelper.openOverlaySettings(context) },
                        onOpenBattery = { PermissionHelper.openBatteryOptimizationSettings(context) }
                    )
                }
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

            // Per-App Breakdown Card (Instagram, YouTube, Facebook, Snapchat)
            item {
                PerAppBreakdownCard(
                    instagramCount = uiState.todayLog.instagramCount,
                    youtubeCount = uiState.todayLog.youtubeCount,
                    facebookCount = uiState.todayLog.facebookCount,
                    snapchatCount = uiState.todayLog.snapchatCount,
                    totalCount = uiState.todayTotal
                )
            }

            // Honest Built-In Uji Coba / Simulator Panel
            item {
                SimulationPanelCard(
                    selectedApp = uiState.selectedTestApp,
                    isTestOverlayVisible = uiState.isTestOverlayVisible,
                    onSelectApp = onSelectTestApp,
                    onSimulateScroll = onSimulateScroll,
                    onResetToday = onResetToday,
                    onToggleTestOverlay = onToggleTestOverlay,
                    onPreviewBlocker = onPreviewBlocker
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HomeHeaderRow(permissionHealth: PermissionHealth) {
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

        val healthy = permissionHealth.isFullyHealthy
        val badgeColor = if (healthy) BrainHealthyEmerald else BrainTiredAmber
        Surface(
            shape = RoundedCornerShape(50),
            color = badgeColor.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
            modifier = Modifier.testTag("detection_health_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (healthy) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                    contentDescription = "Status deteksi otomatis",
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (healthy) "Deteksi Aktif" else "Izin Belum Full",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor
                )
            }
        }
    }
}

@Composable
private fun PermissionHealthBanner(
    health: PermissionHealth,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenBattery: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = BrainTiredAmber.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, BrainTiredAmber.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_health_banner")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = BrainTiredAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Deteksi Otomatis Lagi Turu, Ndes!",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrainTiredAmber
                )
            }

            val missingList = buildList {
                if (!health.isAccessibilityEnabled) add("Accessibility Service")
                if (!health.isOverlayEnabled) add("Izin Overlay")
            }.joinToString(" & ")

            Text(
                text = "$missingList durung urip. Aktifke ben UwesTo ngitung otomatis nang IG, YouTube, FB, lan Snapchat, utawa jajal nganggo Mode Uji Coba nang ngisor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!health.isAccessibilityEnabled) {
                    Button(
                        onClick = onOpenAccessibility,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fix_accessibility_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrainTiredAmber,
                            contentColor = Color(0xFF211400)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Uripke Accessibility", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (!health.isOverlayEnabled) {
                    OutlinedButton(
                        onClick = onOpenOverlay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fix_overlay_button"),
                        border = BorderStroke(1.dp, BrainTiredAmber.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Izin Overlay", style = MaterialTheme.typography.labelSmall, color = BrainTiredAmber)
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenBattery,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tips Anti-Mati: Kecualikan Penghemat Baterai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PerAppBreakdownCard(
    instagramCount: Int,
    youtubeCount: Int,
    facebookCount: Int,
    snapchatCount: Int,
    totalCount: Int
) {
    val rows = listOf(
        SupportedApp.INSTAGRAM to instagramCount,
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
                    text = "Rincian Per Aplikasi",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total: $totalCount",
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
private fun SimulationPanelCard(
    selectedApp: SupportedApp,
    isTestOverlayVisible: Boolean,
    onSelectApp: (SupportedApp) -> Unit,
    onSimulateScroll: (Int) -> Unit,
    onResetToday: () -> Unit,
    onToggleTestOverlay: () -> Unit,
    onPreviewBlocker: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simulation_panel_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Science,
                    contentDescription = null,
                    tint = BrainHealthyEmerald,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Mode Uji Coba (Simulasi Scroll)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jujur simulasi lokal kanggo ngetes counter, tahap otak 50–200+, lan blokir tanpa mbukak IG/YT sik.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Source App Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SupportedApp.entries.forEach { app ->
                    FilterChip(
                        selected = selectedApp == app,
                        onClick = { onSelectApp(app) },
                        label = { Text(app.shortLabel) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sim_app_${app.id}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = app.accentColor.copy(alpha = 0.22f),
                            selectedLabelColor = app.accentColor
                        )
                    )
                }
            }

            // Primary +1 Reel and quick jumps (+10, +50)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onSimulateScroll(1) },
                    modifier = Modifier
                        .weight(1.6f)
                        .height(50.dp)
                        .testTag("sim_plus_one_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrainHealthyEmerald,
                        contentColor = Color(0xFF062014)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("1 Reel", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { onSimulateScroll(10) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sim_plus_ten_button")
                ) {
                    Text("+10", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { onSimulateScroll(50) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("sim_plus_fifty_button")
                ) {
                    Text("+50", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Secondary test tools: Floating Pill, Blocker Preview, Reset Daily
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleTestOverlay,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_toggle_overlay_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isTestOverlayVisible) "Tutup Pill" else "Tes Pill",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                OutlinedButton(
                    onClick = onPreviewBlocker,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_preview_blocker_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Block,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrainCrackedCoral
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tes Blokir", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = onResetToday,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sim_reset_today_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
