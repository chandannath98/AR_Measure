package com.armeasure.app.ar

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Pose
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders anchor dots and the line segment between two anchors.
 */
class AnchorDotRenderer {

    private var program = 0
    private var positionAttr = 0
    private var colorUniform = 0
    private var modelViewProjectionUniform = 0
    private var pointSizeUniform = 0

    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    private lateinit var dotBuffer: FloatBuffer
    private lateinit var lineBuffer: FloatBuffer

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_ModelViewProjection;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ModelViewProjection * a_Position;
                gl_PointSize = u_PointSize;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                float d = distance(gl_PointCoord, vec2(0.5));
                if (d > 0.5) discard;
                gl_FragColor = u_Color;
            }
        """
    }

    fun createOnGlThread() {
        val vs = compile(GLES20.GL_VERTEX_SHADER,   VERTEX_SHADER)
        val fs = compile(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        positionAttr             = GLES20.glGetAttribLocation(program,  "a_Position")
        colorUniform             = GLES20.glGetUniformLocation(program, "u_Color")
        modelViewProjectionUniform = GLES20.glGetUniformLocation(program, "u_ModelViewProjection")
        pointSizeUniform         = GLES20.glGetUniformLocation(program, "u_PointSize")

        dotBuffer  = floatBuffer(3)   // x, y, z
        lineBuffer = floatBuffer(6)   // 2 × (x, y, z)
    }

    fun drawDot(pose: Pose, view: FloatArray, proj: FloatArray, isStart: Boolean) {
        pose.toMatrix(modelMatrix, 0)
        Matrix.multiplyMM(modelViewMatrix, 0, view, 0, modelMatrix, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, proj, 0, modelViewMatrix, 0)

        dotBuffer.put(0, pose.tx()); dotBuffer.put(1, pose.ty()); dotBuffer.put(2, pose.tz())
        dotBuffer.position(0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)
        GLES20.glUniform1f(pointSizeUniform, 48f)
        if (isStart) GLES20.glUniform4f(colorUniform, 0f, 0.85f, 1f, 1f)  // cyan
        else         GLES20.glUniform4f(colorUniform, 1f, 0.5f, 0f, 1f)   // orange

        // Build identity MVP with translation only for world-space dot
        val identProj = FloatArray(16)
        Matrix.setIdentityM(identProj, 0)
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, modelViewProjectionMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glVertexAttribPointer(positionAttr, 3, GLES20.GL_FLOAT, false, 0, dotBuffer)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
        GLES20.glDisableVertexAttribArray(positionAttr)
    }

    fun drawLine(start: Pose, end: Pose, view: FloatArray, proj: FloatArray) {
        lineBuffer.put(0, start.tx()); lineBuffer.put(1, start.ty()); lineBuffer.put(2, start.tz())
        lineBuffer.put(3, end.tx());   lineBuffer.put(4, end.ty());   lineBuffer.put(5, end.tz())
        lineBuffer.position(0)

        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(modelViewProjectionUniform, 1, false, mvp, 0)
        GLES20.glUniform4f(colorUniform, 1f, 0.95f, 0.3f, 1f)  // yellow
        GLES20.glUniform1f(pointSizeUniform, 0f)

        GLES20.glLineWidth(6f)
        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glVertexAttribPointer(positionAttr, 3, GLES20.GL_FLOAT, false, 0, lineBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
        GLES20.glDisableVertexAttribArray(positionAttr)
    }

    private fun compile(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also { GLES20.glShaderSource(it, src); GLES20.glCompileShader(it) }

    private fun floatBuffer(capacity: Int): FloatBuffer =
        ByteBuffer.allocateDirect(capacity * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
}
