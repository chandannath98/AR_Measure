package com.armeasure.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armeasure.app.ui.theme.AppColors

// ── Measurement display bubble ────────────────────────────────────────────────

@Composable
fun MeasurementBubble(
    value: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.radialGradient(
                    listOf(AppColors.Background.copy(alpha = 0.95f), AppColors.Surface.copy(alpha = 0.9f))
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(AppColors.Accent.copy(alpha = glowAlpha), AppColors.AccentThird.copy(alpha = glowAlpha))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium.copy(
                color = AppColors.AccentThird,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ── Scan hint banner ──────────────────────────────────────────────────────────

@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextSecondary
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.Background.copy(alpha = 0.75f))
            .border(1.dp, AppColors.Grid, RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(color = tint),
            textAlign = TextAlign.Center
        )
    }
}

// ── Circular icon button ──────────────────────────────────────────────────────

@Composable
fun CircleIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.TextPrimary,
    size: Dp = 56.dp
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Grid, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = AppColors.TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

// ── Mode chip ─────────────────────────────────────────────────────────────────

@Composable
fun ModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) AppColors.Accent else AppColors.Surface
    val textColor = if (selected) AppColors.Background else AppColors.TextSecondary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else AppColors.Grid,
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = textColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

// ── Crosshair / reticle ───────────────────────────────────────────────────────

@Composable
fun Reticle(
    modifier: Modifier = Modifier,
    color: Color = AppColors.Accent,
    size: Dp = 60.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reticle")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue  = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .drawBehind {
                val cx = this.size.width / 2
                val cy = this.size.height / 2
                val arm = this.size.width / 2
                val gap = arm * 0.3f
                val stroke = 2.5f

                // Horizontal arms
                drawLine(color, Offset(cx - arm, cy), Offset(cx - gap, cy), strokeWidth = stroke)
                drawLine(color, Offset(cx + gap, cy), Offset(cx + arm, cy), strokeWidth = stroke)
                // Vertical arms
                drawLine(color, Offset(cx, cy - arm), Offset(cx, cy - gap), strokeWidth = stroke)
                drawLine(color, Offset(cx, cy + gap), Offset(cx, cy + arm), strokeWidth = stroke)
                // Center dot
                drawCircle(color, radius = 3f, center = Offset(cx, cy))
            }
    )
}

// ── Unit toggle pill ──────────────────────────────────────────────────────────

@Composable
fun UnitBadge(unit: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceAlt)
            .border(1.dp, AppColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = unit,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppColors.Accent,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
