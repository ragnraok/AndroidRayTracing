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

@Language("glsl")
val intersectPlane = """
    Intersection intersectPlane(Ray ray, Plane plane) {
        Intersection intersect;
        float t = ${PassVariable.infinity};
        if (dot(ray.origin, plane.normal) > 0.0) {
            t = dot(plane.center - ray.origin, plane.normal) / dot(ray.direction, plane.normal);
            vec3 hit = pointAt(ray, t);
            if (abs(hit.x - plane.center.x) > plane.size || abs(hit.y - plane.center.y) > plane.size || abs(hit.z - plane.center.z) > plane.size) {
                 t = ${PassVariable.infinity};
            }
        }
        intersect.nearFar = vec2(t, t);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val normalForPlane = """
    vec3 normalForPlane(vec3 hit, Plane plane) {
        return plane.normal;
    }
""".trimIndent()

@Language("glsl")
val intersectPointLight = """
    Intersection intersectPointLight(Ray ray, PointLight pointLight) {
        Sphere pointLightSphere = Sphere(pointLight.position, pointLight.radius);
        Intersection intersect = intersectSphere(ray, pointLightSphere);
        return intersect;
    }
""".trimIndent()

val intersections = """
    $intersectCube
    $normalForCube
    $intersectSphere
    $normalForSphere
    $intersectPlane
    $normalForPlane
""".trimIndent()