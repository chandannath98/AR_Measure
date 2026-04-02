package com.armeasure.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.armeasure.app.ar.LevelSensorHelper
import com.armeasure.app.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MeasureUiState(
    val mode: MeasureMode          = MeasureMode.MEASURE,
    val placementState: PlacementState = PlacementState.SCANNING,
    val unit: MeasurementUnit      = MeasurementUnit.CENTIMETERS,
    val distanceMeters: Float?     = null,
    val measurementHistory: List<Float> = emptyList(),  // metres
    val isArAvailable: Boolean     = true
)

class MeasureViewModel(app: Application) : AndroidViewModel(app) {

    private val levelHelper = LevelSensorHelper(app)

    private val _uiState = MutableStateFlow(MeasureUiState())
    val uiState: StateFlow<MeasureUiState> = _uiState.asStateFlow()

    val levelData = levelHelper.levelData

    init {
        levelHelper.start()
    }

    fun setMode(mode: MeasureMode) {
        _uiState.update { it.copy(mode = mode, distanceMeters = null, placementState = PlacementState.SCANNING) }
    }

    fun setUnit(unit: MeasurementUnit) = _uiState.update { it.copy(unit = unit) }

    fun onPlacementStateChanged(state: PlacementState) = _uiState.update { it.copy(placementState = state) }

    fun onMeasurementComplete(distanceMeters: Float) {
        _uiState.update {
            val history = (it.measurementHistory + distanceMeters).takeLast(10)
            it.copy(distanceMeters = distanceMeters, measurementHistory = history)
        }
    }

    fun onReset() {
        _uiState.update { it.copy(distanceMeters = null, placementState = PlacementState.SCANNING) }
    }

    fun setArAvailable(available: Boolean) = _uiState.update { it.copy(isArAvailable = available) }

    fun formattedDistance(): String? {
        val d = _uiState.value.distanceMeters ?: return null
        return d.formatUnit(_uiState.value.unit)
    }

    fun toggleUnit() {
        val units = MeasurementUnit.entries
        val current = _uiState.value.unit
        val next = units[(units.indexOf(current) + 1) % units.size]
        setUnit(next)
    }

    override fun onCleared() {
        super.onCleared()
        levelHelper.stop()
    }
}
