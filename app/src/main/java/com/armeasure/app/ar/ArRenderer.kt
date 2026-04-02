package com.armeasure.app.ar

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent
import com.armeasure.app.model.MeasureMode
import com.armeasure.app.model.PlacementState
import com.google.ar.core.*
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

private const val TAG = "ArRenderer"

/**
 * GLSurfaceView renderer that draws the camera background and AR anchors/lines.
 */
class ArRenderer(
    private val context: Context,
    private val sessionManager: ArSessionManager,
    private val onStateChanged: (PlacementState) -> Unit,
    private val onMeasurementReady: (Float) -> Unit  // metres
) : GLSurfaceView.Renderer {

    // ── Background renderer ────────────────────────────────────────────────────
    private val backgroundRenderer = BackgroundRenderer()

    // ── Point / Line rendering ─────────────────────────────────────────────────
    private val pointRenderer  = PointCloudRenderer()
    private val planeRenderer  = PlaneRenderer()
    private val anchorRenderer = AnchorDotRenderer()

    // ── Matrices ───────────────────────────────────────────────────────────────
    private val projMtx   = FloatArray(16)
    private val viewMtx   = FloatArray(16)

    // ── Input queue ────────────────────────────────────────────────────────────
    private val tapQueue = ArrayDeque<MotionEvent>()

    fun enqueueTap(e: MotionEvent) { tapQueue.addLast(e) }

    // ── GL lifecycle ───────────────────────────────────────────────────────────

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        try {
            backgroundRenderer.createOnGlThread(context)
            sessionManager.setCameraTextureId(backgroundRenderer.textureId)
            
            planeRenderer.createOnGlThread(context, "models/trigrid.png")
            pointRenderer.createOnGlThread(context)
            anchorRenderer.createOnGlThread()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read asset", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        sessionManager.onSurfaceChanged(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = sessionManager.session ?: return
        
        // Ensure texture is set
        sessionManager.setCameraTextureId(backgroundRenderer.textureId)
        
        val frame = sessionManager.update() ?: return

        // ── Draw camera background ─────────────────────────────────────────────
        backgroundRenderer.draw(frame)

        if (frame.camera.trackingState == TrackingState.PAUSED) return

        // ── Handle taps ────────────────────────────────────────────────────────
        val tap = tapQueue.removeFirstOrNull()
        if (tap != null) {
            val complete = sessionManager.onTap(tap, frame, 0, 0)
            onStateChanged(sessionManager.placementState)
            if (complete) {
                sessionManager.lastMeasurement?.distanceMeters?.let { onMeasurementReady(it) }
            }
        }

        // ── Camera matrices ────────────────────────────────────────────────────
        frame.camera.getProjectionMatrix(projMtx, 0, 0.01f, 100f)
        frame.camera.getViewMatrix(viewMtx, 0)

        // ── Draw planes ────────────────────────────────────────────────────────
        val planes = session.getAllTrackables(Plane::class.java)
        planeRenderer.drawPlanes(planes, frame.camera.displayOrientedPose, projMtx)

        // ── Draw point cloud ───────────────────────────────────────────────────
        val pointCloud = frame.acquirePointCloud()
        pointRenderer.update(pointCloud)
        pointRenderer.draw(viewMtx, projMtx)
        pointCloud.release()

        // ── Draw anchors and lines ─────────────────────────────────────────────
        val startAnchor = sessionManager.startAnchor?.anchor
        val endAnchor   = sessionManager.endAnchor?.anchor
        val latestHitPose = sessionManager.latestHitPose

        // Draw start dot
        startAnchor?.let { 
            if (it.trackingState == TrackingState.TRACKING) {
                anchorRenderer.drawDot(it.pose, viewMtx, projMtx, isStart = true)
            }
        }
        
        // Draw end dot
        endAnchor?.let { 
            if (it.trackingState == TrackingState.TRACKING) {
                anchorRenderer.drawDot(it.pose, viewMtx, projMtx, isStart = false)
            }
        }

        // Draw final line
        if (startAnchor != null && endAnchor != null &&
            startAnchor.trackingState == TrackingState.TRACKING &&
            endAnchor.trackingState   == TrackingState.TRACKING
        ) {
            anchorRenderer.drawLine(startAnchor.pose, endAnchor.pose, viewMtx, projMtx)
        } 
        // OR draw preview line if we only have one point and a current hit
        else if (startAnchor != null && endAnchor == null && latestHitPose != null &&
                 startAnchor.trackingState == TrackingState.TRACKING &&
                 sessionManager.placementState == PlacementState.FIRST_POINT_SET) {
            anchorRenderer.drawLine(startAnchor.pose, latestHitPose, viewMtx, projMtx)
        }
    }
}
