package com.ragnarok.raytracing.primitive

import android.opengl.GLES32
import com.ragnarok.raytracing.utils.gen2DTextures
import com.ragnarok.raytracing.utils.genBuffer
import rangarok.com.androidpbr.utils.Shader
import java.nio.Buffer

class BufferTexture private constructor(val buffer: Buffer, val size: Int, val isInt: Boolean) {
    var texId = 0
    var bufId = 0

    companion object {
        fun create(buffer: Buffer, size: Int, isInt: Boolean = false): BufferTexture {
            return BufferTexture(buffer, size, isInt)
        }
    }

    init {
        val texs = IntArray(1)
        gen2DTextures(texs)
        texId = texs[0]

        bufId = genBuffer()
        GLES32.glBindBuffer(GLES32.GL_TEXTURE_BUFFER, bufId)
        buffer.position(0)
        GLES32.glBufferData(GLES32.GL_TEXTURE_BUFFER, size, buffer, GLES32.GL_STATIC_DRAW)
        GLES32.glBindBuffer(GLES32.GL_TEXTURE_BUFFER, 0)
    }

    fun bind(slot: Int, shader: Shader, name: String) {
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + slot)
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, texId)
        if (!isInt) {
            GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, GLES32.GL_R32F, bufId)
        } else {
            GLES32.glTexBuffer(GLES32.GL_TEXTURE_BUFFER, GLES32.GL_R32I, bufId)
        }
        shader.setInt(name, slot)
        GLES32.glBindTexture(GLES32.GL_TEXTURE_BUFFER, 0)
    }

    fun release() {
        GLES32.glDeleteBuffers(1, intArrayOf(bufId), 0)
        GLES32.glDeleteTextures(1, intArrayOf(texId), 0)
    }
}