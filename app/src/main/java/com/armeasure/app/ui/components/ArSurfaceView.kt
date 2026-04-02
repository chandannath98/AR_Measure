package com.armeasure.app.ui.components

import android.opengl.GLSurfaceView
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.armeasure.app.ar.ArRenderer
import com.armeasure.app.ar.ArSessionManager
import com.armeasure.app.model.PlacementState

/**
 * Lifecycle-aware GLSurfaceView wrapper that feeds ARCore camera frames.
 */
@Composable
fun ArSurfaceView(
    sessionManager: ArSessionManager,
    onStateChanged: (PlacementState) -> Unit,
    onMeasurementReady: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val glView = remember {
        GLSurfaceView(context).apply {
            preserveEGLContextOnPause = true
            setEGLContextClientVersion(2)
            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            val renderer = ArRenderer(context, sessionManager, onStateChanged, onMeasurementReady)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    renderer.enqueueTap(MotionEvent.obtain(event))
                }
                true
            }
        }
    }

    // Bind GLSurfaceView and AR session to lifecycle.
    // We include sessionManager.session in keys so that if the session is created
    // after this component is already resumed, it will trigger a resume call.
    DisposableEffect(lifecycleOwner, sessionManager, sessionManager.session) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    sessionManager.resume()
                    glView.onResume()
                }
                Lifecycle.Event.ON_PAUSE  -> {
                    glView.onPause()
                    sessionManager.pause()
                }
                else -> Unit
            }
        }
        
        // If the lifecycle is already resumed and we have a session, resume it now.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            sessionManager.resume()
            glView.onResume()
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Note: glView.onPause and sessionManager.pause are called here 
            // to ensure cleanup if the composable is removed while active.
            glView.onPause()
            sessionManager.pause()
        }
    }

    AndroidView(factory = { glView }, modifier = modifier)
}
