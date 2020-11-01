package com.ragnarok.raytracing.renderer.bvh

import glm_.Java.Companion.glm
import glm_.vec3.Vec3
import kotlin.math.sqrt

class Triangle(private val a: Vec3, private val b: Vec3, private val c: Vec3) {
    val bound = Bound(a, b).union(c)
    val area: Float
    val normal: Vec3

    init {
        val e1 = b - a
        val e2 = c - a
        normal = glm.normalize(glm.cross(e1, e2))
        area = glm.cross(e1, e2).norm() * 0.5f
    }
}

fun Vec3.norm(): Float {
    return sqrt(x * x + y * y + z * z)
}