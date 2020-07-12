package com.ragnarok.raytracing.model

import glm_.glm
import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Camera(pos: Vec3, var fov: Float) {

    var position = pos
    var front = Vec3(0, 0, -1)
    var up = Vec3(0, 1, 0)
    var worldUp = up
    var right = Vec3(0)
    var yaw: Float = -90.0f
    var pitch: Float = 0f
    var zoom = 45.0f

    var center = Vec3(0)

    var aperture = 0f
    var focusLength = 0f
    var shutterOpenTime = 0f
    var shutterCloseTime = 0f

    init {
        this.up = Vec3(0, 1, 0)
        this.yaw = -90f
        this.pitch = 0f
    }

    fun lookAt(center: Vec3): Mat4 {
        this.center = center
        return glm.lookAt(position, center, Vec3(0, 1, 0))
    }

    fun getWorldMatrix(eye: Vec3): Mat4 {
        return glm.lookAt(position, eye, Vec3(0, 1, 0)).inverse()
    }

    fun getVerticalFovRadian(): Float {
        return (0.5 / tan(0.5 * Math.PI * fov / 180)).toFloat()
    }
}