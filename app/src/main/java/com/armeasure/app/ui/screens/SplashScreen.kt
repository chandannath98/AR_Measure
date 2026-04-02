package com.armeasure.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armeasure.app.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onComplete: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rotation"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300); titleVisible    = true
        delay(400); subtitleVisible = true
        delay(2000); onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        // Animated rings background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            listOf(200f, 350f, 500f).forEachIndexed { i, r ->
                drawCircle(
                    AppColors.Grid.copy(alpha = 0.25f - i * 0.06f),
                    radius = r.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Logo mark
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(140.dp).rotate(rotation)) {
                    drawCircle(
                        Brush.sweepGradient(listOf(AppColors.Accent, AppColors.Accent.copy(alpha = 0f))),
                        radius = size.minDimension / 2 - 8.dp.toPx(),
                        style = Stroke(3.dp.toPx())
                    )
                }
                Box(
                    Modifier.size(100.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(AppColors.SurfaceAlt, AppColors.Surface)))
                        .border(1.5.dp, AppColors.Accent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📏", fontSize = 36.sp)
                }
            }

            // Title
            AnimatedAppearance(visible = titleVisible) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AR",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = AppColors.Accent,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp
                        )
                    )
                    Text(
                        text = "MEASURE",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 8.sp
                        )
                    )
                }
            }

            AnimatedAppearance(visible = subtitleVisible) {
                Text(
                    text = "Augmented Reality Measuring Tape",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        color = AppColors.TextSecondary
                    )
                )
            }
        }

        // Loading dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) { i ->
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(600, delayMillis = i * 150),
                        RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(
                    Modifier.size(8.dp).clip(CircleShape)
                        .background(AppColors.Accent.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
private fun AnimatedAppearance(visible: Boolean, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(tween(600)) +
                androidx.compose.animation.slideInVertically(tween(600)) { it / 4 }
    ) { content() }
}
