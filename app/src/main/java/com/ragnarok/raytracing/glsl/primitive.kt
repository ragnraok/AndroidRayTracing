package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// shader data structure defines

@Language("glsl")
val ray = """
    struct Ray {
        vec3 origin;
        vec3 direction;
        bool pbrBRDF;
        bool pbrDiffuseRay;
        float time;
    };
    Ray createRay(vec3 origin, vec3 direction) {
        Ray ray;
        ray.origin = origin;
        ray.direction = direction;
        return ray;
    }
    vec3 pointAt(Ray ray, float t) {
        return ray.origin + ray.direction * t;
    }
""".trimIndent()


@Language("glsl")
val material = """
    const int DIFFUSE = 1;
    const int MIRROR = 2;
    const int GLOSSY = 3;
    const int PBR_BRDF = 4;
    const int LIGHT = 5;
    struct Material {
        int type;
        vec3 color;
        float metallic;
        float roughness;
        float specular;
        bool glass;
        float glassRatio;
    };
""".trimIndent()

@Language("glsl")
val intersection = """
    struct Intersection {
        vec2 nearFar;
        vec3 normal;
        float t;
        vec3 hit;
        Material material;
    };
""".trimIndent()

@Language("glsl")
val cube = """
    struct Cube {
        vec3 cubeMin;
        vec3 cubeMax;
        Material material;
    };
""".trimIndent()

@Language("glsl")
val sphere = """
    struct Sphere {
        vec3 center;
        float radius;
        Material material;
    };
""".trimIndent()

@Language("glsl")
val plane = """
    struct Plane { 
        vec3 center;
        vec3 normal;
        float size;
        Material material;
    };
""".trimIndent()

@Language("glsl")
val pointLight = """
    struct PointLight {
        vec3 position;
        float radius;
        vec3 color;
        float intensity;
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
    $plane
    $pointLight
    $directionLight
""".trimIndent()


