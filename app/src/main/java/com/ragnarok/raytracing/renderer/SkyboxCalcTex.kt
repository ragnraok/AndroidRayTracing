package rangarok.com.androidpbr.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.glsl.CubeMapConversionFs
import com.ragnarok.raytracing.glsl.CubeMapVs
import com.ragnarok.raytracing.primitive.CubeRenderer
import com.ragnarok.raytracing.utils.*
import rangarok.com.androidpbr.utils.*

class SkyboxCalcTex(hdrTexture: Int) {

    companion object {
        const val TAG = "SkyboxCalcTex"
    }

    private val shader = Shader(vertex = CubeMapVs, fragment = CubeMapConversionFs)

    private val texId: Int

    private val cubeRenderer = CubeRenderer()

    init {
        val texArray = intArrayOf(0)
        GLES30.glGenTextures(1, texArray, 0)
        texId = texArray[0]

        val fbo = genFBO()
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fbo)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, texId)

        for (i in 0 until 6) {
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GLES30.GL_RGB16F, 512, 512, 0, GLES30.GL_RGB, GLES30.GL_FLOAT, null)
        }
        setCubemapTexParam()
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_CUBE_MAP)


        shader.enable()
        shader.setInt("equirectangularMap", 0)
        shader.setMat4("projection", cubemapProjection)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, hdrTexture)

        viewport(512, 512)

        for (i in 0 until 6) {
            shader.setMat4("view", cubemapViews[i])
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, texId, 0)
            clearGL()
            cubeRenderer.render()
        }

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        Log.i(TAG, "finish draw skybox texId:$texId")
    }

    fun texId(): Int {
        return texId
    }
}