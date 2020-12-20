package com.ragnarok.raytracing.primitive

import android.opengl.GLES31Ext
import android.opengl.GLES32
import com.ragnarok.raytracing.utils.gen2DTextures
import com.ragnarok.raytracing.utils.genBuffer
import glm_.BYTES
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
        GLES32.glBindBuffer(GLES31Ext.GL_TEXTURE_BUFFER_EXT, bufId)
        buffer.position(0)
        val byteSize = size * if (isInt) Int.BYTES else Float.BYTES
        GLES32.glBufferData(GLES31Ext.GL_TEXTURE_BUFFER_EXT, byteSize, buffer, GLES32.GL_STATIC_DRAW)
        GLES32.glBindBuffer(GLES31Ext.GL_TEXTURE_BUFFER_EXT, 0)
    }

    fun bind(slot: Int, shader: Shader, name: String) {
        GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + slot)
        GLES32.glBindTexture(GLES31Ext.GL_TEXTURE_BUFFER_EXT, texId)
        if (!isInt) {
            GLES32.glTexBuffer(GLES31Ext.GL_TEXTURE_BUFFER_EXT, GLES32.GL_R32F, bufId)
        } else {
            GLES32.glTexBuffer(GLES31Ext.GL_TEXTURE_BUFFER_EXT, GLES32.GL_R32I, bufId)
        }
        shader.setInt(name, slot)
    }

    fun release() {
        GLES32.glDeleteBuffers(1, intArrayOf(bufId), 0)
        GLES32.glDeleteTextures(1, intArrayOf(texId), 0)
    }
}