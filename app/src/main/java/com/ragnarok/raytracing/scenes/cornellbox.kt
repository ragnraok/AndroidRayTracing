package com.ragnarok.raytracing.scenes

import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.glsl.intersectSceneFuncHead
import com.ragnarok.raytracing.glsl.intersectShadowRayFuncHead
import org.intellij.lang.annotations.Language

@Language("glsl")
val cornellBox = """
    const int BOX_NUMS = 2;
    const int SPHERE_NUMS = 1;
    const int MOVE_SPHERE_NUMS = 1;
    Cube cornellBox = Cube(vec3(-1.0, -1.0, -1.0), vec3(1.0, 1.0, 1.0), createNormalMaterial(vec3(0.5), 0.0, 1.0));
    Cube boxCubes[BOX_NUMS] = Cube[BOX_NUMS](
        Cube(vec3(0.2, -1.0, -0.5), vec3(0.7, 0.0, -0.25), createNormalMaterial(vec3(1.0), 0.01, 1.0)),
        Cube(vec3(-0.7, -1.0, -0.5), vec3(-0.2, 0.25, -0.25), createNormalMaterial(vec3(0.75), 0.01, 1.0))
    );
    Sphere boxSpheres[SPHERE_NUMS] = Sphere[SPHERE_NUMS](
        Sphere(vec3(0.0, -0.75, 0.5), 0.25, createNormalMaterial(vec3(0.5), 1.0, 0.1))
    );
    MoveSphere moveSpheres[MOVE_SPHERE_NUMS] = MoveSphere[MOVE_SPHERE_NUMS](
        MoveSphere(vec3(-0.4, 0.3, 0.5), vec3(-0.45, 0.25, 0.5), 0.25, createNormalMaterial(vec3(0.5), 1.0, 0.1))
    );
    Plane emissivePlane = Plane(vec3(0.0, 0.98, 0.0), normalize(vec3(0.0, 1.0, 0.0)), 0.5, createEmissiveMaterial(vec3(1.0), vec3(1.0) * 5.0f, 0.01, 1.0));
    
    PointLight pointLight = PointLight(vec3(0.0, 0.95, 0.7), 0.1, vec3(1.0), 3.0);
    
    uniform mat4 cubeTransform[BOX_NUMS];
    
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
                material.color = vec3(1.0, 0.0, 0.0);
            } else if (hit.x > delta) {
                material.color = vec3(0.0, 1.0, 0.0);
            } else if (hit.y < -1.0 * delta || hit.y > delta) {
                material.color =  vec3(0.5);
            } else if (hit.z < -1.0 * delta) {
                material.color = vec3(0.5);
            } else {
                material.color = vec3(0.0);
            }
        }
        
        Intersection intersect;
        
        Intersection planeIntersect = intersectPlane(ray, emissivePlane);
        if (planeIntersect.nearFar.x > 0.0 && planeIntersect.nearFar.x < t) {
            t = planeIntersect.nearFar.x;
            hit = intersect.hit;
            intersect.nearFar = planeIntersect.nearFar;
            normal = normalForPlane(hit, emissivePlane);
            material = emissivePlane.material;
        }

        for (int i = 0; i < BOX_NUMS; i++) {
            intersect = intersectCubeWithTransform(ray, boxCubes[i], cubeTransform[i]);
            if (intersect.nearFar.x > 1.0 && intersect.nearFar.x < intersect.nearFar.y && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = intersect.hit;
                normal = normalForCube(hit, boxCubes[i]);
                material = boxCubes[i].material;
            }
        }
        
        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(ray, boxSpheres[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = intersect.hit;
                normal = normalForSphere(hit, boxSpheres[i]);
                material = boxSpheres[i].material;
            }
        }
        
        for (int i = 0; i < MOVE_SPHERE_NUMS; i++) {
            intersect = intersectMoveSphere(ray, moveSpheres[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = intersect.hit;
                normal = normalForMoveSphere(hit, ray.time, moveSpheres[i]);
                material = moveSpheres[i].material;
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
            intersect = intersectCubeWithTransform(shadowRay, boxCubes[i], cubeTransform[i]);

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
        
        for (int i = 0; i < MOVE_SPHERE_NUMS; i++) {
            intersect = intersectMoveSphere(shadowRay, moveSpheres[i]);
            if (intersect.nearFar.x < 1.0) {
                shadow = 0.0;
            }    
        }        

        return shadow;
    }
""".trimIndent()