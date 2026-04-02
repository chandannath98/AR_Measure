package com.armeasure.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armeasure.app.ui.theme.AppColors

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "perm")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "pulse"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {

        // Faint radial background glow
        Canvas(modifier = Modifier.size(400.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(AppColors.Accent.copy(alpha = 0.06f), AppColors.Background)
                ),
                radius = size.minDimension / 2
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            // Pulsing camera icon
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(160.dp).scale(pulseScale)) {
                    drawCircle(
                        color = AppColors.Accent.copy(alpha = ringAlpha),
                        radius = size.minDimension / 2,
                        style = Stroke(2.dp.toPx())
                    )
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AppColors.SurfaceAlt, AppColors.Surface)
                            )
                        )
                        .border(1.5.dp, AppColors.Accent.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // Heading
            Text(
                text = "Camera Access\nRequired",
                style = MaterialTheme.typography.headlineLarge.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            )

            // Description
            Text(
                text = "AR Measure uses your camera to detect surfaces and measure real-world distances with augmented reality.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                ),
                modifier = Modifier.widthIn(max = 300.dp)
            )

            Spacer(Modifier.height(8.dp))

            // CTA button
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Accent,
                    contentColor   = AppColors.Background
                )
            ) {
                Text(
                    text = "Allow Camera Access",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Text(
                text = "Your camera is only used for AR — nothing is recorded or stored.",
                style = MaterialTheme.typography.labelSmall.copy(
                    textAlign = TextAlign.Center,
                    color = AppColors.TextSecondary
                )
            )
        }
    }
}

@Composable
fun ArUnavailableScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = "⚠",
                style = MaterialTheme.typography.displayLarge.copy(color = AppColors.AccentSecond)
            )
            Text(
                text = "AR Not Supported",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
            Text(
                text = "This device doesn't support ARCore, which is required for AR measurement. Please try on a compatible Android device.",
                style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }
    }
}
