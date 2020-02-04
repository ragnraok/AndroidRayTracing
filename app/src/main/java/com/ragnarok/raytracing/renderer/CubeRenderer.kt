package com.ragnarok.raytracing.renderer

import android.opengl.GLES30
import android.util.Log
import com.ragnarok.raytracing.primitive.PrimitiveRenderer
import com.ragnarok.raytracing.utils.CubeVertices
import rangarok.com.androidpbr.utils.genBuffer
import rangarok.com.androidpbr.utils.genVAO
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CubeRenderer : PrimitiveRenderer {

    private var cubeVAO = 0
    private var cubeVBO = 0

    override fun render() {
        if (cubeVAO == 0) {
            cubeVAO = genVAO()

            cubeVBO = genBuffer()

            // fill buffer
            GLES30.glBindVertexArray(cubeVAO)
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, cubeVBO)
            val buffer =
                ByteBuffer.allocateDirect(CubeVertices.size * 4).order(ByteOrder.nativeOrder())
                    .asFloatBuffer().put(
                        CubeVertices
                    )
            buffer.position(0)
            GLES30.glBufferData(
                GLES30.GL_ARRAY_BUFFER,
                CubeVertices.size * 4,
                buffer,
                GLES30.GL_STATIC_DRAW
            )

            // position
            GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 8 * 4, 0)
            GLES30.glEnableVertexAttribArray(0)

            // texCoords
            GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)
            GLES30.glEnableVertexAttribArray(1)

            // normals
            GLES30.glEnableVertexAttribArray(2)
            GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 8 * 4, 6 * 4)

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
            GLES30.glBindVertexArray(0)

            Log.i(TAG, "finish setup cubeVAO:$cubeVAO")
        }

        GLES30.glBindVertexArray(cubeVAO)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 36)
        GLES30.glBindVertexArray(0)
        Log.i(TAG, "finish draw cube: $cubeVAO")
    }

    companion object {
        const val TAG = "CubeRenderer"
    }

}