package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.glsl.skyBoxFs
import com.ragnarok.raytracing.glsl.skyBoxVs
import com.ragnarok.raytracing.utils.SkyBoxVertices
import glm_.mat4x4.Mat4
import rangarok.com.androidpbr.utils.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Skybox {

    var skyBoxVAO = 0

    var cubeMapTexture = 0

    var skyBoxShader = Shader(
        skyBoxVs,
        skyBoxFs
    )

    fun initWithSkyboxTex(texId: Int) {
        this.cubeMapTexture = texId
        Log.i(TAG, "initWithSkyboxTex: $texId")
    }

    fun render(projection: Mat4, view: Mat4) {
        if (skyBoxVAO == 0) {
            val vaoArray = intArrayOf(0)
            GLES30.glGenVertexArrays(1, vaoArray, 0)
            skyBoxVAO = vaoArray[0]

            val vboArray = intArrayOf(0)
            GLES30.glGenBuffers(1, vboArray, 0)
            val vbo = vboArray[0]

            GLES30.glBindVertexArray(skyBoxVAO)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)

            val buffer = ByteBuffer.allocateDirect(SkyBoxVertices.size * 4).
                order(ByteOrder.nativeOrder()).asFloatBuffer().put(SkyBoxVertices)
            buffer.position(0)
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, SkyBoxVertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
            GLES30.glBindVertexArray(0)

            Log.i(TAG, "init skyBoxVAO:$skyBoxVAO")
        }

        skyBoxShader.enable()
        skyBoxShader.setMat4("projection", projection)
        skyBoxShader.setMat4("view", view)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glBindVertexArray(skyBoxVAO)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, cubeMapTexture)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        GLES30.glBindVertexArray(0)
        GLES30.glDepthFunc(GLES30.GL_LESS)

        Log.i(TAG, "draw skybox:$skyBoxVAO")
    }

    companion object {
        const val TAG = "Skybox"
    }
}