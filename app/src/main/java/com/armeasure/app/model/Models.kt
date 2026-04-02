package com.armeasure.app.model

import com.google.ar.core.Anchor
import com.google.ar.core.Pose

// ── Measurement unit ──────────────────────────────────────────────────────────
enum class MeasurementUnit(val label: String, val symbol: String) {
    CENTIMETERS("Centimeters", "cm"),
    INCHES("Inches", "in"),
    METERS("Meters", "m"),
    FEET("Feet", "ft")
}

fun Float.toUnit(unit: MeasurementUnit): Float = when (unit) {
    MeasurementUnit.CENTIMETERS -> this * 100f
    MeasurementUnit.INCHES      -> this * 39.3701f
    MeasurementUnit.METERS      -> this
    MeasurementUnit.FEET        -> this * 3.28084f
}

fun Float.formatUnit(unit: MeasurementUnit): String {
    val value = this.toUnit(unit)
    return when (unit) {
        MeasurementUnit.CENTIMETERS -> "%.1f cm".format(value)
        MeasurementUnit.INCHES      -> "%.2f in".format(value)
        MeasurementUnit.METERS      -> "%.3f m".format(value)
        MeasurementUnit.FEET        -> "%.2f ft".format(value)
    }
}

// ── AR screen mode ─────────────────────────────────────────────────────────────
enum class MeasureMode {
    MEASURE,   // Length / distance measurement
    HEIGHT,    // Person height measurement
    LEVEL      // Spirit level
}

// ── AR placement state ─────────────────────────────────────────────────────────
enum class PlacementState {
    SCANNING,          // Moving phone to detect surfaces
    READY,             // Surface detected – tap to place first point
    FIRST_POINT_SET,   // First anchor placed – move to end point
    MEASURED,          // Both anchors placed – measurement shown
    ERROR
}

// ── Measurement result ─────────────────────────────────────────────────────────
data class MeasurementResult(
    val distanceMeters: Float,
    val startPose: Pose,
    val endPose: Pose
)

// ── Level result ───────────────────────────────────────────────────────────────
data class LevelData(
    val pitch: Float,   // degrees – tilt front/back
    val roll: Float,    // degrees – tilt left/right
    val isLevel: Boolean = kotlin.math.abs(pitch) < 2f && kotlin.math.abs(roll) < 2f
)

// ── Anchor point ──────────────────────────────────────────────────────────────
data class AnchorPoint(
    val anchor: Anchor,
    val screenX: Float,
    val screenY: Float
)
