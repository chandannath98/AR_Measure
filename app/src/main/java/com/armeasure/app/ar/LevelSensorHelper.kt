package com.armeasure.app.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.armeasure.app.model.LevelData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Reads the accelerometer to provide pitch/roll for the spirit-level tool.
 */
class LevelSensorHelper(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _levelData = MutableStateFlow(LevelData(0f, 0f))
    val levelData: StateFlow<LevelData> = _levelData

    // Low-pass filter state
    private val alpha = 0.15f
    private var gx = 0f; private var gy = 0f; private var gz = 0f

    fun start() {
        gravitySensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sensorManager.unregisterListener(this)

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        // Low-pass filter
        gx = alpha * event.values[0] + (1 - alpha) * gx
        gy = alpha * event.values[1] + (1 - alpha) * gy
        gz = alpha * event.values[2] + (1 - alpha) * gz

        // Pitch: rotation around X axis (forward/back tilt)
        val pitch = Math.toDegrees(atan2(gy.toDouble(), sqrt((gx * gx + gz * gz).toDouble()))).toFloat()
        // Roll: rotation around Y axis (left/right tilt)
        val roll  = Math.toDegrees(atan2(gx.toDouble(), sqrt((gy * gy + gz * gz).toDouble()))).toFloat()

        _levelData.value = LevelData(pitch, roll)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
