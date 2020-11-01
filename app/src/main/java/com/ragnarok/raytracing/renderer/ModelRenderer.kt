package com.ragnarok.raytracing.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.ragnarok.raytracing.glsl.*
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import com.ragnarok.raytracing.renderer.bvh.BVH
import com.ragnarok.raytracing.scenes.cornellBox
import com.ragnarok.raytracing.utils.*
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import de.javagl.obj.ReadableObj
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import rangarok.com.androidpbr.utils.Shader
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ModelRenderer(private val context: Context, private val modelAssetPath: String) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "ModelRenderer"
    }

    private var width = 0
    private var height = 0

    private val texturesData = HashMap<String, Int>()

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

    private var obj: ReadableObj? = null
    private var bvh: BVH? = null

    init {
        camera = Camera(Vec3(0.0, 0.0, 2.5), 30.0f)
        fs = traceFS(cornellBox)
        camera.shutterOpenTime = 0.0f
        camera.shutterCloseTime = 1.0f
        needToneMapping = true

        readModelData()
    }

    private fun readModelData() {
        obj = ObjUtils.convertToRenderable(ObjReader.read(context.assets.open(modelAssetPath)))
        obj?.let {
            bvh = BVH(it)
            bvh?.buildBVH()
        }
        // TODO: how pass bvh data to shader?
        // by texture data?
        // by uniform array? (may be better for mobile platform)

        // TODO: intersect ray->bvh in fragment shader
    }

    override fun onDrawFrame(gl: GL10?) {
        clearGL()
        renderFrame()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(RayTracingRenderer.TAG, "onSurfaceChanged, width:$width, height:$height")
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
        rayTracingShader = Shader(traceVS, fs)
        setShaderInput()
        clearGLBufferStatus()

        skyboxTex = uploadTexture(context, "envs/newport_loft.png")
        clearGLBufferStatus()

        pingRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[0])
        pongRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[1])

        outputShader = Shader(outputVs, outputFs)
        outputRenderer = QuadRenderer()
        clearGLBufferStatus()
    }

    private fun setShaderInput() {
        rayTracingShader?.apply {
            enable()
            setCommonShaderInput(this)
            setupBVHShaderInput(this)

            disable()
        }
    }

    private fun setCommonShaderInput(shader: Shader) {
        shader.apply {
            val model = Mat4(1.0)
            val projection = glm.perspective(
                glm.radians(camera.zoom),
                (PassVariable.eachPassOutputWidth / PassVariable.eachPassOutputHeight).toFloat(),
                0.1f,
                1000.0f
            )
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
        }
    }

    private fun setupBVHShaderInput(shader: Shader) {
        shader.apply {
            bvh?.let {
                setVec3Array("vertices", it.verticesArray.toTypedArray())
                setVec3Array("normals", it.normalArray.toTypedArray())
                setVec3Array("bvhMinBounds", it.bvhMinFlatArray.toTypedArray())
                setVec3Array("bvhMaxBounds", it.bvhMaxFlatArray.toTypedArray())
                setIntArray("bvhTriangleIndex", it.bvhTriangleIndexArray.toTypedArray())
            }
        }
    }

    private fun renderFrame() {
        // render ray tracing scene

//        setShaderInput()
        pingRenderer?.render(renderCount, pongRenderer?.outputTex?:0, texturesData)
        pongRenderer?.render(renderCount, pingRenderer?.outputTex?:0, texturesData)

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