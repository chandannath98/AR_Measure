package com.armeasure.app.ar

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.PointCloud
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

// ─────────────────────────────────────────────────────────────────────────────
// PlaneRenderer – draws a translucent grid on detected planes
// ─────────────────────────────────────────────────────────────────────────────

class PlaneRenderer {

    private var program = 0
    private var positionAttr = 0
    private var colorUniform = 0
    private var mvpUniform = 0

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            uniform mat4 u_MVP;
            void main() { gl_Position = u_MVP * a_Position; }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() { gl_FragColor = u_Color; }
        """
    }

    fun createOnGlThread(context: Context, gridTextureName: String) {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        positionAttr  = GLES20.glGetAttribLocation(program,  "a_Position")
        colorUniform  = GLES20.glGetUniformLocation(program, "u_Color")
        mvpUniform    = GLES20.glGetUniformLocation(program, "u_MVP")
    }

    fun drawPlanes(planes: Collection<Plane>, cameraPose: Pose, projMatrix: FloatArray) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        for (plane in planes) {
            if (plane.trackingState != TrackingState.TRACKING) continue
            val polygon = plane.polygon ?: continue
            drawConvexPolygon(polygon, plane.centerPose, projMatrix)
        }
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawConvexPolygon(polygon: FloatBuffer, center: Pose, proj: FloatArray) {
        // Build a simple model matrix from the plane center
        val model = FloatArray(16)
        center.toMatrix(model, 0)
        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, proj, 0, model, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0)
        GLES20.glUniform4f(colorUniform, 0.15f, 0.85f, 1f, 0.12f)  // subtle blue tint

        // Repackage polygon as 3D (y = 0 in local space)
        val vertCount = polygon.limit() / 2
        val verts3d = ByteBuffer.allocateDirect(vertCount * 3 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        for (i in 0 until vertCount) {
            verts3d.put(polygon[i * 2])
            verts3d.put(0f)
            verts3d.put(polygon[i * 2 + 1])
        }
        verts3d.position(0)

        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glVertexAttribPointer(positionAttr, 3, GLES20.GL_FLOAT, false, 0, verts3d)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertCount)
        GLES20.glDisableVertexAttribArray(positionAttr)
    }

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also { GLES20.glShaderSource(it, src); GLES20.glCompileShader(it) }
}

// ─────────────────────────────────────────────────────────────────────────────
// PointCloudRenderer – draws feature points for scanning feedback
// ─────────────────────────────────────────────────────────────────────────────

class PointCloudRenderer {

    private var program = 0
    private var positionAttr = 0
    private var colorUniform = 0
    private var mvpUniform = 0
    private var pointSizeUniform = 0

    private var pointCloud: PointCloud? = null

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MVP;
            uniform float u_PointSize;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MVP * vec4(a_Position.xyz, 1.0);
                gl_PointSize = u_PointSize;
            }
        """
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                float d = distance(gl_PointCoord, vec2(0.5));
                if (d > 0.5) discard;
                gl_FragColor = u_Color * (1.0 - d * 2.0);
            }
        """
    }

    fun createOnGlThread(context: Context) {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs)
            GLES20.glAttachShader(it, fs)
            GLES20.glLinkProgram(it)
        }
        positionAttr     = GLES20.glGetAttribLocation(program,  "a_Position")
        colorUniform     = GLES20.glGetUniformLocation(program, "u_Color")
        mvpUniform       = GLES20.glGetUniformLocation(program, "u_MVP")
        pointSizeUniform = GLES20.glGetUniformLocation(program, "u_PointSize")
    }

    fun update(pc: PointCloud) {
        pointCloud?.release()
        pointCloud = pc
    }

    fun draw(view: FloatArray, proj: FloatArray) {
        val pc = pointCloud ?: return
        val mvp = FloatArray(16)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0)
        GLES20.glUniform4f(colorUniform, 0.3f, 1f, 0.6f, 0.8f)  // green dots
        GLES20.glUniform1f(pointSizeUniform, 6f)

        GLES20.glEnableVertexAttribArray(positionAttr)
        GLES20.glVertexAttribPointer(positionAttr, 4, GLES20.GL_FLOAT, false, 0, pc.points)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pc.ids.limit())
        GLES20.glDisableVertexAttribArray(positionAttr)
    }

    private fun compileShader(type: Int, src: String): Int =
        GLES20.glCreateShader(type).also { GLES20.glShaderSource(it, src); GLES20.glCompileShader(it) }
}
