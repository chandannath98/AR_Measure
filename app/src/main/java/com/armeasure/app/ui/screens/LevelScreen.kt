package com.armeasure.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armeasure.app.model.LevelData
import com.armeasure.app.ui.theme.AppColors
import kotlin.math.*

@Composable
fun LevelOverlay(
    levelData: LevelData,
    modifier: Modifier = Modifier
) {
    val bubbleColor by animateColorAsState(
        targetValue = if (levelData.isLevel) AppColors.Success else AppColors.Accent,
        animationSpec = tween(300),
        label = "bubbleColor"
    )

    Box(
        modifier = modifier.background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {

        // ── Background grid lines ──────────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // Title
            Text(
                text = "SPIRIT LEVEL",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 4.sp,
                    color = AppColors.TextSecondary
                )
            )

            // ── Circular bubble level ──────────────────────────────────────────
            BubbleLevelCircle(
                pitch = levelData.pitch,
                roll = levelData.roll,
                bubbleColor = bubbleColor,
                modifier = Modifier.size(280.dp)
            )

            // ── Angle readouts ─────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                AngleReadout(label = "PITCH", value = levelData.pitch)
                AngleReadout(label = "ROLL",  value = levelData.roll)
            }

            // ── Level status ───────────────────────────────────────────────────
            LevelStatusBadge(isLevel = levelData.isLevel)
        }

        // ── Horizon line ───────────────────────────────────────────────────────
        HorizonIndicator(
            roll = levelData.roll,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.Center)
        )
    }
}

// ── Bubble Circle ─────────────────────────────────────────────────────────────

@Composable
fun BubbleLevelCircle(
    pitch: Float,
    roll: Float,
    bubbleColor: Color,
    modifier: Modifier = Modifier
) {
    val maxOffset = 80f
    // Clamp bubble position within the outer circle
    val rawX = -roll  / 45f * maxOffset
    val rawY = -pitch / 45f * maxOffset
    val dist = sqrt(rawX * rawX + rawY * rawY)
    val clampedDist = min(dist, maxOffset)
    val angle = if (dist == 0f) 0f else atan2(rawY, rawX)
    val bx by animateFloatAsState(
        targetValue = if (dist == 0f) 0f else cos(angle) * clampedDist,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "bx"
    )
    val by by animateFloatAsState(
        targetValue = if (dist == 0f) 0f else sin(angle) * clampedDist,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "by"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val outerR = size.minDimension / 2 - 8.dp.toPx()
        val innerR = 36.dp.toPx()    // center target circle
        val bubbleR = 24.dp.toPx()

        // Outer ring
        drawCircle(
            color = AppColors.Grid,
            radius = outerR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Subtle background fill
        drawCircle(
            brush = Brush.radialGradient(
                listOf(AppColors.SurfaceAlt, AppColors.Background),
                center = Offset(cx, cy),
                radius = outerR
            ),
            radius = outerR,
            center = Offset(cx, cy)
        )

        // Crosshair
        val lineLen = outerR
        drawLine(AppColors.Grid, Offset(cx - lineLen, cy), Offset(cx + lineLen, cy), 1.dp.toPx())
        drawLine(AppColors.Grid, Offset(cx, cy - lineLen), Offset(cx, cy + lineLen), 1.dp.toPx())

        // Concentric rings
        listOf(0.33f, 0.66f).forEach { frac ->
            drawCircle(AppColors.Grid.copy(alpha = 0.4f), radius = outerR * frac,
                center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }

        // Center target circle
        drawCircle(
            color = if (bubbleColor == AppColors.Success) AppColors.Success.copy(alpha = 0.2f)
                    else AppColors.Accent.copy(alpha = 0.12f),
            radius = innerR,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = bubbleColor.copy(alpha = 0.6f),
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(1.5.dp.toPx())
        )

        // Bubble
        val bCenter = Offset(cx + bx, cy + by)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(bubbleColor.copy(alpha = 0.9f), bubbleColor.copy(alpha = 0.5f)),
                center = bCenter,
                radius = bubbleR
            ),
            radius = bubbleR,
            center = bCenter
        )
        drawCircle(
            color = bubbleColor,
            radius = bubbleR,
            center = bCenter,
            style = Stroke(2.dp.toPx())
        )
        // Bubble highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = bubbleR * 0.35f,
            center = Offset(bCenter.x - bubbleR * 0.2f, bCenter.y - bubbleR * 0.2f)
        )
    }
}

// ── Horizon line that rotates with roll ────────────────────────────────────────

@Composable
fun HorizonIndicator(roll: Float, modifier: Modifier = Modifier) {
    val animatedRoll by animateFloatAsState(roll, spring(), label = "roll")
    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val halfLen = size.width * 0.45f
        val rad = Math.toRadians(animatedRoll.toDouble()).toFloat()
        val dx = cos(rad) * halfLen
        val dy = sin(rad) * halfLen
        drawLine(
            AppColors.AccentThird.copy(alpha = 0.7f),
            Offset(cx - dx, cy - dy), Offset(cx + dx, cy + dy),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Center tick
        drawCircle(AppColors.AccentThird, 5.dp.toPx(), Offset(cx, cy))
    }
}

// ── Angle readout box ─────────────────────────────────────────────────────────

@Composable
fun AngleReadout(label: String, value: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Grid, RoundedCornerShape(12.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                color = AppColors.TextSecondary
            )
        )
        Text(
            text = "%.1f°".format(value),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = AppColors.Accent
            )
        )
    }
}

// ── Level / Not level badge ────────────────────────────────────────────────────

@Composable
fun LevelStatusBadge(isLevel: Boolean) {
    val color = if (isLevel) AppColors.Success else AppColors.TextSecondary
    val text  = if (isLevel) "✓  LEVEL" else "Adjust to level"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = color,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

// ── Background grid helper ────────────────────────────────────────────────────

private fun DrawScope.drawGrid() {
    val spacing = 48.dp.toPx()
    val color   = AppColors.Grid.copy(alpha = 0.35f)
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 0.5f)
        x += spacing
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 0.5f)
        y += spacing
    }
}
