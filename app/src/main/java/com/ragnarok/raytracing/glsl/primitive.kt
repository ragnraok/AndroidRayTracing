package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// shader data structure defines

//TODO: object transformation, rotate/scale/translate

@Language("glsl")
val ray = """
    struct Ray {
        vec3 origin;
        vec3 direction;
        bool pbrBRDF;
        bool pbrDiffuseRay;
        float time;
        vec2 textureCoord;
    };
    Ray createRay(vec3 origin, vec3 direction) {
        Ray ray;
        ray.origin = origin;
        ray.direction = direction;
        ray.textureCoord = vec2(-1.0, -1.0);
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
    
    struct MaterialTextures {
        // Textures
        sampler2D colorTex;
        sampler2D normalTex;
        sampler2D metallicTex;
        sampler2D roughnessTex;    
    };    
    struct Material {
        int type;
        
        // Lambert
        vec3 color;
        
        // BRDF
        float metallic;
        float roughness;
        
        // BTDF
        bool glass;
        float glassRatio;
        
        bool hasTextures;
    };
    Material createNonPBRMaterial(int type, vec3 color) {
        Material material;
        material.type = type;
        material.color = color;
        return material;
    }
    Material createGlassMaterial(vec3 color, float glassRatio) {
        Material material;
        material.type = PBR_BRDF;
        material.color = color;
        material.glass = true;
        material.glassRatio = glassRatio;
        return material;
    }
    Material createPBRMaterial(vec3 color, float metallic, float roughness) {
        Material material;
        material.type = PBR_BRDF;
        material.color = color;
        material.metallic = metallic;
        material.roughness = roughness;
        return material;
    }
""".trimIndent()

@Language("glsl")
val intersection = """
    struct Intersection {
        vec2 nearFar;
        vec3 normal;
        float t;
        vec3 hit;
        vec2 uv;
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
    struct MoveSphere {
        vec3 centerStart;
        vec3 centerEnd;
        float radius;
        Material material;
    };
    vec3 centerOfMoveSphere(float time, MoveSphere sphere) {
        return mix(sphere.centerStart, sphere.centerEnd, time);
    }
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


