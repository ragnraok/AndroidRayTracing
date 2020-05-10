package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rangarok.com.androidpbr.utils.*
import java.util.*
import kotlin.math.max

class SceneRenderer(private val shader: Shader?, private val camera: Camera, private val skyboxTex: Int = 0, val outputTex: Int = 0) {

    companion object {
        const val TAG = "SceneRenderer"
    }

    private var quadRenderer: QuadRenderer? = null

    private var fbo: Int = 0
    private var rbo: Int = 0

    private var startTime = -1L

    private val random = Random(System.currentTimeMillis())



    init {
        quadRenderer = QuadRenderer()

        if (outputTex > 0) {
            fbo = genFBO()
            rbo = genRBO()
        }
    }
    /**
     * render to an output texture, with preview pass input
     */
    fun render(camera: Camera, center: Vec3, previewOutput: Int, count: Int = 0) {
        val projection = glm.perspective(glm.radians(camera.zoom), (PassVariable.eachPassOutputWidth/PassVariable.eachPassOutputHeight).toFloat(), 0.1f, 1000.0f)
        val view = camera.lookAt(center)
        if (fbo != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTex)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB16F, PassVariable.eachPassOutputWidth.toInt(), PassVariable.eachPassOutputHeight.toInt(),
                0, GLES30.GL_RGB, GLES30.GL_FLOAT, null)
            setup2DTexParam()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)

            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo)
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, PassVariable.eachPassOutputWidth.toInt(), PassVariable.eachPassOutputHeight.toInt())

            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, outputTex, 0)

            viewport(PassVariable.eachPassOutputWidth.toInt(), PassVariable.eachPassOutputHeight.toInt())
            clearGL()
        }

        shader?.apply {
            enable()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, previewOutput)
            setInt("previous", 0)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, skyboxTex)
            setInt("skybox", 1)

            val model = Mat4(1.0)
            val modelViewProjection = projection * view
            setMat4("projection", projection)
            setMat4("view", view)
            setMat4("model", model)

            val eye = center
            setVec3("eye", eye)

            setMat4("cameraWorldMatrix", camera.getWorldMatrix(center))
            setFloat("cameraAspect", (PassVariable.eachPassOutputWidth / PassVariable.eachPassOutputHeight).toFloat())
            setFloat("cameraFov", camera.getVerticalFovRadian())
            setFloat("cameraAperture", 0.06f)
            setFloat("cameraFocusLength", 2.0f)

            setInt("frame", count)

            setVec2("ran", Vec2(random.nextFloat(), random.nextFloat()))


            val weight = count / (count + 1).toFloat()
            setFloat("weight", weight)

            var time = 0.0
            if (startTime < 0) {
                startTime = System.currentTimeMillis()
            }
            time = (System.currentTimeMillis() - time) / 10.0

            setFloat("time", time.toFloat())

            Log.i(TAG, "render weight:$weight, time:$time, count:$count")

            quadRenderer?.render()
        }


        GLES30.glFinish()

        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

}