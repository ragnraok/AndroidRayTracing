package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rangarok.com.androidpbr.utils.*
import kotlin.math.max

class SceneRenderer(private val shader: Shader?, private val camera: Camera, val outputTex: Int = 0) {

    companion object {
        const val TAG = "SceneRenderer"
    }

    private var quadRenderer: QuadRenderer? = null

    private var fbo: Int = 0
    private var rbo: Int = 0

    private var startTime = -1L

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
    fun render(projection: Mat4, view: Mat4, previewOutput: Int, count: Int = 0) {
        if (fbo != 0) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, outputTex)
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB16F, PassConstants.eachPassOutputWidth.toInt(), PassConstants.eachPassOutputHeight.toInt(),
                0, GLES30.GL_RGB, GLES30.GL_FLOAT, null)
            setup2DTexParam()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)

            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)

            GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, rbo)
            GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT24, PassConstants.eachPassOutputWidth.toInt(), PassConstants.eachPassOutputHeight.toInt())

            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, outputTex, 0)

            viewport(PassConstants.eachPassOutputWidth.toInt(), PassConstants.eachPassOutputHeight.toInt())
            clearGL()
        }

        shader?.apply {
            enable()
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, previewOutput)
            setInt("previous", 0)

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

            setInt("frame", count)


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

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    private fun getEyeRay(modelViewProjection: Mat4, screenPos: Vec2, eyeCenter: Vec3): Vec3 {
        val randomVec = Vec3(Math.random(), Math.random(), Math.random()) * (1 / max(PassConstants.eachPassOutputHeight, PassConstants.eachPassOutputWidth))
        Log.i(TAG, "randomVec:$randomVec, width:${PassConstants.eachPassOutputWidth}, height:${PassConstants.eachPassOutputHeight}")
        val jitterMVP = glm.translate(modelViewProjection, randomVec).inverse()
        val inv = jitterMVP * Vec4(screenPos.x, screenPos.y, 0, 1)
        return inv.div(inv.w).toVec3() - eyeCenter
    }

}