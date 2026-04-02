package com.armeasure.app.ar

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import com.google.ar.core.Session

/**
 * Helper to track display rotation changes and update the ARCore session.
 */
class DisplayRotationHelper(context: Context) : DisplayManager.DisplayListener {

    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager

    private val display: Display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay

    private var viewportChanged = false
    var viewportWidth  = 0
        private set
    var viewportHeight = 0
        private set

    var rotation: Int = display.rotation
        private set

    fun onResume()  = displayManager.registerDisplayListener(this, null)
    fun onPause()   = displayManager.unregisterDisplayListener(this)

    fun onSurfaceChanged(width: Int, height: Int) {
        viewportWidth   = width
        viewportHeight  = height
        viewportChanged = true
    }

    fun updateSessionIfNeeded(session: Session) {
        if (viewportChanged) {
            rotation = display.rotation
            session.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
            viewportChanged = false
        }
    }

    override fun onDisplayAdded(id: Int)   {}
    override fun onDisplayRemoved(id: Int) {}
    override fun onDisplayChanged(id: Int) { viewportChanged = true }
}
