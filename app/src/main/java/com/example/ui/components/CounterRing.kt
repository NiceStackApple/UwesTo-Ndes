package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BrainStage
import com.example.ui.theme.StreakFlameOrange

@Composable
fun CounterRing(
    todayCount: Int,
    dailyLimit: Int,
    streakDays: Int,
    stage: BrainStage,
    modifier: Modifier = Modifier
) {
    val rawProgress = if (dailyLimit > 0) (todayCount.toFloat() / dailyLimit.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ring_progress"
    )
    val ringColor by animateColorAsState(
        targetValue = stage.accentColor,
        animationSpec = tween(400),
        label = "ring_color"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val remaining = (dailyLimit - todayCount).coerceAtLeast(0)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(236.dp)
                .testTag("home_counter_ring"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 18.dp.toPx()
                val inset = strokePx / 2f + 6.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                val topLeft = Offset(inset, inset)

                // Soft ambient radial glow behind ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ringColor.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        radius = size.minDimension * 0.55f
                    ),
                    radius = size.minDimension * 0.55f
                )

                // Background ring track
                drawArc(
                    color = trackColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Animated progress arc
                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                ringColor.copy(alpha = 0.65f),
                                ringColor,
                                ringColor
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "REELS DINO IKI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = todayCount.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("today_reel_count_text")
                )
                Text(
                    text = if (todayCount >= dailyLimit) {
                        "Limit $dailyLimit wis entek!"
                    } else {
                        "Sisa $remaining soko $dailyLimit"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = ringColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Streak flame pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = StreakFlameOrange.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, StreakFlameOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("streak_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Streak aktif",
                            tint = StreakFlameOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Streak $streakDays dino",
                            style = MaterialTheme.typography.labelSmall,
                            color = StreakFlameOrange
                        )
                    }
                }
            }
        }
    }
}
