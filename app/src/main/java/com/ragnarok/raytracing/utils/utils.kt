package rangarok.com.androidpbr.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils
import android.os.SystemClock

fun clearGL() {
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    GLES30.glClearColor(0.0f, 0f, 0.0f, 1.0f)
}

fun viewport(width: Int, height: Int) {
    GLES30.glViewport(0, 0, width, height)
}

fun genVAO(): Int {
    val vaoArray = intArrayOf(0)
    GLES30.glGenVertexArrays(1, vaoArray, 0)
    return vaoArray[0]
}

fun genFBO(): Int  {
    val fboArray = intArrayOf(0)
    GLES30.glGenFramebuffers(1, fboArray, 0)
    return fboArray[0]
}

fun genBuffer(): Int {
    val vboArray = intArrayOf(0)
    GLES30.glGenBuffers(1, vboArray, 0)
    return vboArray[0]
}

fun setup2DTexParam() {
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
}

fun setCubemapTexParam(withMipmap: Boolean = false) {
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_CUBE_MAP,
        GLES30.GL_TEXTURE_MIN_FILTER,
        if(withMipmap)  GLES30.GL_LINEAR_MIPMAP_LINEAR else  GLES30.GL_LINEAR
    )
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_CUBE_MAP,
        GLES30.GL_TEXTURE_MAG_FILTER,
        GLES30.GL_LINEAR
    )
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_CUBE_MAP,
        GLES30.GL_TEXTURE_WRAP_R,
        GLES30.GL_CLAMP_TO_EDGE
    )
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_CUBE_MAP,
        GLES30.GL_TEXTURE_WRAP_S,
        GLES30.GL_CLAMP_TO_EDGE
    )
    GLES30.glTexParameteri(
        GLES30.GL_TEXTURE_CUBE_MAP,
        GLES30.GL_TEXTURE_WRAP_T,
        GLES30.GL_CLAMP_TO_EDGE
    )

}

fun uploadTexture(context: Context, path: String): Int {
    val texIdArray = intArrayOf(0)
    GLES30.glGenTextures(1, texIdArray, 0)
    val texId = texIdArray[0]
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
    setup2DTexParam()
    val bitmap = BitmapFactory.decodeStream(context.assets.open(path))
    GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap,0)
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
    return texId
}

fun activeTexture(texId: Int, slot: Int) {
    GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + slot)
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
}

fun currentTick(): Long {
    return SystemClock.elapsedRealtime()
}

fun tickToNowMs(tick: Long): Long {
    return SystemClock.elapsedRealtime() - tick
}