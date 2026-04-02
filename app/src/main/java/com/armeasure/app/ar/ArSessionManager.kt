package com.armeasure.app.ar

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.compose.runtime.*
import com.armeasure.app.model.AnchorPoint
import com.armeasure.app.model.MeasurementResult
import com.armeasure.app.model.PlacementState
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import kotlin.math.sqrt

private const val TAG = "ArSessionManager"

/**
 * Manages the ARCore session lifecycle and hit-test based measurement.
 */
class ArSessionManager(private val context: Context) {

    var session: Session? by mutableStateOf(null)
        private set

    private var displayRotationHelper: DisplayRotationHelper? = null

    // ── State ──────────────────────────────────────────────────────────────────
    var placementState: PlacementState by mutableStateOf(PlacementState.SCANNING)
        private set

    var startAnchor: AnchorPoint? by mutableStateOf(null)
        private set
    var endAnchor: AnchorPoint? by mutableStateOf(null)
        private set

    var lastMeasurement: MeasurementResult? by mutableStateOf(null)
        private set

    var trackingState: TrackingState by mutableStateOf(TrackingState.STOPPED)
        private set

    // ── Session setup ──────────────────────────────────────────────────────────

    fun createSession(activity: Activity): Boolean {
        return try {
            if (!ArCoreApk.getInstance().checkAvailability(context).isSupported) {
                Log.e(TAG, "ARCore not supported")
                return false
            }
            val s = Session(context)
            val config = Config(s).apply {
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                focusMode = Config.FocusMode.AUTO
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            }
            s.configure(config)
            session = s
            displayRotationHelper = DisplayRotationHelper(context)
            true
        } catch (e: UnavailableArcoreNotInstalledException) {
            Log.e(TAG, "ARCore not installed", e); false
        } catch (e: UnavailableDeviceNotCompatibleException) {
            Log.e(TAG, "Device not compatible", e); false
        } catch (e: Exception) {
            Log.e(TAG, "Session creation failed", e); false
        }
    }

    fun setCameraTextureId(textureId: Int) {
        session?.setCameraTextureName(textureId)
    }

    fun resume() {
        try {
            session?.resume()
            displayRotationHelper?.onResume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available on resume", e)
            session = null
        }
    }

    fun pause() {
        displayRotationHelper?.onPause()
        session?.pause()
    }

    fun destroy() {
        session?.close()
        session = null
    }

    fun onSurfaceChanged(width: Int, height: Int) {
        displayRotationHelper?.onSurfaceChanged(width, height)
        session?.setDisplayGeometry(
            displayRotationHelper?.rotation ?: 0, width, height
        )
    }

    // ── Per-frame update ───────────────────────────────────────────────────────

    fun update(): Frame? {
        val s = session ?: return null
        displayRotationHelper?.updateSessionIfNeeded(s)
        return try {
            val frame = s.update()
            trackingState = frame.camera.trackingState

            // Advance to READY once we detect a plane
            if (placementState == PlacementState.SCANNING &&
                trackingState == TrackingState.TRACKING &&
                s.getAllTrackables(Plane::class.java).any { it.trackingState == TrackingState.TRACKING }
            ) {
                placementState = PlacementState.READY
            }
            frame
        } catch (e: Exception) {
            // Log.e(TAG, "Session update failed", e) // Avoid flooding logs
            null
        }
    }

    // ── Touch / placement ──────────────────────────────────────────────────────

    /**
     * Called on user tap. Performs a hit-test and places an anchor.
     * Returns true when a measurement is complete.
     */
    fun onTap(tap: MotionEvent, frame: Frame, screenW: Int, screenH: Int): Boolean {
        if (trackingState != TrackingState.TRACKING) return false

        val hitResult = frame.hitTest(tap).firstOrNull { hit ->
            val trackable = hit.trackable
            (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) &&
                    trackable.trackingState == TrackingState.TRACKING) ||
                    (trackable is Point && trackable.orientationMode == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)
        } ?: return false

        return when (placementState) {
            PlacementState.READY, PlacementState.MEASURED -> {
                // Start fresh
                startAnchor?.anchor?.detach()
                endAnchor?.anchor?.detach()
                startAnchor = AnchorPoint(hitResult.createAnchor(), tap.x, tap.y)
                endAnchor = null
                lastMeasurement = null
                placementState = PlacementState.FIRST_POINT_SET
                false
            }
            PlacementState.FIRST_POINT_SET -> {
                endAnchor = AnchorPoint(hitResult.createAnchor(), tap.x, tap.y)
                computeMeasurement()
                placementState = PlacementState.MEASURED
                true
            }
            else -> false
        }
    }

    /** Called on tap when in HEIGHT mode – uses two y-positions on the same x. */
    fun onHeightTap(tap: MotionEvent, frame: Frame): Boolean {
        return onTap(tap, frame, 0, 0)
    }

    fun reset() {
        startAnchor?.anchor?.detach()
        endAnchor?.anchor?.detach()
        startAnchor = null
        endAnchor = null
        lastMeasurement = null
        placementState = if (trackingState == TrackingState.TRACKING) PlacementState.READY
        else PlacementState.SCANNING
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private fun computeMeasurement() {
        val start = startAnchor?.anchor?.pose ?: return
        val end   = endAnchor?.anchor?.pose   ?: return
        val dx = start.tx() - end.tx()
        val dy = start.ty() - end.ty()
        val dz = start.tz() - end.tz()
        val distance = sqrt(dx * dx + dy * dy + dz * dz)
        lastMeasurement = MeasurementResult(distance, start, end)
    }
}
