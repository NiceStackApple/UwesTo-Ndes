package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DailyScrollLogEntity
import com.example.data.PermissionEventEntity
import com.example.data.ReelRepository
import com.example.model.BrainStage
import com.example.model.StreakSummary
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainTiredAmber
import com.example.ui.theme.StreakFlameOrange
import com.example.viewmodel.UwesToUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayChartPoint(
    val dateKey: String,
    val shortDay: String,
    val count: Int,
    val limit: Int,
    val isToday: Boolean
)

@Composable
fun StatsScreen(
    uiState: UwesToUiState,
    modifier: Modifier = Modifier
) {
    val chartPoints = remember(uiState.recentLogs, uiState.todayLog, uiState.settings.dailyLimit) {
        buildLast7DaysPoints(uiState.recentLogs, uiState.todayLog, uiState.settings.dailyLimit)
    }

    val counts = chartPoints.map { it.count }
    val weeklyTotal = counts.sum()
    val weeklyAvg = if (counts.isNotEmpty()) weeklyTotal / counts.size else 0
    val sortedCounts = counts.sorted()
    val weeklyMedian = if (sortedCounts.isNotEmpty()) sortedCounts[sortedCounts.size / 2] else 0

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("stats_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Statistik & Riwayat",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cek tren 7 dino terakhir ben ora mung perasaan tok, Ndes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Key KPI Grid (2x2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        title = "Median 7 Dino",
                        value = "$weeklyMedian",
                        subtitle = "reels / dino",
                        accent = BrainHealthyEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Rata-Rata",
                        value = "$weeklyAvg",
                        subtitle = "Total: $weeklyTotal",
                        accent = BrainTiredAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 7-Day Bar Chart with dashed daily limit line
            item {
                WeeklyBarChartCard(
                    points = chartPoints,
                    dailyLimit = uiState.settings.dailyLimit
                )
            }

            // Streak & Honesty Summary Card
            item {
                StreakDetailCard(
                    streak = uiState.streakSummary,
                    todayBreaksSkipped = uiState.todayLog.breaksSkipped,
                    todayEmergencyUnlocks = uiState.todayLog.emergencyUnlocks
                )
            }

            // Permission Health Local Event Log (PRD 9 transparency)
            item {
                PermissionEventsCard(events = uiState.permissionEvents)
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineLarge, color = accent)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyBarChartCard(
    points: List<DayChartPoint>,
    dailyLimit: Int
) {
    val maxVal = (points.maxOfOrNull { it.count } ?: 0).coerceAtLeast(dailyLimit).coerceAtLeast(50)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_bar_chart_card")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = BrainHealthyEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Grafik 7 Dino Terakhir",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Limit: $dailyLimit",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrainCrackedCoral
                )
            }

            // Custom Canvas bar chart with dashed limit guide
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .padding(top = 8.dp)
            ) {
                val barCount = points.size.coerceAtLeast(1)
                val slotWidth = size.width / barCount
                val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                val chartHeight = size.height

                // Dashed limit line
                val limitRatio = (dailyLimit.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val limitY = chartHeight * (1f - limitRatio)
                drawLine(
                    color = BrainCrackedCoral.copy(alpha = 0.7f),
                    start = Offset(0f, limitY),
                    end = Offset(size.width, limitY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                )

                points.forEachIndexed { index, point ->
                    val ratio = (point.count.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val barHeight = (chartHeight * ratio).coerceAtLeast(6.dp.toPx())
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val top = chartHeight - barHeight
                    val barColor = if (point.count == 0) {
                        Color(0xFF283040)
                    } else {
                        BrainStage.fromCount(point.count).accentColor
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }

            // X-axis day & count labels
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEach { point ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${point.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point.isToday) BrainHealthyEmerald else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = point.shortDay,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (point.isToday) BrainHealthyEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakDetailCard(
    streak: StreakSummary,
    todayBreaksSkipped: Int,
    todayEmergencyUnlocks: Int
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, StreakFlameOrange.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = StreakFlameOrange.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Ikon Streak",
                            tint = StreakFlameOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Streak Patuh: ${streak.currentStreak} Dino",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rekor paling apik: ${streak.bestStreak} dino • Mulai: ${streak.startDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (streak.lastBreakReason != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "Catatan jujur: ${streak.lastBreakReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrainTiredAmber,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Jeda dilewati dino iki: $todayBreaksSkipped",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Buka darurat: $todayEmergencyUnlocks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun PermissionEventsCard(events: List<PermissionEventEntity>) {
    val formatter = remember { SimpleDateFormat("dd MMM HH:mm", Locale("id", "ID")) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
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
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Log Kesehatan Izin (Lokal)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (events.isEmpty()) {
                Text(
                    text = "Durung ono catetan izin mati/urip. Nek izin Accessibility sempat dicabut sistem, bakal kecatet jujur nang kene.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                events.take(5).forEach { event ->
                    val isWarning = event.eventType.contains("OFF") || event.eventType.contains("STOPPED")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isWarning) BrainCrackedCoral else BrainHealthyEmerald)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = formatter.format(Date(event.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildLast7DaysPoints(
    recentLogs: List<DailyScrollLogEntity>,
    todayLog: DailyScrollLogEntity,
    dailyLimit: Int
): List<DayChartPoint> {
    val byDate = recentLogs.associateBy { it.dateKey }.toMutableMap()
    byDate[todayLog.dateKey] = todayLog
    val todayKey = ReelRepository.todayDateKey()

    val result = mutableListOf<DayChartPoint>()
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -6)
    for (i in 0 until 7) {
        val key = ReelRepository.formatDateKey(cal.time)
        val log = byDate[key]
        result.add(
            DayChartPoint(
                dateKey = key,
                shortDay = if (key == todayKey) "Saiki" else ReelRepository.shortDayLabel(key),
                count = log?.totalCount ?: 0,
                limit = log?.dailyLimitSnapshot ?: dailyLimit,
                isToday = key == todayKey
            )
        )
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}
