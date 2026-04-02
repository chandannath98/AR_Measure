package com.armeasure.app.ar

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders the camera image as a full-screen background quad.
 */
class BackgroundRenderer {

    private var quadProgram = 0
    private var quadPositionParam = 0
    private var quadTexCoordParam = 0
    var textureId = -1
        private set

    private lateinit var quadVertices: FloatBuffer
    private lateinit var quadTexCoordTransformed: FloatBuffer
    
    private val ndcCoords = floatArrayOf(
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    )
    
    private val transformedTexCoords = FloatArray(8)
    private var isInitialized = false

    companion object {
        private const val TAG = "BackgroundRenderer"
        
        private val QUAD_COORDS = floatArrayOf(
            -1f, -1f, 0f,
             1f, -1f, 0f,
            -1f,  1f, 0f,
             1f,  1f, 0f
        )
        
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """
        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform samplerExternalOES sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, v_TexCoord);
            }
        """
    }

    fun createOnGlThread(context: Context) {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        quadVertices = allocateFloatBuffer(QUAD_COORDS)
        quadTexCoordTransformed = allocateFloatBuffer(8)

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        
        quadProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(it, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(it))
            }
        }
        quadPositionParam = GLES20.glGetAttribLocation(quadProgram, "a_Position")
        quadTexCoordParam  = GLES20.glGetAttribLocation(quadProgram, "a_TexCoord")
    }

    fun draw(frame: Frame) {
        if (frame.hasDisplayGeometryChanged() || !isInitialized) {
            // Using the float array version of transformCoordinates2d to avoid Buffer size issues
            frame.transformCoordinates2d(
                Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES,
                ndcCoords,
                Coordinates2d.TEXTURE_NORMALIZED,
                transformedTexCoords
            )
            quadTexCoordTransformed.clear()
            quadTexCoordTransformed.put(transformedTexCoords)
            quadTexCoordTransformed.position(0)
            isInitialized = true
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUseProgram(quadProgram)
        
        quadVertices.position(0)
        GLES20.glVertexAttribPointer(quadPositionParam, 3, GLES20.GL_FLOAT, false, 0, quadVertices)
        GLES20.glEnableVertexAttribArray(quadPositionParam)
        
        quadTexCoordTransformed.position(0)
        GLES20.glVertexAttribPointer(quadTexCoordParam, 2, GLES20.GL_FLOAT, false, 0, quadTexCoordTransformed)
        GLES20.glEnableVertexAttribArray(quadTexCoordParam)
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        GLES20.glDisableVertexAttribArray(quadPositionParam)
        GLES20.glDisableVertexAttribArray(quadTexCoordParam)
        GLES20.glDepthMask(true)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun allocateFloatBuffer(data: FloatArray): FloatBuffer =
        allocateFloatBuffer(data.size).apply {
            put(data)
            position(0) 
        }

    private fun allocateFloatBuffer(capacity: Int): FloatBuffer =
        ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
}
