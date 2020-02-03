package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.ragnarok.raytracing.glsl.*
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import glm_.glm
import glm_.vec3.Vec3
import rangarok.com.androidpbr.utils.Shader
import rangarok.com.androidpbr.utils.clearGL
import rangarok.com.androidpbr.utils.gen2DTextures
import rangarok.com.androidpbr.utils.viewport
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RayTracingRenderer : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "RayTracingRenderer"
    }

    private var width = 0
    private var height = 0

    private var rayTracingShader: Shader? = null
    private var pingRenderer: SceneRenderer? = null
    private var pongRenderer: SceneRenderer? = null

    private var outputShader: Shader? = null
    private var outputRenderer: QuadRenderer? = null

    private val camera = Camera(Vec3(0.0, 0.0, 3.0))

    private val textures = IntArray(2)

    private var renderCount = 0

    override fun onDrawFrame(gl: GL10?) {
        clearGL()
        renderFrame()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged, width:$width, height:$height")
        viewport(width, height)
        this.width = width
        this.height = height

        initRenderer()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "onSurfaceCreated")

    }

    private fun initRenderer() {
        textures.fill(0)
        gen2DTextures(textures)
        rayTracingShader = Shader(tracerVs, tracerFs)

        pingRenderer = SceneRenderer(rayTracingShader, camera, textures[0])
        pongRenderer = SceneRenderer(rayTracingShader, camera, textures[1])

        outputShader = Shader(outputVs, outputFs)
        outputRenderer = QuadRenderer()


    }

    private fun renderFrame() {
        // render ray tracing scene
        val projection = glm.perspective(glm.radians(camera.zoom), (PassVariable.eachPassOutputWidth/PassVariable.eachPassOutputHeight).toFloat(), 0.1f, 1000.0f)
        var view = camera.lookAt(Vec3(0))

        pingRenderer?.render(projection, view, pongRenderer?.outputTex?:0, renderCount)
        pongRenderer?.render(projection, view, pingRenderer?.outputTex?:0, renderCount)

        renderCount++

        clearGL()

        val outputTex = pongRenderer?.outputTex ?: 0

        if (outputTex > 0) {
            viewport(width, height)
            // render output
            outputShader?.apply {
                enable()
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTex)
                setInt("texture", 0)
                outputRenderer?.render()

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            }
        }
    }

    fun frameCount() = renderCount
}