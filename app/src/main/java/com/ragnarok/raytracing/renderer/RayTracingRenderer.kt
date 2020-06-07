package com.ragnarok.raytracing.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.ragnarok.raytracing.glsl.*
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import com.ragnarok.raytracing.scenes.cornellBox
import com.ragnarok.raytracing.scenes.glassMaterials
import com.ragnarok.raytracing.scenes.spherePlane
import com.ragnarok.raytracing.scenes.texture_spheres
import com.ragnarok.raytracing.utils.*
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import rangarok.com.androidpbr.utils.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RayTracingRenderer(private val context: Context, private val scene: Int) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "RayTracingRenderer"
    }

    private var width = 0
    private var height = 0

    private var rayTracingShader: Shader? = null
    private var pingRenderer: PingPongRenderer? = null
    private var pongRenderer: PingPongRenderer? = null

    private var outputShader: Shader? = null
    private var outputRenderer: QuadRenderer? = null

    private var skyboxTex: Int = 0

    private val camera: Camera

    private val textures = IntArray(2)

    private var renderCount = 0

    private val fs: String

    private var needToneMapping = false

    private val center = Vec3(0)

    init {
        when (scene) {
            Scenes.CORNELL_BOX -> {
                camera = Camera(Vec3(0.0, 0.0, 2.5), 30.0f)
                fs = tracerFs(cornellBox)
                camera.shutterOpenTime = 0.0f
                camera.shutterCloseTime = 1.0f
                needToneMapping = false
            }
            Scenes.PBR_SPHERE -> {
                camera = Camera(Vec3(0.0, 2.0, 2.5), 30.0f)
                fs = tracerFs(spherePlane)
                needToneMapping = true
            }
            Scenes.PBR_SPHERE_DOF -> {
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
            Scenes.TEXTURE_SPHERE -> {
                camera = Camera(Vec3(0.0, 1.0, 3.0), 30.0f)
                fs = tracerFs(texture_spheres)
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

        textures.fill(0)
        gen2DTextures(textures)
        rayTracingShader = Shader(tracerVs, fs)
        clearGLBufferStatus()

        skyboxTex = uploadTexture(context, "envs/newport_loft.png")
        clearGLBufferStatus()

        pingRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[0])
        pongRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[1])

        outputShader = Shader(outputVs, outputFs)
        outputRenderer = QuadRenderer()
        clearGLBufferStatus()
    }

    private fun setCommonShaderInput() {
        rayTracingShader?.apply {
            enable()

            activeTexture(skyboxTex, 1)
            setInt("skybox", 1)

            val model = Mat4(1.0)
            val projection = glm.perspective(glm.radians(camera.zoom), (PassVariable.eachPassOutputWidth/PassVariable.eachPassOutputHeight).toFloat(), 0.1f, 1000.0f)
            val view = camera.lookAt(center)
            setMat4("projection", projection)
            setMat4("view", view)
            setMat4("model", model)

            val eye = center
            setVec3("eye", eye)

            // camera related data...
            setMat4("cameraWorldMatrix", camera.getWorldMatrix(center))
            setFloat("cameraAspect", (PassVariable.eachPassOutputWidth / PassVariable.eachPassOutputHeight).toFloat())
            setFloat("cameraFov", camera.getVerticalFovRadian())
            setFloat("cameraAperture", camera.aperture)
            setFloat("cameraFocusLength", camera.focusLength)
            setFloat("cameraShutterOpenTime", camera.shutterOpenTime)
            setFloat("cameraShutterCloseTime", camera.shutterCloseTime)

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            disable()
        }
    }

    private fun setShaderInput() {

        // texture slots:
        // 0 for skybox
        // 1 for previous output
        // other for scene textures

        setCommonShaderInput()

        when (scene) {
            Scenes.TEXTURE_SPHERE -> {
                rayTracingShader?.apply {
                    enable()
//                    setInt("textures[0].colorTex", 0);
                }
            }
        }
    }

    private fun renderFrame() {
        // render ray tracing scene

        setShaderInput()
        pingRenderer?.render(renderCount, pongRenderer?.outputTex?:0)
        pongRenderer?.render(renderCount, pingRenderer?.outputTex?:0)

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