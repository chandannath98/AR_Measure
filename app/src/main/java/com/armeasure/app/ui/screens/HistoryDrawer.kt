package com.armeasure.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armeasure.app.model.MeasurementUnit
import com.armeasure.app.model.formatUnit
import com.armeasure.app.ui.theme.AppColors

@Composable
fun HistoryDrawer(
    measurements: List<Float>,
    unit: MeasurementUnit,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter   = slideInVertically(initialOffsetY = { it }),
        exit    = slideOutVertically(targetOffsetY  = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background.copy(alpha = 0.92f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(AppColors.Surface)
                    .padding(24.dp)
            ) {
                // Handle + header
                Box(
                    modifier = Modifier
                        .width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppColors.Grid)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(20.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, null, tint = AppColors.Accent)
                        Text(
                            "Measurement History",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close", tint = AppColors.TextSecondary)
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (measurements.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No measurements yet.\nStart measuring to see results here.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textAlign = TextAlign.Center,
                                color = AppColors.TextSecondary
                            )
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(measurements.reversed()) { index, metres ->
                            HistoryRow(
                                index   = measurements.size - index,
                                value   = metres.formatUnit(unit),
                                metres  = metres
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(index: Int, value: String, metres: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.SurfaceAlt)
            .border(1.dp, AppColors.Grid, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.Accent.copy(alpha = 0.15f))
                .border(1.dp, AppColors.Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$index",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = AppColors.Accent,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        // Measurement
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = AppColors.AccentThird,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        )

        // Metres equivalent (always shown for reference)
        Text(
            text = "%.3f m".format(metres),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = AppColors.TextSecondary,
                fontSize = 12.sp
            )
        )
    }
}
