package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language


@Language("glsl")
val normalForCube = """
    vec3 normalForCube(vec3 hit, Cube cube) {
        if(hit.x < cube.cubeMin.x + ${PassVariable.eps}) return vec3(-1.0, 0.0, 0.0);
        else if (hit.x > cube.cubeMax.x - ${PassVariable.eps}) return vec3(1.0, 0.0, 0.0);
        else if (hit.y < cube.cubeMin.y + ${PassVariable.eps}) return vec3(0.0, -1.0, 0.0);
        else if (hit.y > cube.cubeMax.y - ${PassVariable.eps}) return vec3(0.0, 1.0, 0.0);
        else if (hit.z < cube.cubeMin.z + ${PassVariable.eps}) return vec3(0.0, 0.0, -1.0);
        else return vec3(0.0, 0.0, 1.0);
    }
""".trimIndent()

@Language("glsl")
val intersectCube = """
    Intersection intersectCube(Ray ray, Cube cube) {
        Intersection intersect;
        vec3 tMin = (cube.cubeMin - ray.origin) / ray.direction;
        vec3 tMax = (cube.cubeMax - ray.origin) / ray.direction;
        vec3 t1 = min(tMin, tMax);
        vec3 t2 = max(tMin, tMax);
        float tNear = max(max(t1.x, t1.y), t1.z);
        float tFar = min(min(t2.x, t2.y), t2.z);
        intersect.nearFar = vec2(tNear, tFar);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val intersectSphere = """
    Intersection intersectSphere(Ray ray, Sphere sphere) {
        Intersection intersect;
        vec3 toSphere = ray.origin - sphere.center;
        float a = dot(ray.direction, ray.direction);
        float b = 2.0 * dot(toSphere, ray.direction);
        float c = dot(toSphere, toSphere) - sphere.radius*sphere.radius;
        float discriminant = b*b - 4.0*a*c;
        if(discriminant > 0.0) {
            float t = (-b - sqrt(discriminant)) / (2.0 * a);
            if(t > 0.0) {
                intersect.nearFar = vec2(t, t);
                return intersect;
            }
        }
        intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val normalForSphere = """
    vec3 normalForSphere(vec3 hit, Sphere sphere) {
        return (hit - sphere.center) / sphere.radius;
    }
""".trimIndent()

val intersections = """
    $intersectCube
    $normalForCube
    $intersectSphere
    $normalForSphere
""".trimIndent()