package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BrainStage
import com.example.model.PermissionHealth
import com.example.service.PermissionHelper
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainTiredAmber
import com.example.ui.theme.InfoCyan
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    permissionHealth: PermissionHealth,
    onFinish: (initialLimit: Int) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var selectedLimit by remember { mutableIntStateOf(50) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrainHealthyEmerald.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top step indicator (no skip button — user must grant permissions to enter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { idx ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= step) BrainHealthyEmerald
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = if (idx == step) 14.dp else 6.dp)
                        )
                    }
                }
                Text(
                    text = "Langkah ${step + 1}/3",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Step content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (step) {
                    0 -> OnboardingStepIntro()
                    1 -> OnboardingStepLimit(
                        selectedLimit = selectedLimit,
                        onLimitChange = { selectedLimit = it }
                    )
                    else -> MandatoryPermissionChecklist(
                        title = "Wajib Aktifke Izin Sik, Ndes",
                        subtitle = "Supoyo UwesTo iso ngitung gabungan scroll asli nang Instagram, TikTok, YouTube Shorts, Facebook, lan Snapchat, aktifke loro izin iki sik baru iso mlebu aplikasi.",
                        health = permissionHealth,
                        onOpenAppInfo = { PermissionHelper.openAppInfoSettings(context) },
                        onOpenAccessibility = { PermissionHelper.openAccessibilitySettings(context) },
                        onOpenOverlay = { PermissionHelper.openOverlaySettings(context) }
                    )
                }
            }

            // Bottom action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (step < 2) {
                    Button(
                        onClick = { step += 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_next_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrainHealthyEmerald,
                            contentColor = Color(0xFF062014)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_next),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Button(
                        onClick = { onFinish(selectedLimit) },
                        enabled = permissionHealth.isFullyHealthy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_finish_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrainHealthyEmerald,
                            contentColor = Color(0xFF062014)
                        )
                    ) {
                        Icon(
                            imageVector = if (permissionHealth.isFullyHealthy) Icons.Rounded.PlayArrow else Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (permissionHealth.isFullyHealthy) {
                                stringResource(R.string.onboarding_start)
                            } else {
                                stringResource(R.string.onboarding_locked_cta)
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepIntro() {
    Image(
        painter = painterResource(id = R.drawable.img_brain_healthy_1795230570721),
        contentDescription = "Ilustrasi otak sehat UwesTo",
        modifier = Modifier
            .size(144.dp)
            .clip(RoundedCornerShape(30.dp)),
        contentScale = ContentScale.Crop
    )

    Text(
        text = "UwesTo, Ndes!",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Masalahmu dudu jam screen time, tapi jumlah reels sing rak kerasa mbok scroll. Niat 5 menit, ngerti-ngerti wis 300 reels!",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "5 Tahap Kondisi Otakmu",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            BrainStage.entries.forEach { stage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(stage.accentColor)
                        )
                        Text(
                            text = "${stage.titleSemarang} (${stage.standardLabel})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${stage.badgeCode} reels",
                        style = MaterialTheme.typography.labelMedium,
                        color = stage.accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepLimit(
    selectedLimit: Int,
    onLimitChange: (Int) -> Unit
) {
    val stagePreview = BrainStage.fromCount(selectedLimit)

    Text(
        text = "Setel Limit Harianmu",
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Text(
        text = "Kowe pengen dibatesi piro reels sedino? Mengko nek wis mentok, UwesTo bantu ngelingke lan ngunci feed-e (chat tetep aman!).",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, stagePreview.accentColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$selectedLimit",
                style = MaterialTheme.typography.displayLarge,
                color = stagePreview.accentColor,
                modifier = Modifier.testTag("onboarding_limit_value")
            )
            Text(
                text = "reels / dino  •  Ambang: ${stagePreview.titleSemarang}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = selectedLimit.toFloat(),
                onValueChange = { onLimitChange((it / 5f).roundToInt() * 5) },
                valueRange = 20f..250f,
                colors = SliderDefaults.colors(
                    thumbColor = stagePreview.accentColor,
                    activeTrackColor = stagePreview.accentColor
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 50, 100, 150).forEach { preset ->
                    FilterChip(
                        selected = selectedLimit == preset,
                        onClick = { onLimitChange(preset) },
                        label = { Text("$preset") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MandatoryPermissionChecklist(
    title: String,
    subtitle: String,
    health: PermissionHealth,
    onOpenAppInfo: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    // Privacy Guarantee Card
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = BrainHealthyEmerald.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, BrainHealthyEmerald.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Jaminan privasi",
                tint = BrainHealthyEmerald,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Janji Privasi UwesTo (100% Lokal)",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrainHealthyEmerald
                )
                Text(
                    text = "• Mung aktif nang IG, TikTok, YouTube, FB, lan Snapchat.\n• ORA tau maca chat, sandi, OTP, isi video, utawa app bank.\n• Data 100% lokal nang HP-mu & ora dikirim nang endi-endi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Android 13+ Sideloaded APK "Controlled by Restricted Setting" Helper Card
    if (!health.isAccessibilityEnabled) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = InfoCyan.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, InfoCyan.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("restricted_setting_guide_card")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = InfoCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Kena 'App was denied access' / Setelan Terbatas?",
                        style = MaterialTheme.typography.titleSmall,
                        color = InfoCyan
                    )
                }
                Text(
                    text = "Khusus APK sing diinstal langsung (dudu soko Play Store), Android 13+ otomatis ngunci Accessibility.\nCarane mbukak (10 detik):\n1. Pencet tombol 'Buka Info Aplikasi' nang ngisor.\n2. Pencet titik telu (⋮) nang pojok kanan nduwur (utawa gulir ngisor).\n3. Pilih 'Izinkan setelan terbatas / Allow restricted settings'.\n4. Balik mrene lan uripke Accessibility UwesTo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_app_info_button"),
                    border = BorderStroke(1.dp, InfoCyan.copy(alpha = 0.65f))
                ) {
                    Text("Buka Info Aplikasi (Izinkan Setelan Terbatas)", color = InfoCyan)
                }
            }
        }
    }

    PermissionSetupCard(
        icon = Icons.Rounded.AccessibilityNew,
        title = "1. Accessibility Service (Wajib)",
        description = "Ngitung scroll feed Reels/Shorts secara presisi (chat & komentar ora keitung).",
        isGranted = health.isAccessibilityEnabled,
        buttonText = "Bukak Pengaturan Accessibility",
        testTag = "onboarding_accessibility_button",
        onClick = onOpenAccessibility
    )

    PermissionSetupCard(
        icon = Icons.Rounded.Layers,
        title = "2. Tampil di Atas Aplikasi Lain (Wajib)",
        description = "Nampilke pill angka live cilik sing iso digeser nang pinggir layar.",
        isGranted = health.isOverlayEnabled,
        buttonText = "Aktifke Izin Overlay",
        testTag = "onboarding_overlay_button",
        onClick = onOpenOverlay
    )
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonText: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isGranted) BrainHealthyEmerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Rounded.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) BrainHealthyEmerald else BrainTiredAmber,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!isGranted) {
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrainTiredAmber,
                        contentColor = Color(0xFF211400)
                    )
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text(
                    text = "Wis aktif, mantep Ndes!",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrainHealthyEmerald
                )
            }
        }
    }
}
