package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// shader data structure defines

@Language("glsl")
val ray = """
    struct Ray {
        vec3 origin;
        vec3 direction;
    };
    vec3 pointAt(Ray ray, float t) {
        return ray.origin + ray.direction * t;
    }
""".trimIndent()


@Language("glsl")
val material = """
    const int DIFFUSE = 1;
    const int MIRROR = 2;
    const int GLOSSY = 3;
    struct Material {
        int type;
    };
""".trimIndent()

@Language("glsl")
val intersection = """
    struct Intersection {
        vec2 nearFar;
        vec3 normal;
        float t;
        vec3 color;
        vec3 hit;
        Material material;
    };
""".trimIndent()

@Language("glsl")
val cube = """
    struct Cube {
        vec3 cubeMin;
        vec3 cubeMax;
    };
""".trimIndent()

@Language("glsl")
val sphere = """
    struct Sphere {
        vec3 center;
        float radius;
    };
""".trimIndent()

@Language("glsl")
val pointLight = """
    struct PointLight {
        vec3 position;
        float radius;
        vec3 color;
    };
""".trimIndent()

@Language("glsl")
val directionLight = """
    struct DirectionLight {
        vec3 direction;
        vec3 color;
    };
""".trimIndent()

val primitives = """
    $ray
    $material
    $intersection
    $cube
    $sphere
    $pointLight
    $directionLight
""".trimIndent()


