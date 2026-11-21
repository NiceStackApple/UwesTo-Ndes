package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.BlockRuleEntity
import com.example.model.BlockRuleType
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainTiredAmber
import com.example.ui.theme.StrictPurple
import com.example.viewmodel.UwesToUiState
import kotlin.math.roundToInt

@Composable
fun BlocksScreen(
    uiState: UwesToUiState,
    onUpdateDailyLimit: (Int) -> Unit,
    onSetLimitEnforcement: (Boolean) -> Unit,
    onSetNearLimitWarning: (Boolean) -> Unit,
    onSetFloatingOverlayEnabled: (Boolean) -> Unit,
    onSetBreakConfig: (Boolean, Int, Int) -> Unit,
    onToggleBlockRule: (BlockRuleEntity, Boolean) -> Unit,
    onToggleBlockRuleStrict: (BlockRuleEntity, Boolean) -> Unit,
    onAddCustomBlock: (String, Int, Int, Int, Int, Boolean) -> Unit,
    onDeleteBlockRule: (BlockRuleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("blocks_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Blokir, Limit & Jeda",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kabeh fitur kendali gratis penuh! Mung ngunci feed Reels/Shorts, chat & pencarian tetep aman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Daily Reel Limit Card
            item {
                DailyLimitControlCard(
                    dailyLimit = uiState.settings.dailyLimit,
                    enforcementEnabled = uiState.settings.limitEnforcementEnabled,
                    nearWarningEnabled = uiState.settings.nearLimitWarningEnabled,
                    overlayEnabled = uiState.settings.floatingOverlayEnabled,
                    onUpdateDailyLimit = onUpdateDailyLimit,
                    onSetLimitEnforcement = onSetLimitEnforcement,
                    onSetNearLimitWarning = onSetNearLimitWarning,
                    onSetFloatingOverlayEnabled = onSetFloatingOverlayEnabled
                )
            }

            // 2. Breaks Saat Scrolling Card
            item {
                BreaksConfigCard(
                    enabled = uiState.settings.breakEnabled,
                    everyReels = uiState.settings.breakEveryReels,
                    durationSec = uiState.settings.breakDurationSeconds,
                    onSetBreakConfig = onSetBreakConfig
                )
            }

            // 3. Scheduled Blocks & Strict Mode Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Blocks Terjadwal & Mode Strict",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Kunci otomatis pas jam turu utawa jam fokus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_block_rule_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrainHealthyEmerald,
                            contentColor = Color(0xFF062014)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(uiState.blockRules, key = { it.id }) { rule ->
                BlockRuleCard(
                    rule = rule,
                    onToggleEnabled = { onToggleBlockRule(rule, it) },
                    onToggleStrict = { onToggleBlockRuleStrict(rule, it) },
                    onDelete = { onDeleteBlockRule(rule) }
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddDialog) {
        AddBlockRuleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, startH, startM, endH, endM, strict ->
                onAddCustomBlock(name, startH, startM, endH, endM, strict)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun DailyLimitControlCard(
    dailyLimit: Int,
    enforcementEnabled: Boolean,
    nearWarningEnabled: Boolean,
    overlayEnabled: Boolean,
    onUpdateDailyLimit: (Int) -> Unit,
    onSetLimitEnforcement: (Boolean) -> Unit,
    onSetNearLimitWarning: (Boolean) -> Unit,
    onSetFloatingOverlayEnabled: (Boolean) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_limit_card")
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Limit Harian Reels", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Kunci feed pas hitungan tekan $dailyLimit reels/dino.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enforcementEnabled,
                    onCheckedChange = onSetLimitEnforcement,
                    modifier = Modifier.testTag("limit_enforcement_switch")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Batas Harian", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$dailyLimit reels", style = MaterialTheme.typography.titleLarge, color = BrainHealthyEmerald)
            }

            Slider(
                value = dailyLimit.toFloat(),
                onValueChange = { onUpdateDailyLimit((it / 5f).roundToInt() * 5) },
                valueRange = 10f..300f,
                colors = SliderDefaults.colors(
                    thumbColor = BrainHealthyEmerald,
                    activeTrackColor = BrainHealthyEmerald
                ),
                modifier = Modifier.testTag("daily_limit_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 50, 100, 150).forEach { preset ->
                    FilterChip(
                        selected = dailyLimit == preset,
                        onClick = { onUpdateDailyLimit(preset) },
                        label = { Text("$preset") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Peringatan 80% Meh Entek", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Ngelingke sedurung limit entek (pas ${(dailyLimit * 0.8f).toInt()} reels).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = nearWarningEnabled, onCheckedChange = onSetNearLimitWarning)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pill Counter Melayang (Overlay)", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Tampilke angka live cilik nang nduwur IG/YT/FB/SC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = overlayEnabled, onCheckedChange = onSetFloatingOverlayEnabled)
            }
        }
    }
}

@Composable
private fun BreaksConfigCard(
    enabled: Boolean,
    everyReels: Int,
    durationSec: Int,
    onSetBreakConfig: (Boolean, Int, Int) -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrainTiredAmber.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("breaks_config_card")
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SelfImprovement,
                        contentDescription = null,
                        tint = BrainTiredAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text("Breaks Saat Scrolling", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Jeda sadar ben ora bablas nang tengah sesi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onSetBreakConfig(it, everyReels, durationSec) },
                    modifier = Modifier.testTag("breaks_enabled_switch")
                )
            }

            Text(
                text = "Pemicu tiap piro reels:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 20, 30).forEach { interval ->
                    FilterChip(
                        selected = everyReels == interval,
                        onClick = { onSetBreakConfig(enabled, interval, durationSec) },
                        label = { Text("$interval") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Text(
                text = "Durasi jeda napas:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 30).forEach { sec ->
                    FilterChip(
                        selected = durationSec == sec,
                        onClick = { onSetBreakConfig(enabled, everyReels, sec) },
                        label = { Text("${sec} dtk") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockRuleCard(
    rule: BlockRuleEntity,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleStrict: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val isCustom = rule.type == BlockRuleType.CUSTOM.name
    val accent = if (rule.isEnabled) {
        if (rule.isStrict) StrictPurple else BrainHealthyEmerald
    } else {
        MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (rule.isEnabled) 0.55f else 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("block_rule_card_${rule.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (rule.isStrict) Icons.Rounded.Shield else Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = if (rule.isEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(rule.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${rule.formattedWindow()} • IG, YT, FB, SC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCustom) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Hapus jadwal ${rule.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.testTag("block_rule_switch_${rule.id}")
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (rule.isStrict) StrictPurple.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (rule.isStrict) "Mode Strict (Anti-Ngeyel)" else "Mode Santai",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (rule.isStrict) StrictPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                TextButton(onClick = { onToggleStrict(!rule.isStrict) }) {
                    Text(
                        text = if (rule.isStrict) "Ganti Santai" else "Aktifke Strict",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AddBlockRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int, Int, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("Jam Sinau / Kerja") }
    var startHour by remember { mutableIntStateOf(9) }
    var endHour by remember { mutableIntStateOf(12) }
    var isStrict by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Jadwal Blokir") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Jeneng Jadwal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mulai jam: %02d:00".format(startHour), style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = { startHour = (startHour + 23) % 24 }) { Text("-") }
                        TextButton(onClick = { startHour = (startHour + 1) % 24 }) { Text("+") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rampung jam: %02d:00".format(endHour), style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = { endHour = (endHour + 23) % 24 }) { Text("-") }
                        TextButton(onClick = { endHour = (endHour + 1) % 24 }) { Text("+") }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifke Mode Strict", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isStrict, onCheckedChange = { isStrict = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, startHour, 0, endHour, 0, isStrict) },
                modifier = Modifier.testTag("confirm_add_block_button")
            ) {
                Text("Simpen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
