package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BrainStage
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BrainVisualCard(
    stage: BrainStage,
    todayCount: Int,
    modifier: Modifier = Modifier
) {
    val accent by animateColorAsState(
        targetValue = stage.accentColor,
        animationSpec = tween(420),
        label = "brain_accent"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("brain_visual_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        radius = 520f
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Artwork centerpiece (bespoke generated image for 0/50/100/150, animated dissolve canvas for 200+)
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF12161F)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = stage,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
                            label = "brain_art_transition"
                        ) { targetStage ->
                            val resId = targetStage.imageRes
                            if (resId != null) {
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = "Visual otak tahap ${targetStage.titleSemarang} (${targetStage.standardLabel})",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                VanishedBrainGlitchCanvas(
                                    accent = targetStage.accentColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Explicit text labels & Semarang Timur commentary (colorblind-accessible)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accent.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${stage.badgeCode} REELS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "(${stage.standardLabel})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stage.titleSemarang,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("brain_stage_title")
                        )

                        Text(
                            text = stage.remarkSemarang,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 5-Stage Threshold Stepper (0 -> 50 -> 100 -> 150 -> 200+)
                BrainStageThresholdRow(currentStage = stage, todayCount = todayCount)

                // Clear non-medical behavioral disclaimer per PRD 6.3 & 12
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.non_medical_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BrainStageThresholdRow(
    currentStage: BrainStage,
    todayCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BrainStage.entries.forEach { item ->
            val reached = todayCount >= item.minReels
            val isCurrent = item == currentStage
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCurrent) 7.dp else 5.dp)
                        .clip(CircleShape)
                        .background(
                            if (reached) item.accentColor
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
                Text(
                    text = if (item.minReels == 200) "200+" else "${item.minReels}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) item.accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Custom animated Canvas for Stage 5 (200+ Ilang Blass / Hilang):
 * Dashed ghostly brain silhouette with crimson static scanlines & drifting pixel embers.
 */
@Composable
private fun VanishedBrainGlitchCanvas(
    accent: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vanished_brain")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glitch_phase"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = size.minDimension * 0.30f

            // Subtle crimson void glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.6f
                ),
                radius = r * 1.6f,
                center = Offset(cx, cy)
            )

            // Dashed vanishing brain hemispheres outline
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), phase * 36f)
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = r * 0.82f,
                center = Offset(cx - r * 0.35f, cy),
                style = Stroke(width = 3.5f, pathEffect = dashEffect)
            )
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = r * 0.82f,
                center = Offset(cx + r * 0.35f, cy),
                style = Stroke(width = 3.5f, pathEffect = dashEffect)
            )

            // Glitch scanlines
            val scanY = size.height * phase
            drawLine(
                color = accent.copy(alpha = 0.55f),
                start = Offset(size.width * 0.12f, scanY),
                end = Offset(size.width * 0.88f, scanY),
                strokeWidth = 2.5f
            )

            // Drifting dissolution embers
            for (i in 0 until 10) {
                val angle = (i * 36f) * (Math.PI / 180.0)
                val drift = ((phase + i * 0.13f) % 1f)
                val px = cx + (cos(angle) * r * (0.7f + drift * 0.7f)).toFloat()
                val py = cy + (sin(angle) * r * (0.6f + drift * 0.5f)).toFloat() - drift * 18f
                drawCircle(
                    color = accent.copy(alpha = (1f - drift) * 0.9f),
                    radius = 2.5f + (i % 3),
                    center = Offset(px, py)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "404",
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "EDAN KI",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
