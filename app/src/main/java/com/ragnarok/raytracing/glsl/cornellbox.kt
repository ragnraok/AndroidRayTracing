package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

@Language("glsl")
val cornellBox = """
    const int BOX_NUMS = 3;
    const int SPHERE_NUMS = 1;
    const Material diffuseMaterial = Material(DIFFUSE, vec3(0.5), 0.0, 0.0, 0.0, false, 0.0);
    const Material mirrorMaterial = Material(MIRROR, vec3(0.5), 0.0, 0.0, 0.0, false, 0.0);
    const Material glossyMaterial = Material(GLOSSY, vec3(0.5), 0.0, 0.0, 0.0, false, 0.0);
    Cube cornellBox = Cube(vec3(-1.0, -1.0, -1.0), vec3(1.0, 1.0, 1.0), diffuseMaterial);
    Cube boxCubes[BOX_NUMS] = Cube[BOX_NUMS](
        Cube(vec3(-0.25, -1.0, -0.25), vec3(0.25, -0.25, 0.0), mirrorMaterial),
        Cube(vec3(0.5, -1.0, -1.0), vec3(1.0, -0.25, -0.75), glossyMaterial),
        Cube(vec3(-1.0, -1.0, 0.0), vec3(-0.5, 0.25, 0.25), diffuseMaterial)
    );
    Sphere boxSpheres[SPHERE_NUMS] = Sphere[SPHERE_NUMS](
        Sphere(vec3(0.1, -0.75, 0.5), 0.25, mirrorMaterial)
    );
    
    PointLight pointLight = PointLight(vec3(-0.5, 1.0, 1.0), 1.0, vec3(0.5), 2.0);
    DirectionLight directionLight = DirectionLight(normalize(vec3(0) - vec3(-1.0, 1.0, 1.0)), vec3(0.5));
    
    // scene intersect
    $intersectSceneFuncHead {
        float t = ${PassVariable.infinity};
        vec3 hit = vec3(0.0);
        vec3 normal = vec3(0.0);
        Material material;
        
        Intersection roomIntersect = intersectCube(ray, cornellBox);
        if (roomIntersect.nearFar.x < roomIntersect.nearFar.y) {
            t = roomIntersect.nearFar.y;
            hit = pointAt(ray, t);
            normal = -normalForCube(hit, cornellBox);
            material = cornellBox.material;
            float delta = 0.9999;
            if (hit.x < -1.0 * delta) {
                material.color = vec3(1.0, 0.3, 0.1);
            } else if (hit.x > delta) {
                material.color = vec3(0.3, 1.0, 0.1);
            } else if (hit.y < -1.0 * delta || hit.y > delta) {
                material.color =  vec3(0.75);
            } else if (hit.z < -1.0 * delta) {
                material.color = vec3(0.75);
            }
        }
        
        Intersection intersect;
        for (int i = 0; i < BOX_NUMS; i++) {
            intersect = intersectCube(ray, boxCubes[i]);
            if (intersect.nearFar.x > 1.0 && intersect.nearFar.x < intersect.nearFar.y && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = pointAt(ray, t);
                normal = normalForCube(hit, boxCubes[i]);
                material = boxCubes[i].material;
            }
        }
        
        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(ray, boxSpheres[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = pointAt(ray, t);
                normal = normalForSphere(hit, boxSpheres[i]);
                material = boxSpheres[i].material;
            }
        }
        
        intersect.t = t;
        if (t == ${PassVariable.infinity}) {
            intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
            return intersect;
        }
        
        intersect.hit = hit;
        intersect.normal = normal;
        intersect.material = material;

        return intersect;
    }
    
    // shadow test
    $intersectShadowRayFuncHead {
        // something wrong
        Intersection intersect;
        float shadow = 1.0;
        for (int i = 0; i < BOX_NUMS; i++) {
            intersect = intersectCube(shadowRay, boxCubes[i]);

            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < 1.0 && intersect.nearFar.x < intersect.nearFar.y) {
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