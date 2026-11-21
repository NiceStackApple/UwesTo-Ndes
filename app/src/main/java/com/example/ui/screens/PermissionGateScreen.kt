package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.PermissionHealth
import com.example.service.PermissionHelper
import com.example.ui.theme.BrainTiredAmber

/**
 * Full-screen mandatory permission gate shown whenever Accessibility or Overlay
 * permission is off after onboarding. Automatically clears as soon as both are granted.
 */
@Composable
fun PermissionGateScreen(
    permissionHealth: PermissionHealth
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrainTiredAmber.copy(alpha = 0.14f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .testTag("mandatory_permission_gate"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MandatoryPermissionChecklist(
                title = "Izin Wajib Urip Sik, Ndes!",
                subtitle = "Deteksi UwesTo lagi mati. Uripke izin Accessibility lan Overlay nang ngisor iki ben kowe iso mlebu lan ngitung scroll otomatis.",
                health = permissionHealth,
                onOpenAppInfo = { PermissionHelper.openAppInfoSettings(context) },
                onOpenAccessibility = { PermissionHelper.openAccessibilitySettings(context) },
                onOpenOverlay = { PermissionHelper.openOverlaySettings(context) }
            )

            OutlinedButton(
                onClick = { PermissionHelper.openBatteryOptimizationSettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tips Anti-Mati: Kecualikan Penghemat Baterai")
            }
        }
    }
}
