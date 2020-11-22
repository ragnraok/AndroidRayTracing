package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.model.Camera
import com.ragnarok.raytracing.primitive.QuadRenderer
import com.ragnarok.raytracing.utils.*
import glm_.vec2.Vec2
import rangarok.com.androidpbr.utils.*
import java.util.*
import kotlin.collections.HashMap

class PingPongRenderer(private val shader: Shader?, val outputTex: Int = 0) {

    companion object {
        const val TAG = "PingPongRenderer"
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
    fun render(count: Int = 0, previewOutput: Int, texturesData: HashMap<String, Int> = HashMap()) {
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

            activeTexture(previewOutput, 0)
            setInt("previous", 0)

            var slot = 1
            texturesData.forEach {
                val name = it.key
                val tex = it.value
                activeTexture(tex, slot)
                setInt(name, slot)
                slot++
            }

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

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

}