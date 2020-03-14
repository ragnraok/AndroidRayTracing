package com.ragnarok.raytracing.model

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Camera {

    var position = Vec3(0)
    var front = Vec3(0, 0, -1)
    var up = Vec3(0, 1, 0)
    var worldUp = up
    var right = Vec3(0)
    var yaw: Float = -90.0f
    var pitch: Float = 0f
    var zoom = 45.0f
    var fov = 30.0f

    var center = Vec3(0)

//    constructor(pos: Vec3 = Vec3(0), up: Vec3 = Vec3(0, 1, 0), yaw: Float = -90f, pitch: Float = 0f) {
//        this.position = pos
//        this.up = up
//        this.yaw = yaw
//        this.pitch = pitch
//        calcPostion()
//    }

    constructor(pos: Vec3, fov: Float) {
        this.position = pos
        this.fov = fov
        this.up = Vec3(0, 1, 0)
        this.yaw = -90f
        this.pitch = 0f
    }

    fun getViewMatrix(): Mat4 {
        return glm.lookAt(position, position + front, up)
    }

    fun lookAt(center: Vec3): Mat4 {
        this.center = center
        return glm.lookAt(position, center, Vec3(0, 1, 0))
    }

    fun getWorldMatrix(center: Vec3): Mat4 {
        return Mat4(1.0) * glm.translate(Mat4(1.0), position - center)
//        * glm.lookAt(position, center, Vec3(0, 1, 0))
    }

    fun getCameraOrthoNormalBasis(center: Vec3): Array<Vec3> {
        var n = Vec3(position.x - center.x, position.y - center.y, position.z - center.z)
        n = glm.normalize(n)
        var i = Vec3(0, 1, 0).cross(n)
        var o = n.cross(i)
        o = glm.normalize(o)
        return arrayOf(i, o, n)
    }

    fun getVerticalFovRadian(): Float {
        return (0.5 / tan(0.5 * Math.PI * fov / 180)).toFloat()
    }

    private fun calcPostion() {
        front.x = cos(glm.radians(yaw)) * cos(glm.radians(pitch))
        front.y = sin(glm.radians(pitch))
        front.z = sin(glm.radians(yaw)) * cos(glm.radians(pitch))
        front = glm.normalize(front)
        // Also re-calculate the Right and Up vector
        right = glm.normalize(glm.cross(front, worldUp))  // Normalize the vectors, because their length gets closer to 0 the more you look up or down which results in slower movement.
        up    = glm.normalize(glm.cross(right, front))
    }
}