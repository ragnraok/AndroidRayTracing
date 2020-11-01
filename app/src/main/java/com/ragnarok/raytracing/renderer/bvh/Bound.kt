package com.ragnarok.raytracing.renderer.bvh

import glm_.vec3.Vec3
import kotlin.math.max
import kotlin.math.min

class Bound {

    companion object {
        val emptyBound = Bound(Vec3(0f))
    }

    var min: Vec3
    var max: Vec3

    constructor(point: Vec3) {
        min = point
        max = point
    }

    constructor(min: Vec3, max: Vec3) {
        this.min = min
        this.max = max
    }

    fun diagonal(): Vec3 {
        return max - min
    }

    fun maxExtent(): Axis {
        val d = diagonal()
        if (d.x > d.y && d.x > d.z) {
            return Axis.X
        } else if (d.y > d.z) {
            return Axis.Y
        } else {
            return Axis.Z
        }
    }

    fun center(): Vec3 {
        return min * 0.5 + max * 0.5
    }

    fun union(other: Bound): Bound {
        return Bound(min(this.min, other.min), max(this.max, other.max))
    }

    fun union(other: Vec3): Bound {
        return Bound(min(this.min, other), max(this.max, other))
    }

    private fun min(a: Vec3, b: Vec3): Vec3 {
        return Vec3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))
    }

    private fun max(a: Vec3, b: Vec3): Vec3 {
        return Vec3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
    }

    override fun toString(): String {
        return "[$min, $max]"
    }
}

enum class Axis {
    X,
    Y,
    Z
}