package rangarok.com.androidpbr.utils

import android.opengl.GLES30
import android.util.Log
import glm_.mat3x3.Mat3
import glm_.mat4x4.Mat4
import glm_.toInt
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import java.nio.FloatBuffer


// general shader class
class Shader(vertex: String, fragment: String) {

    private var id = 0

    init {
        val vertexShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
        GLES30.glShaderSource(vertexShader, vertex)
        GLES30.glCompileShader(vertexShader)
        checkCompileError(vertexShader, "Vertex")

        val fragmentShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
        GLES30.glShaderSource(fragmentShader, fragment)
        GLES30.glCompileShader(fragmentShader)
        checkCompileError(fragmentShader, "Fragment")

        id = GLES30.glCreateProgram()
        GLES30.glAttachShader(id, vertexShader)
        GLES30.glAttachShader(id, fragmentShader)
        GLES30.glLinkProgram(id)
        checkCompileError(id, "Program")

        GLES30.glDeleteShader(vertexShader)
        GLES30.glDeleteShader(fragmentShader)
    }

    fun enable() {
        GLES30.glUseProgram(id)
    }

    fun setBoolean(name: String, value: Boolean) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(id, name), value.toInt)
    }

    fun setInt(name: String, value: Int) {
        GLES30.glUniform1i(GLES30.glGetUniformLocation(id, name), value)
    }

    fun setFloat(name: String, value: Float) {
        GLES30.glUniform1f(GLES30.glGetUniformLocation(id, name), value)
    }

    fun setVec2(name: String, value: Vec2) {
        val array = FloatArray(2)
        GLES30.glUniform2fv(GLES30.glGetUniformLocation(id, name), 1, value to array, 0)
    }

    fun setVec3(name: String, value: Vec3) {
        val array = FloatArray(3)
        GLES30.glUniform3fv(GLES30.glGetUniformLocation(id, name), 1, value to array, 0)
    }

    fun setVec4(name: String, value: Vec4) {
        val array = FloatArray(4)
        GLES30.glUniform4fv(GLES30.glGetUniformLocation(id, name), 1, value to array, 0)
    }

    fun setMat3(name: String, value: Mat3) {
        val array = FloatBuffer.allocate(9)
        GLES30.glUniformMatrix3fv(GLES30.glGetUniformLocation(id, name), 1, false, value to array)
    }

    fun setMat4(name: String, value: Mat4) {
        val array = FloatBuffer.allocate(16)
        GLES30.glUniformMatrix4fv(GLES30.glGetUniformLocation(id, name), 1, false, value to array)
    }

    private fun checkCompileError(shader: Int, type: String) {
        if (type.equals("program", true)) {
            val succ = intArrayOf(0)
            GLES30.glGetProgramiv(shader, GLES30.GL_LINK_STATUS, succ, 0)
            if (succ[0] != GLES30.GL_TRUE) {
                val log = GLES30.glGetShaderInfoLog(shader)
                Log.e(TAG, "compile program $shader error: ${succ[0]}, $log")
            }
        } else {
            val succ = intArrayOf(0)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, succ, 0)
            if (succ[0] != GLES30.GL_TRUE) {
                val log = GLES30.glGetProgramInfoLog(shader)
                Log.e(TAG, "compile $type shader $shader error: ${succ[0]}, $log")
            }
        }
    }

    companion object {
        const val TAG = "Shader"
    }
}