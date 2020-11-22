package com.ragnarok.raytracing.renderer

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLES32.GL_MAX_TEXTURE_BUFFER_SIZE
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.ragnarok.raytracing.glsl.*
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.BufferTexture
import com.ragnarok.raytracing.primitive.QuadRenderer
import com.ragnarok.raytracing.renderer.bvh.BVH
import com.ragnarok.raytracing.utils.*
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import de.javagl.obj.ReadableObj
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import rangarok.com.androidpbr.utils.Shader
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class ModelRenderer(private val context: Context, private val modelAssetPath: String, private var view: GLSurfaceView?) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "ModelRenderer"
    }

    private var uiHandler = Handler(Looper.getMainLooper())

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

    private var fs: String = ""

    private var needToneMapping = false

    private val center = Vec3(0)

    private var obj: ReadableObj? = null
    private var bvh: BVH? = null

    private var bvhDataBuffer: HashMap<String, BufferTexture> = HashMap()
    private var textureUniformSlot = 0

    @Volatile private var initedRendered = false
    @Volatile private var surfaceReady = false
    private val pendingJob: ConcurrentLinkedQueue<()->Unit> = ConcurrentLinkedQueue()

    init {
        camera = Camera(Vec3(0.0, 0.0, 2.5), 30.0f)
        camera.shutterOpenTime = 0.0f
        camera.shutterCloseTime = 1.0f
        needToneMapping = true

        thread {
            val tick = currentTick()
            readModelData()

            uiHandler.post {
                Toast.makeText(context, "read model data finished, cost:${tickToNowMs(tick)}ms", Toast.LENGTH_LONG).show()
            }

            post {
                initRenderer()
                initedRendered = true
            }
        }
    }

    private fun readModelData() {
        obj = ObjUtils.convertToRenderable(ObjReader.read(context.assets.open(modelAssetPath)))
        obj?.let {
            bvh = BVH(it).apply {
                buildBVH()
                fs = bvhTraceFS(verticesArray.size, bvhMinFlatArray.size)
            }
        }
        // TODO: how pass bvh data to shader?
        // by texture data?
        // by uniform array? (may be better for mobile platform)

        // TODO: intersect ray->bvh in fragment shader
    }

    override fun onDrawFrame(gl: GL10?) {
        clearGL()
        flushPendingJobs()
        if (initedRendered) {
            renderFrame()
        }
    }

    private fun flushPendingJobs() {
        while (pendingJob.isNotEmpty()) {
            val job = pendingJob.peek()
            job()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "onSurfaceChanged, width:$width, height:$height")
        viewport(width, height)
        this.width = width
        this.height = height
        surfaceReady = true
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "onSurfaceCreated")
    }

    private fun initRenderer() {
        Log.i(TAG, "start init renderer")
        textures.fill(0)
        gen2DTextures(textures)
        rayTracingShader = Shader(traceVS, fs)

        // slot 0: outputTex
        pingRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[0])
        pongRenderer = PingPongRenderer(shader = rayTracingShader, outputTex = textures[1])

        setShaderInput()
        clearGLBufferStatus()

        outputShader = Shader(outputVs, outputFs)
        outputRenderer = QuadRenderer()
        clearGLBufferStatus()

        val maxTextureBufferSize = IntArray(1)
        GLES32.glGetIntegerv(GL_MAX_TEXTURE_BUFFER_SIZE, maxTextureBufferSize, 0)
        Log.i(TAG, "maxTextureBufferSize:${maxTextureBufferSize[0]}")
    }

    private fun setShaderInput() {
        skyboxTex = uploadTexture(context, "envs/newport_loft.png")
        clearGLBufferStatus()
        // slot 1: skybox texture
        texturesData["skybox"] = skyboxTex
        textureUniformSlot = 1

        rayTracingShader?.apply {
            enable()
            setCommonShaderInput(this)
            setupBVHBufferInput(this)

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

    @Deprecated("try buffer texture now")
    private fun setupBVHShaderInput(shader: Shader) {
        //TODO: exceed uniform array size limit try another way to pass bvh data to shader
        //maybe use buffer texture object:
        //https://www.khronos.org/opengl/wiki/Buffer_Texture
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

    private fun setupBVHBufferInput(shader: Shader) {
        // https://www.khronos.org/opengl/wiki/Buffer_Texture
        bvh?.let {
            // buffer texture slot start from 2
            var slot = textureUniformSlot + 1

            val verticesBuffer = FloatBuffer.allocate(it.verticesArray.size * 3)
            for (vertex in it.verticesArray) {
                verticesBuffer.put(vertex.x)
                verticesBuffer.put(vertex.y)
                verticesBuffer.put(vertex.z)
            }
            val verticesBufferTexture = BufferTexture.create(verticesBuffer, it.verticesArray.size * 3)
            verticesBufferTexture.bind(slot++, shader, "verticesBuffer")
            bvhDataBuffer["verticesBuffer"] = verticesBufferTexture

            val minFlatBuffer = FloatBuffer.allocate(it.bvhMinFlatArray.size * 3)
            for (min in it.bvhMinFlatArray) {
                minFlatBuffer.put(min.x)
                minFlatBuffer.put(min.y)
                minFlatBuffer.put(min.z)
            }
            val minFlatBufferTexture = BufferTexture.create(minFlatBuffer, it.bvhMinFlatArray.size * 3)
            minFlatBufferTexture.bind(slot++, shader, "bvhMinBoundsBuffer")
            bvhDataBuffer["bvhMinBoundsBuffer"]

            val maxFlatBuffer = FloatBuffer.allocate(it.bvhMaxFlatArray.size * 3)
            for (max in it.bvhMaxFlatArray) {
                maxFlatBuffer.put(max.x)
                maxFlatBuffer.put(max.y)
                maxFlatBuffer.put(max.z)
            }
            val maxFlatBufferTexture = BufferTexture.create(maxFlatBuffer, it.bvhMaxFlatArray.size * 3)
            maxFlatBufferTexture.bind(slot++, shader, "bvhMaxBoundsBuffer")
            bvhDataBuffer["bvhMaxBoundsBuffer"] = maxFlatBufferTexture

            val triangleIndexBuffer = IntBuffer.wrap(it.bvhTriangleIndexArray.toIntArray())
            val triangleIndexBufferTexture = BufferTexture.create(triangleIndexBuffer, it.bvhTriangleIndexArray.size, isInt = true)
            triangleIndexBufferTexture.bind(slot, shader, "bvhTriangleIndexBuffer")
            bvhDataBuffer["bvhTriangleIndexBuffer"] = triangleIndexBufferTexture

            Log.i(TAG, "finished set bvh data buffer texture object, final texture slot:$slot")
            textureUniformSlot = slot

        }
    }


    private fun renderFrame() {
        // render ray tracing scene
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

    private fun post(r: ()->Unit) {
        if (!surfaceReady) {
            pendingJob.add(r)
        } else {
            view?.queueEvent(r)
        }
    }

    fun frameCount() = renderCount

    fun detach() {
        this.view = null
    }

}