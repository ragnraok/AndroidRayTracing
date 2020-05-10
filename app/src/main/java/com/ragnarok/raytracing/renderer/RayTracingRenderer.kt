package com.ragnarok.raytracing.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.ragnarok.raytracing.glsl.*
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import glm_.glm
import glm_.vec3.Vec3
import rangarok.com.androidpbr.renderer.SkyboxCalcTex
import rangarok.com.androidpbr.utils.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RayTracingRenderer(private val context: Context, scene: Int) : GLSurfaceView.Renderer {

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

    private var hdrTex: Int = 0

    private val camera: Camera

    private val textures = IntArray(2)

    private var renderCount = 0

    private val fs: String

    private var needToneMapping = false

    init {
        when (scene) {
            Scenes.CORNELL_BOX -> {
                camera = Camera(Vec3(0.0, 0.0, 2.5), 30.0f)
                fs = tracerFs(cornellBox)
                needToneMapping = false
            }
            Scenes.PBR_SPHERE -> {
                camera = Camera(Vec3(0.0, 2.0, 2.5), 30.0f)
                camera.aperture = 0.06f
                camera.focusLength = 2.0f
                fs = tracerFs(spherePlane)
                needToneMapping = true
            }
            Scenes.GLASS -> {
                camera = Camera(Vec3(0.0, 0.35, 2.0), 30.0f)
                fs = tracerFs(glassMaterials)
                needToneMapping = true
            }
            else -> {
                camera = Camera(Vec3(0.0, 0.0, 3.0), 30.0f)
                fs = tracerFs(cornellBox)
            }

        }
    }

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

        hdrTex = uploadTexture(context, "envs/newport_loft.png")

        textures.fill(0)
        gen2DTextures(textures)
        rayTracingShader = Shader(tracerVs, fs)

        pingRenderer = SceneRenderer(rayTracingShader, camera, skyboxTex = hdrTex, outputTex = textures[0])
        pongRenderer = SceneRenderer(rayTracingShader, camera, skyboxTex = hdrTex, outputTex = textures[1])

        outputShader = Shader(outputVs, outputFs)
        outputRenderer = QuadRenderer()


    }

    private fun renderFrame() {
        // render ray tracing scene
//        val projection = glm.perspective(glm.radians(camera.zoom), (PassVariable.eachPassOutputWidth/PassVariable.eachPassOutputHeight).toFloat(), 0.1f, 1000.0f)
//        val view = camera.lookAt(Vec3(0))

        val center = Vec3(0)
        pingRenderer?.render(camera, center, pongRenderer?.outputTex?:0, renderCount)
        pongRenderer?.render(camera, center, pingRenderer?.outputTex?:0, renderCount)

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

                setInt("toneMapping", if (needToneMapping) 1 else 0)
                outputRenderer?.render()

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            }
        }
    }

    fun frameCount() = renderCount
}