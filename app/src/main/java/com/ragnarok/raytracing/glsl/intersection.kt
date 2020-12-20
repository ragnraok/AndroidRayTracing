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
        intersect.nearFar = vec2(tNear, tFar);
        intersect.t = intersect.nearFar.x;
        intersect.hit = pointAt(ray, intersect.t);
        return intersect;
    }
    Intersection intersectCubeWithTransform(Ray ray, Cube cube, mat4 transform) {
        Ray transformRay = ray;
        transform = inverse(transform);
        transformRay.origin = vec3(transform * vec4(ray.origin, 1.0)); 
        transformRay.direction = vec3(transform * vec4(ray.direction, 1.0));
        Intersection intersect = intersectCube(transformRay, cube);
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
                intersect.t = intersect.nearFar.x;
                intersect.hit = pointAt(ray, intersect.t);
                return intersect;
            }
        }
        intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
        return intersect;
    }
    Intersection intersectSphereWithTransform(Ray ray, Sphere sphere, mat4 transform) {
        Ray transformRay = ray;
        transform = inverse(transform);
        transformRay.origin = vec3(transform * vec4(ray.origin, 1.0)); 
        transformRay.direction = vec3(transform * vec4(ray.direction, 1.0));
        Intersection intersect = intersectSphere(transformRay, sphere);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val normalForSphere = """
    vec3 normalForSphere(vec3 hit, Sphere sphere) {
        return normalize(hit - sphere.center);
    }
""".trimIndent()

@Language("glsl")
val uvForSphere = """
    vec2 uvForSphere(vec3 hit, Sphere sphere) {
        vec3 p = (hit - sphere.center) / sphere.radius;
        float phi = atan(p.z, p.x);
        float theta = asin(p.y);
        float u = 1.0 - (phi + ${PassVariable.pi}) / (2.0 * ${PassVariable.pi});
        float v = (theta + ${PassVariable.pi} / 2.0) / ${PassVariable.pi};
        return vec2(u, v);
    }
""".trimIndent()

@Language("glsl")
val intersectMoveSphere = """
    Intersection intersectMoveSphere(Ray ray, MoveSphere sphere) {
        Intersection intersect;
        vec3 toSphere = ray.origin - centerOfMoveSphere(ray.time, sphere);
        float a = dot(ray.direction, ray.direction);
        float b = 2.0 * dot(toSphere, ray.direction);
        float c = dot(toSphere, toSphere) - sphere.radius*sphere.radius;
        float discriminant = b*b - 4.0*a*c;
        if(discriminant > 0.0) {
            float t = (-b - sqrt(discriminant)) / (2.0 * a);
            if(t > 0.0) {
                intersect.nearFar = vec2(t, t);
                intersect.t = intersect.nearFar.x;
                intersect.hit = pointAt(ray, intersect.t);
                return intersect;
            }
        }
        intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
        return intersect;
    }
    Intersection intersectMoveSphereWithTransform(Ray ray, MoveSphere sphere, mat4 transform) {
        Ray transformRay = ray;
        transform = inverse(transform);
        transformRay.origin = vec3(transform * vec4(ray.origin, 1.0)); 
        transformRay.direction = vec3(transform * vec4(ray.direction, 1.0));
        Intersection intersect = intersectMoveSphere(transformRay, sphere);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val normalForMoveSphere = """
    vec3 normalForMoveSphere(vec3 hit, float time, MoveSphere sphere) {
        return normalize(hit - centerOfMoveSphere(time, sphere));
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
        intersect.t = intersect.nearFar.x;
        intersect.hit = pointAt(ray, intersect.t);
        return intersect;
    }
    Intersection intersectPlaneWithTransform(Ray ray, Plane plane, mat4 transform) {
        Ray transformRay = ray;
        transform = inverse(transform);
        transformRay.origin = vec3(transform * vec4(ray.origin, 1.0)); 
        transformRay.direction = vec3(transform * vec4(ray.direction, 1.0));
        Intersection intersect = intersectPlane(transformRay, plane);
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
val normalForTriangle = """
    vec3 normalForTriangle(Triangle triangle) {
        vec3 e1 = triangle.p1 - triangle.p0;
        vec3 e2 = triangle.p2 - triangle.p0;
        return normalize(cross(e1, e2));
    }
""".trimIndent()

@Language("glsl")
val intersectTriangle = """
    Intersection intersectTriangle(Ray ray, Triangle triangle) {
        Intersection intersect;
        float t = ${PassVariable.infinity};
        do {
            if (dot(-ray.direction, normalForTriangle(triangle)) > 0.0) {
                t = ${PassVariable.infinity};
                break;
            } else {
                vec3 e1 = triangle.p1 - triangle.p0;
                vec3 e2 = triangle.p2 - triangle.p0;
                vec3 pvec = cross(ray.direction, e2);
                float det = dot(e1, pvec);
                float invDet = 1.0 / det;
                if (invDet > 1.0) {
                    t = ${PassVariable.infinity};
                    break;
                }
                vec3 tvec = ray.origin - triangle.p0;
                float u = dot(tvec, pvec) * invDet;
                if (u < 0.0 || u > 1.0) {
                    t = ${PassVariable.infinity};
                    break;
                }
                vec3 qvec = cross(tvec, e1);
                float v = dot(ray.direction, qvec) * invDet;
                if (v < 0.0 || u + v > 1.0) {
                    t = ${PassVariable.infinity};
                    break;
                } else {
                    t = dot(e2, qvec) * invDet;
                }
                if (t < 0.0) {
                    t = ${PassVariable.infinity};
                }
            }
        } while (false);
        intersect.nearFar = vec2(t, t);
        intersect.t = intersect.nearFar.x;
        intersect.hit = pointAt(ray, intersect.t);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val intersectBound = """
    bool isIntersectBound(Ray ray, Bound bound) {
        vec3 invDir = vec3(1.0 / ray.direction.x, 1.0 / ray.direction.y, 1.0 / ray.direction.z);
        float txMin = (bound.min.x - ray.origin.x) * invDir.x;
        float txMax = (bound.max.x - ray.origin.x) * invDir.x;
        
        float tyMin = (bound.min.y - ray.origin.y) * invDir.y;
        float tyMax = (bound.max.y - ray.origin.y) * invDir.y;
        
        float tzMin = (bound.min.z - ray.origin.z) * invDir.z;
        float tzMax = (bound.max.z - ray.origin.z) * invDir.z;
        
        if (ray.direction.x < 0.0) {
            float t = txMin;
            txMin = txMax;
            txMax = t;
        }
        if (ray.direction.y < 0.0) {
            float t = tyMin;
            tyMin = tyMax;
            tyMax = t;
        }
        if (ray.direction.z < 0.0) {
            float t = tzMin;
            tzMin = tzMax;
            tzMax = t;
        }
        float tMin = max(txMin, max(tyMin, tzMin));
        float tMax = min(txMax, min(tyMax, tzMax));
        
        if (tMin <= tMax && tMax >= 0.0) {
            return true;
        }
       
        return false;
        
    }
""".trimIndent()

@Language("glsl")
val intersectPointLight = """
    Intersection intersectPointLight(Ray ray, PointLight pointLight) {
        Material material;
        material.type = LIGHT;
        Sphere pointLightSphere = Sphere(pointLight.position, pointLight.radius, material);
        Intersection intersect = intersectSphere(ray, pointLightSphere);
        return intersect;
    }
""".trimIndent()

@Language("glsl")
val convertVectorByTransform = """
    vec3 convertHitByTransform(vec3 hit, mat4 transform) {
        vec3 transformHit = vec3(transform * vec4(hit, 1.0));
        return transformHit;
    }
    vec3 convertNormalByTransform(vec3 normal, mat4 transform) {
        normal = vec3(transform * vec4(normal, 1.0));
        return normal;
    }
""".trimIndent()

val intersections = """
    $intersectCube
    $normalForCube
    $intersectSphere
    $normalForSphere
    $uvForSphere
    $intersectPlane
    $normalForPlane
    $intersectPointLight
    $intersectMoveSphere
    $normalForMoveSphere
    $convertVectorByTransform
    $normalForTriangle
    $intersectTriangle
    $intersectBound
""".trimIndent()