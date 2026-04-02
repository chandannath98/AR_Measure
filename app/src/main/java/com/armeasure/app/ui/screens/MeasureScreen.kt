package com.armeasure.app.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armeasure.app.ar.ArSessionManager
import com.armeasure.app.model.MeasureMode
import com.armeasure.app.model.PlacementState
import com.armeasure.app.ui.components.*
import com.armeasure.app.ui.theme.AppColors
import com.armeasure.app.viewmodel.MeasureViewModel

@Composable
fun MeasureScreen(vm: MeasureViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    // AR session
    val sessionManager = remember { ArSessionManager(context) }

    LaunchedEffect(Unit) {
        val ok = sessionManager.createSession(activity)
        if (!ok) vm.setArAvailable(false)
    }

    DisposableEffect(Unit) {
        onDispose { sessionManager.destroy() }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {

        // ── AR Camera View ─────────────────────────────────────────────────────
        if (uiState.isArAvailable && uiState.mode != MeasureMode.LEVEL) {
            ArSurfaceView(
                sessionManager = sessionManager,
                onStateChanged = vm::onPlacementStateChanged,
                onMeasurementReady = vm::onMeasurementComplete,
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Level mode ─────────────────────────────────────────────────────────
        if (uiState.mode == MeasureMode.LEVEL) {
            val levelData by vm.levelData.collectAsState()
            LevelOverlay(levelData = levelData, modifier = Modifier.fillMaxSize())
        }

        // ── Measure / Height overlay ───────────────────────────────────────────
        if (uiState.mode != MeasureMode.LEVEL) {
            MeasureOverlay(
                placementState   = uiState.placementState,
                distanceFormatted = vm.formattedDistance(),
                unit             = uiState.unit.symbol,
                onToggleUnit     = vm::toggleUnit,
                onReset          = {
                    sessionManager.reset()
                    vm.onReset()
                }
            )
        }

        // ── Bottom mode bar ────────────────────────────────────────────────────
        BottomModeBar(
            currentMode = uiState.mode,
            onModeSelected = vm::setMode,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        )
    }
}

// ── Measure Overlay ────────────────────────────────────────────────────────────

@Composable
fun MeasureOverlay(
    placementState: PlacementState,
    distanceFormatted: String?,
    unit: String,
    onToggleUnit: () -> Unit,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Reticle at center
        if (placementState == PlacementState.READY || placementState == PlacementState.FIRST_POINT_SET) {
            Reticle(
                modifier = Modifier.align(Alignment.Center),
                color = if (placementState == PlacementState.FIRST_POINT_SET) AppColors.AccentSecond
                        else AppColors.Accent
            )
        }

        // Status banner
        val bannerText = when (placementState) {
            PlacementState.SCANNING          -> "Move your phone slowly to scan the area"
            PlacementState.READY             -> "Tap to place starting point"
            PlacementState.FIRST_POINT_SET   -> "Move to end point, then tap"
            PlacementState.MEASURED          -> "Tap anywhere to start a new measurement"
            PlacementState.ERROR             -> "AR unavailable. Please restart the app."
            else -> ""
        }
        StatusBanner(
            message = bannerText,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .widthIn(max = 320.dp),
            tint = if (placementState == PlacementState.ERROR) AppColors.Error else AppColors.TextSecondary
        )

        // Measurement result
        AnimatedVisibility(
            visible = distanceFormatted != null,
            enter   = scaleIn() + fadeIn(),
            exit    = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center).offset(y = (-100).dp)
        ) {
            distanceFormatted?.let {
                MeasurementBubble(value = it)
            }
        }

        // Top-right controls
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UnitBadge(unit = unit, onClick = onToggleUnit)
            if (placementState != PlacementState.SCANNING) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Surface)
                ) {
                    Icon(Icons.Default.Refresh, "Reset", tint = AppColors.AccentSecond)
                }
            }
        }
    }
}

// ── Bottom mode bar ────────────────────────────────────────────────────────────

@Composable
fun BottomModeBar(
    currentMode: MeasureMode,
    onModeSelected: (MeasureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.Surface.copy(alpha = 0.9f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeChip("Measure", currentMode == MeasureMode.MEASURE, { onModeSelected(MeasureMode.MEASURE) })
        ModeChip("Height",  currentMode == MeasureMode.HEIGHT,  { onModeSelected(MeasureMode.HEIGHT)  })
        ModeChip("Level",   currentMode == MeasureMode.LEVEL,   { onModeSelected(MeasureMode.LEVEL)   })
    }
}
