package com.armeasure.app.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.armeasure.app.ar.ArSessionManager
import com.armeasure.app.model.MeasureMode
import com.armeasure.app.model.PlacementState
import com.armeasure.app.ui.components.*
import com.armeasure.app.ui.theme.AppColors
import com.armeasure.app.viewmodel.MeasureViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainMeasureScreen(vm: MeasureViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as Activity

    // Camera permission
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // UI state
    var showHistory by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }

    // AR session
    val sessionManager = remember { ArSessionManager(context) }

    // Sync mode from UI state to SessionManager
    LaunchedEffect(uiState.mode) {
        sessionManager.currentMode = uiState.mode
    }

    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted) {
            val ok = sessionManager.createSession(activity)
            if (!ok) vm.setArAvailable(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose { sessionManager.destroy() }
    }

    // ── Permission gate ────────────────────────────────────────────────────────
    if (!cameraPermission.status.isGranted) {
        PermissionScreen(onRequestPermission = { cameraPermission.launchPermissionRequest() })
        return
    }

    // ── AR unavailable ─────────────────────────────────────────────────────────
    if (!uiState.isArAvailable && uiState.mode != MeasureMode.LEVEL) {
        ArUnavailableScreen()
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {

        // ── AR Camera View ─────────────────────────────────────────────────────
        if (uiState.mode != MeasureMode.LEVEL) {
            ArSurfaceView(
                sessionManager  = sessionManager,
                onStateChanged  = vm::onPlacementStateChanged,
                onMeasurementReady = { d ->
                    vm.onMeasurementComplete(d)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Level mode ─────────────────────────────────────────────────────────
        if (uiState.mode == MeasureMode.LEVEL) {
            val levelData by vm.levelData.collectAsState()
            LevelOverlay(levelData = levelData, modifier = Modifier.fillMaxSize())
        }

        // ── Measure / Height AR overlay ────────────────────────────────────────
        if (uiState.mode != MeasureMode.LEVEL) {
            MeasureArOverlay(
                placementState    = uiState.placementState,
                distanceFormatted = vm.formattedDistance(),
                unit              = uiState.unit.symbol,
                onToggleUnit      = vm::toggleUnit,
                onReset           = {
                    sessionManager.reset()
                    vm.onReset()
                }
            )
        }

        // ── Top bar ────────────────────────────────────────────────────────────
        TopBar(
            mode         = uiState.mode,
            onShowHistory = { showHistory = true },
            modifier     = Modifier.align(Alignment.TopStart).statusBarsPadding()
        )

        // ── Bottom mode selector ───────────────────────────────────────────────
        BottomModeSelector(
            currentMode    = uiState.mode,
            onModeSelected = { mode ->
                sessionManager.reset()
                vm.setMode(mode)
                showInstructions = true // Show instructions when mode changes
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )

        // ── Instructions Overlay ───────────────────────────────────────────────
        if (showInstructions && uiState.mode != MeasureMode.LEVEL) {
            InstructionsOverlay(
                mode = uiState.mode,
                onDismiss = { showInstructions = false }
            )
        }

        // ── History drawer ─────────────────────────────────────────────────────
        HistoryDrawer(
            measurements = uiState.measurementHistory,
            unit         = uiState.unit,
            visible      = showHistory,
            onDismiss    = { showHistory = false }
        )
    }
}

@Composable
fun InstructionsOverlay(mode: MeasureMode, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = if (mode == MeasureMode.HEIGHT) Icons.Default.Height else Icons.Default.Straighten,
                    contentDescription = null,
                    tint = AppColors.Accent,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = if (mode == MeasureMode.HEIGHT) "How to measure height" else "How to measure distance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InstructionItem("1", "Stand 2-3 meters back from the subject.")
                    InstructionItem("2", "Slowly scan the floor until a grid appears.")
                    if (mode == MeasureMode.HEIGHT) {
                        InstructionItem("3", "Tap the floor at the person's heels.")
                        InstructionItem("4", "Slowly tilt up and tap the top of their head.")
                    } else {
                        InstructionItem("3", "Tap the floor at the starting point.")
                        InstructionItem("4", "Move the phone and tap at the end point.")
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) {
                    Text("GOT IT")
                }
            }
        }
    }
}

@Composable
fun InstructionItem(number: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(AppColors.Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Text(text, color = AppColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Top app bar ────────────────────────────────────────────────────────────────

@Composable
fun TopBar(
    mode: MeasureMode,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App title
        Column {
            Text(
                text = "MEASURE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.Accent,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(3f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            )
            Text(
                text = when (mode) {
                    MeasureMode.MEASURE -> "Length"
                    MeasureMode.HEIGHT  -> "Height"
                    MeasureMode.LEVEL   -> "Spirit Level"
                },
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // History button
        if (mode != MeasureMode.LEVEL) {
            IconButton(
                onClick = onShowHistory,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AppColors.Surface.copy(alpha = 0.85f))
                    .border(1.dp, AppColors.Grid, CircleShape)
            ) {
                Icon(Icons.Outlined.History, "History", tint = AppColors.TextPrimary)
            }
        }
    }
}

// ── Measure AR overlay ─────────────────────────────────────────────────────────

@Composable
fun MeasureArOverlay(
    placementState: PlacementState,
    distanceFormatted: String?,
    unit: String,
    onToggleUnit: () -> Unit,
    onReset: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Reticle
        if (placementState == PlacementState.READY || placementState == PlacementState.FIRST_POINT_SET) {
            Reticle(
                modifier = Modifier.align(Alignment.Center),
                color = if (placementState == PlacementState.FIRST_POINT_SET) AppColors.AccentSecond
                        else AppColors.Accent
            )
        }

        // Scanning animation
        if (placementState == PlacementState.SCANNING) {
            ScanningIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Status banner
        val (bannerText, bannerColor) = when (placementState) {
            PlacementState.SCANNING        -> "Slowly move your phone to scan the surface" to AppColors.TextSecondary
            PlacementState.READY           -> "Tap to place the start point" to AppColors.Accent
            PlacementState.FIRST_POINT_SET -> "Move to endpoint, then tap" to AppColors.AccentSecond
            PlacementState.MEASURED        -> "Tap to start a new measurement" to AppColors.Success
            PlacementState.ERROR           -> "AR error — please restart" to AppColors.Error
        }

        StatusBanner(
            message  = bannerText,
            tint     = bannerColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .widthIn(max = 300.dp)
        )

        // Measurement bubble
        AnimatedVisibility(
            visible  = distanceFormatted != null,
            enter    = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit    = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center).offset(y = (-90).dp)
        ) {
            distanceFormatted?.let { MeasurementBubble(it) }
        }

        // Top-right controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 120.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            UnitBadge(unit = unit, onClick = onToggleUnit)

            AnimatedVisibility(visible = placementState != PlacementState.SCANNING) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Surface.copy(alpha = 0.9f))
                        .border(1.dp, AppColors.Grid, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Refresh, "Reset", tint = AppColors.AccentSecond)
                }
            }
        }

        // Point indicators at bottom
        if (placementState == PlacementState.FIRST_POINT_SET || placementState == PlacementState.MEASURED) {
            PointStatusRow(
                hasStart = true,
                hasEnd   = placementState == PlacementState.MEASURED,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp)
            )
        }
    }
}

// ── Animated scanning indicator ────────────────────────────────────────────────

@Composable
fun ScanningIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.Surface.copy(alpha = 0.8f))
            .border(1.dp, AppColors.Accent.copy(alpha = alpha), RoundedCornerShape(16.dp))
            .padding(horizontal = 32.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("◉", style = MaterialTheme.typography.headlineLarge.copy(color = AppColors.Accent.copy(alpha = alpha)))
            Text(
                "Scanning for surfaces…",
                style = MaterialTheme.typography.bodyMedium.copy(color = AppColors.TextSecondary, textAlign = TextAlign.Center)
            )
        }
    }
}

// ── Start / end point status row ───────────────────────────────────────────────

@Composable
fun PointStatusRow(hasStart: Boolean, hasEnd: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.Surface.copy(alpha = 0.88f))
            .border(1.dp, AppColors.Grid, RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PointDot("START", hasStart, AppColors.Accent)
        Box(Modifier.width(24.dp).height(1.dp).background(AppColors.Grid))
        PointDot("END", hasEnd, AppColors.AccentSecond)
    }
}

@Composable
fun PointDot(label: String, active: Boolean, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(10.dp).clip(CircleShape)
                .background(if (active) color else AppColors.Grid)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (active) color else AppColors.TextSecondary,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

// ── Bottom mode selector ───────────────────────────────────────────────────────

@Composable
fun BottomModeSelector(
    currentMode: MeasureMode,
    onModeSelected: (MeasureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(AppColors.Surface.copy(alpha = 0.92f))
            .border(1.dp, AppColors.Grid, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(
            MeasureMode.MEASURE to "Measure",
            MeasureMode.HEIGHT  to "Height",
            MeasureMode.LEVEL   to "Level"
        ).forEach { (mode, label) ->
            ModeChip(label, currentMode == mode, { onModeSelected(mode) })
        }
    }
}
