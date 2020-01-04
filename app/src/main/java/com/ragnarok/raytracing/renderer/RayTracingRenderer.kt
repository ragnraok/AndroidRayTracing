package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rangarok.com.androidpbr.utils.Shader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RayTracingRenderer : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "RayTracingRenderer"
    }

    private var width = 0
    private var height = 0

    private var shader: Shader? = null
    private var quadRenderer: QuadRenderer? = null

    private val camera = Camera(Vec3(0.0, 0.0, 2.5))

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glClearColor(0.75f, 0.75f, 0.75f, 1.0f)
        renderFrame()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged, width:$width, height:$height")
        GLES30.glViewport(0, 0, width, height)
        this.width = width
        this.height = height

    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "onSurfaceCreated")

        initRenderer()
    }

    private fun initRenderer() {
        shader = Shader(tracerVs, tracerFs)
        quadRenderer = QuadRenderer()
    }

    private fun renderFrame() {
        val projection = glm.perspective(glm.radians(camera.zoom), width/height.toFloat(), 0.1f, 1000.0f)
        val view = camera.lookAt(Vec3(0))
        shader?.apply {
            enable()
            var model = Mat4(1.0)
            val modelViewProjection = projection * view
            setMat4("projection", projection)
            setMat4("view", view)
            setMat4("model", model)

            // generate ray00/ray01/ray10/ray11
            val eye = camera.position
            setVec3("eye", eye)
            setVec3("ray00", getEyeRay(modelViewProjection, Vec2(-1, -1), eye))
            setVec3("ray01", getEyeRay(modelViewProjection, Vec2(-1, 1), eye))
            setVec3("ray10", getEyeRay(modelViewProjection, Vec2(1, -1), eye))
            setVec3("ray11", getEyeRay(modelViewProjection, Vec2(1, 1), eye))

        }

        quadRenderer?.render()
    }

    private fun getEyeRay(modelViewProjection: Mat4, screenPos: Vec2, eyeCenter: Vec3): Vec3 {
        //TODO: randomization process
        val inv = modelViewProjection.inverse() * Vec4(screenPos.x, screenPos.y, 0, 1)
        return inv.div(inv.w).toVec3() - eyeCenter
    }

}