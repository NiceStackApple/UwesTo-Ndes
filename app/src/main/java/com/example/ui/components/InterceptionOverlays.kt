package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.ActiveInterception
import com.example.model.BlockRuleType
import com.example.model.BrainStage
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainLostCrimson
import com.example.ui.theme.BrainTiredAmber
import kotlinx.coroutines.delay

@Composable
fun InterceptionModalOverlay(
    interception: ActiveInterception,
    onDismiss: (wasSkippedBreak: Boolean) -> Unit,
    onEmergencyUnlock: () -> Unit,
    onDisableAllDayRule: (Long) -> Unit
) {
    if (interception is ActiveInterception.None) return

    BackHandler {
        // Require explicit action on blocker / break modal
        onDismiss(interception is ActiveInterception.BreakPause)
    }

    when (interception) {
        is ActiveInterception.BreakPause -> BreakPauseFullScreen(
            interception = interception,
            onFinish = { onDismiss(false) },
            onSkip = { onDismiss(true) }
        )
        is ActiveInterception.LimitReached -> BlockerFullScreen(
            title = "Uwes To, Ndes!",
            subtitle = "Kuota ${interception.dailyLimit} reels dino iki wis entek (${interception.todayCount}/${interception.dailyLimit}).",
            detail = "Feed ${interception.sourceApp.displayName} ${interception.sourceApp.feedName} dikunci nganti jam 00:00. Tenang, chat lan fitur liyane nang ${interception.sourceApp.displayName} tetep iso mbok bukak.",
            stage = BrainStage.fromCount(interception.todayCount),
            isStrict = true,
            allDayRuleId = null,
            onCloseFeed = { onDismiss(false) },
            onCompleteUnlockChallenge = onEmergencyUnlock,
            onDisableAllDayRule = onDisableAllDayRule
        )
        is ActiveInterception.RuleBlocked -> BlockerFullScreen(
            title = "${interception.ruleName} Aktif",
            subtitle = "Jadwal ${interception.timeWindowLabel} lagi ngunci feed.",
            detail = "Feed ${interception.sourceApp.displayName} ${interception.sourceApp.feedName} ditahan sik ben fokusmu aman. Hitunganmu saiki ${interception.todayCount}/${interception.dailyLimit} reels.",
            stage = BrainStage.fromCount(interception.todayCount),
            isStrict = interception.isStrict,
            allDayRuleId = if (interception.ruleType == BlockRuleType.ALL_DAY) interception.ruleId else null,
            onCloseFeed = { onDismiss(false) },
            onCompleteUnlockChallenge = onEmergencyUnlock,
            onDisableAllDayRule = onDisableAllDayRule
        )
        ActiveInterception.None -> Unit
    }
}

@Composable
private fun BreakPauseFullScreen(
    interception: ActiveInterception.BreakPause,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    var secondsLeft by remember(interception) { mutableIntStateOf(interception.durationSeconds) }

    LaunchedEffect(interception) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        }
    }

    val progress = 1f - (secondsLeft.toFloat() / interception.durationSeconds.coerceAtLeast(1).toFloat())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1610),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("break_pause_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BrainTiredAmber.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, BrainTiredAmber.copy(alpha = 0.5f)),
                modifier = Modifier.size(84.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.SelfImprovement,
                        contentDescription = "Jeda istirahat",
                        tint = BrainTiredAmber,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Text(
                text = "Mandek Sik, Tarik Napas!",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Kowe wis scroll ${interception.triggerCount} reels nang ${interception.sourceApp.displayName}. Ngaso sedelo, delok adoh ben matane ora sepet, Ndes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (secondsLeft > 0) "${secondsLeft}d" else "SIAP!",
                style = MaterialTheme.typography.displayLarge,
                color = if (secondsLeft > 0) BrainTiredAmber else BrainHealthyEmerald,
                modifier = Modifier.testTag("break_countdown_text")
            )

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = if (secondsLeft > 0) BrainTiredAmber else BrainHealthyEmerald,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onFinish,
                enabled = secondsLeft == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("break_continue_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrainHealthyEmerald,
                    contentColor = Color(0xFF062014)
                )
            ) {
                Text(
                    text = if (secondsLeft > 0) "Tunggu ${secondsLeft} detik maneh..." else "Wis Seger, Balik Beranda",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (secondsLeft > 0) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.testTag("break_skip_button")
                ) {
                    Text(
                        text = "Lewati Jeda (Kecatet nang riwayat)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockerFullScreen(
    title: String,
    subtitle: String,
    detail: String,
    stage: BrainStage,
    isStrict: Boolean,
    allDayRuleId: Long?,
    onCloseFeed: () -> Unit,
    onCompleteUnlockChallenge: () -> Unit,
    onDisableAllDayRule: (Long) -> Unit
) {
    var challengeStarted by remember { mutableStateOf(false) }
    var challengeSecondsLeft by remember { mutableIntStateOf(15) }

    LaunchedEffect(challengeStarted) {
        if (challengeStarted) {
            challengeSecondsLeft = 15
            while (challengeSecondsLeft > 0) {
                delay(1000L)
                challengeSecondsLeft -= 1
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrainLostCrimson.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(24.dp)
            .testTag("blocker_modal_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = BrainCrackedCoral.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, BrainCrackedCoral.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isStrict) Icons.Rounded.Shield else Icons.Rounded.Block,
                        contentDescription = null,
                        tint = BrainCrackedCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isStrict) "MODE STRICT AKTIF" else "UWESTO BLOKIR FEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrainCrackedCoral
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = stage.accentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Reset info + brain stage pill
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kondisi Otak", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stage.titleSemarang} (${stage.standardLabel})", style = MaterialTheme.typography.labelLarge, color = stage.accentColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reset Otomatis", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Jam 00:00 (Tengah Bengi)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Button(
                onClick = onCloseFeed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("blocker_accept_stop_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrainHealthyEmerald,
                    contentColor = Color(0xFF062014)
                )
            ) {
                Text("Siap, Aku Mandek Saiki!", style = MaterialTheme.typography.labelLarge)
            }

            // PRD 6.10: All-Day block can be turned off directly from lock screen even when Strict is active
            if (allDayRuleId != null) {
                OutlinedButton(
                    onClick = { onDisableAllDayRule(allDayRuleId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("blocker_disable_allday_button"),
                    border = BorderStroke(1.dp, BrainTiredAmber.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "Matikan Kunci Sak Dino soko Layar Iki",
                        color = BrainTiredAmber
                    )
                }
            }

            // Mindful Emergency Unlock Challenge (15s calm wait with honest streak consequence warning)
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Butuh Buka Darurat? (Tantangan Tenang 15 Detik)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Konsekuensi jujur: mbukak darurat bakal kecatet nang riwayat lan nek ngluwihi limit, streak dino iki iso putus, Ndes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!challengeStarted) {
                        OutlinedButton(
                            onClick = { challengeStarted = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blocker_start_unlock_challenge_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Mulai Tahan 15 Detik (Ojo Kemrungsung)")
                        }
                    } else if (challengeSecondsLeft > 0) {
                        LinearProgressIndicator(
                            progress = { (15 - challengeSecondsLeft) / 15f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = BrainTiredAmber
                        )
                        Text(
                            text = "Sabar sik, tahan ${challengeSecondsLeft} detik maneh... Yakin tenan meh lanjut scroll?",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrainTiredAmber
                        )
                    } else {
                        Button(
                            onClick = onCompleteUnlockChallenge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("blocker_confirm_unlock_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrainCrackedCoral,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Buka Sementara 10 Menit")
                        }
                    }
                }
            }
        }
    }
}
