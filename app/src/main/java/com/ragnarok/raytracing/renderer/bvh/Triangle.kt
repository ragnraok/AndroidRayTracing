package com.ragnarok.raytracing.renderer.bvh

import glm_.Java.Companion.glm
import glm_.vec3.Vec3
import kotlin.math.sqrt

class Triangle(val a: Vec3, val b: Vec3, val c: Vec3) {
    val bound = Bound(a, b).union(c)
    val area: Float
    val normal: Vec3

    init {
        val e1 = b - a
        val e2 = c - a
        normal = glm.normalize(glm.cross(e1, e2))
        area = glm.cross(e1, e2).norm() * 0.5f
    }

    override fun toString(): String {
        return "Triangle:{bound:$bound, points:[$a,$b,$c]}"
    }

    override fun equals(other: Any?): Boolean {
        if (other is Triangle) {
            return this.a == other.a && this.b == other.b && this.c == other.c
        }
        return false
    }
}

fun Vec3.norm(): Float {
    return sqrt(x * x + y * y + z * z)
}