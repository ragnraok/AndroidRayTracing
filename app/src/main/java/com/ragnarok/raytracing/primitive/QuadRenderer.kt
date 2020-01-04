package com.ragnarok.raytracing.primitive

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.utils.QuadVertices
import rangarok.com.androidpbr.utils.genBuffer
import rangarok.com.androidpbr.utils.genVAO
import java.nio.ByteBuffer
import java.nio.ByteOrder

class QuadRenderer : PrimitiveRenderer {

    private var vao = 0

    override fun render() {
        if (vao == 0) {
            vao = genVAO()

            val vbo = genBuffer()

            GLES30.glBindVertexArray(vao)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)

            val buffer = ByteBuffer.allocateDirect(QuadVertices.size * 4).order(ByteOrder.nativeOrder()).
                asFloatBuffer().put(QuadVertices)
            buffer.position(0)
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, QuadVertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0)

            GLES30.glEnableVertexAttribArray(1)
            GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4)

            GLES30.glBindVertexArray(0)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

            Log.i(TAG, "finish setup vao: $vao")
        }
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
//        Log.i(TAG, "render quad")
    }

    companion object {
        const val TAG = "QuadRenderer"
    }

}