package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.BattleRivalEntity
import com.example.ui.theme.BrainCrackedCoral
import com.example.ui.theme.BrainHealthyEmerald
import com.example.ui.theme.BrainTiredAmber
import com.example.viewmodel.UwesToUiState

@Composable
fun BattlesScreen(
    uiState: UwesToUiState,
    onAddRival: (String, String, Int, Boolean) -> Unit,
    onUpdateRivalScore: (BattleRivalEntity, Int) -> Unit,
    onSimulateRivals: () -> Unit,
    onDeleteRival: (BattleRivalEntity) -> Unit,
    onBuildShareSummary: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRival by remember { mutableStateOf<BattleRivalEntity?>(null) }

    val myCount = uiState.todayTotal
    val featuredRival = uiState.battleRivals.firstOrNull()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .testTag("battles_screen_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Scroll Battles (Lokal)",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aturan main: jumlah reels PALING SITHIK sing menang dino iki! Ojo kebalik, Ndes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Featured Side-by-Side Duel Card (synced with exact same Home daily count)
            if (featuredRival != null) {
                item {
                    FeaturedDuelCard(
                        myCount = myCount,
                        rival = featuredRival,
                        onShare = {
                            val shareText = onBuildShareSummary()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Hasil Scroll Battle"))
                        }
                    )
                }
            }

            // Quick Actions: Add Local Friend + Simulate Rival Activity
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("battle_add_friend_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrainHealthyEmerald,
                            contentColor = Color(0xFF062014)
                        )
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Kanca", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onSimulateRivals,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("battle_simulate_tick_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulasi Rival", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            item {
                Text(
                    text = "Daftar Rival Semarang (${uiState.battleRivals.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (uiState.battleRivals.isEmpty()) {
                item {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = BrainTiredAmber,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Durung ono lawan battle, Ndes!",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Tambahke jeneng kancamu ben iso adu sapa sing paling sithik scroll-ne dino iki.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.battleRivals, key = { it.id }) { rival ->
                    BattleRivalRowCard(
                        myCount = myCount,
                        rival = rival,
                        onEditScore = { editingRival = rival },
                        onDelete = { onDeleteRival(rival) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showAddDialog) {
        AddBattleRivalDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, area, initialCount, isSimulated ->
                onAddRival(name, area, initialCount, isSimulated)
                showAddDialog = false
            }
        )
    }

    editingRival?.let { rival ->
        EditRivalScoreDialog(
            rival = rival,
            onDismiss = { editingRival = null },
            onConfirm = { newScore ->
                onUpdateRivalScore(rival, newScore)
                editingRival = null
            }
        )
    }
}

@Composable
private fun FeaturedDuelCard(
    myCount: Int,
    rival: BattleRivalEntity,
    onShare: () -> Unit
) {
    val (statusText, statusColor) = when {
        myCount < rival.manualCountToday -> "Kowe Menang Dino Iki, Ndes!" to BrainHealthyEmerald
        myCount == rival.manualCountToday -> "Skor Seri! Ojo Kalah Disik." to BrainTiredAmber
        else -> "Waduh Kalah, Uwes To!" to BrainCrackedCoral
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_duel_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = statusColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(statusText, style = MaterialTheme.typography.labelMedium, color = statusColor)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kowe", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$myCount",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (myCount <= rival.manualCountToday) BrainHealthyEmerald else BrainCrackedCoral
                    )
                    Text("reels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = "VS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(rival.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${rival.manualCountToday}",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(rival.neighborhood, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedButton(
                onClick = onShare,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("battle_share_button")
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bagikan Skor Battle Dino Iki")
            }
        }
    }
}

@Composable
private fun BattleRivalRowCard(
    myCount: Int,
    rival: BattleRivalEntity,
    onEditScore: () -> Unit,
    onDelete: () -> Unit
) {
    val winning = myCount < rival.manualCountToday
    val tied = myCount == rival.manualCountToday
    val resultLabel = when {
        winning -> "Unggul ${rival.manualCountToday - myCount} reels"
        tied -> "Seri"
        else -> "Kalah ${myCount - rival.manualCountToday} reels"
    }
    val resultColor = when {
        winning -> BrainHealthyEmerald
        tied -> BrainTiredAmber
        else -> BrainCrackedCoral
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rival_card_${rival.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(rival.accentHex).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rival.avatarInitials,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(rival.accentHex)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(rival.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${rival.neighborhood} • ${if (rival.isSimulated) "Simulasi" else "Manual"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = resultLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = resultColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${rival.manualCountToday}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text("reels", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(onClick = onEditScore) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Ubah skor ${rival.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Hapus ${rival.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddBattleRivalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("Semarang Timur") }
    var countText by remember { mutableStateOf("40") }
    var isSimulated by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kanca Battle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Jeneng Kanca") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("Daerah (mis. Pedurungan / Gayamsari)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Skor Reels Dino Iki") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gerak otomatis (Simulasi)", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isSimulated, onCheckedChange = { isSimulated = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countText.toIntOrNull() ?: 0
                    onConfirm(name, area, count, isSimulated)
                },
                modifier = Modifier.testTag("confirm_add_rival_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun EditRivalScoreDialog(
    rival: BattleRivalEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var scoreText by remember(rival) { mutableStateOf(rival.manualCountToday.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update Skor ${rival.name}") },
        text = {
            OutlinedTextField(
                value = scoreText,
                onValueChange = { scoreText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Jumlah Reels Dino Iki") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(scoreText.toIntOrNull() ?: 0) }) {
                Text("Simpen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
