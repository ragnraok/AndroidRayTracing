package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.infinity
import org.intellij.lang.annotations.Language


@Language("glsl")
val intersectScene = "Intersection intersectScene(Ray ray)"

@Language("glsl")
val intersectShadowRay = "float intersectShadowRay(Ray shadowRay)"

@Language("glsl")
val cornellBoxScene = """
    const int BOX_NUMS = 3;
    const int SPHERE_NUMS = 1;
    Cube cornellBox = Cube(vec3(-1.0, -1.0, -1.0), vec3(1.0, 1.0, 1.0));
    Cube boxCubes[BOX_NUMS] = Cube[BOX_NUMS](
        Cube(vec3(-0.25, -1.0, -0.5), vec3(0.25, 0.5, -0.25)),
        Cube(vec3(0.5, -1.0, -0.5), vec3(1.0, -0.25, -0.25)),
        Cube(vec3(-1.0, -1.0, -0.25), vec3(-0.5, -0.25, 0.0))
    );
    Sphere boxSpheres[SPHERE_NUMS] = Sphere[SPHERE_NUMS](
        Sphere(vec3(0.25, -0.75, 0.5), 0.25)
    );
    
    PointLight pointLight = PointLight(vec3(-0.5, 0.3, 0.5), 0.3, vec3(0.75));
    
    // scene intersect
    $intersectScene {
        float t = $infinity;
        vec3 color = vec3(0.0);
        vec3 hit = vec3(0.0);
        vec3 normal = vec3(0.0);
        Material material;
        material.type = DIFFUSE;
        
        Intersection roomIntersect = intersectCube(ray, cornellBox);
        if (roomIntersect.nearFar.x < roomIntersect.nearFar.y) {
            t = roomIntersect.nearFar.y;
            hit = pointAt(ray, t);
            normal = -normalForCube(hit, cornellBox);
            
            float delta = 0.9999;
            if (hit.x < -1.0 * delta) {
                color = vec3(1.0, 0.3, 0.1);
            } else if (hit.x > delta) {
                color = vec3(0.3, 1.0, 0.1);
            } else if (hit.y < -1.0 * delta || hit.y > delta) {
                color =  vec3(0.75);
            } else if (hit.z < -1.0 * delta) {
                color = vec3(0.75);
            }
            material.type = DIFFUSE;
        }
        
        Intersection intersect;
        for (int i = 0; i < BOX_NUMS; i++) {
            intersect = intersectCube(ray, boxCubes[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < intersect.nearFar.y && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = pointAt(ray, t);
                normal = normalForCube(hit, boxCubes[i]);
                color = vec3(0.5);
                if (i == 0) {
                    material.type = MIRROR;
                } else if (i == 1) {
                    material.type = GLOSSY;
                }
            }
        }
        
        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(ray, boxSpheres[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = pointAt(ray, t);
                normal = normalForSphere(hit, boxSpheres[i]);
                color = vec3(0.75);
                material.type = MIRROR;
            }
        }
        
        intersect.t = t;
        if (t == $infinity) {
            intersect.nearFar = vec2($infinity, $infinity);
            return intersect;
        }
        
        intersect.hit = hit;
        intersect.normal = normal;
        intersect.material = material;
        
        intersect.color = color;
        return intersect;
    }
    
    // shadow test
    $intersectShadowRay {
        // something wrong
        Intersection intersect;
        float shadow = 1.0;
        for (int i = 0; i < BOX_NUMS; i++) {
            intersect = intersectCube(shadowRay, boxCubes[i]);

            if (intersect.nearFar.x > 0.0 && intersect.nearFar.y < 1.0 && intersect.nearFar.x < intersect.nearFar.y) {
                shadow = 0.0;
            }
        }

        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(shadowRay, boxSpheres[i]);

            if (intersect.nearFar.x < 1.0) {
                shadow = 0.0;
            }   
        }

        return shadow;
    }
""".trimIndent()

