package com.ragnarok.raytracing.scenes

import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.glsl.intersectSceneFuncHead
import com.ragnarok.raytracing.glsl.intersectShadowRayFuncHead
import org.intellij.lang.annotations.Language

@Language("glsl")
val glassMaterials = """
        Plane plane = Plane(vec3(0.0, 0.0, 0.0), normalize(vec3(0.0, 1.0, 0.0)), 1.5, createPBRMaterial(vec3(0.5), 0.0, 1.0));
        const int SPHERE_NUMS = 1;
        Sphere spheres[SPHERE_NUMS] = Sphere[SPHERE_NUMS](
            Sphere(vec3(-0.3, 0.3, 0.3), 0.3, createGlassMaterial(vec3(1.0), 1.05))
        ); 
        
        const int BOX_NUMS = 1;
        Cube boxCubes[BOX_NUMS] = Cube[BOX_NUMS](
            Cube(vec3(0.2, 0.0, 0.3), vec3(0.5, 0.5, 0.6), createGlassMaterial(vec3(0.75), 1.5))
        );
        
        PointLight pointLight = PointLight(vec3(0.0, 0.5, 0.5), 0.2, vec3(1.0), 5.0);
            
        $intersectSceneFuncHead {
            float t = ${PassVariable.infinity};
            vec3 hit = vec3(0.0);
            vec3 normal = vec3(0.0);
            Material material;
            
            Intersection intersect;
            
            Intersection planeIntersect = intersectPlane(ray, plane);
            if (planeIntersect.nearFar.x > 0.0 && planeIntersect.nearFar.x < t) {
                t = planeIntersect.nearFar.x;
                hit = pointAt(ray, t);
                intersect.nearFar = planeIntersect.nearFar;
                normal = normalForPlane(hit, plane);
                material = plane.material;
            }

            for (int i = 0; i < SPHERE_NUMS; i++) {
                intersect = intersectSphere(ray, spheres[i]);
                if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                    t = intersect.nearFar.x;
                    hit = pointAt(ray, t);
                    normal = normalForSphere(hit, spheres[i]);
                    material = spheres[i].material;
                }
            }
            
            for (int i = 0; i < BOX_NUMS; i++) {
                intersect = intersectCube(ray, boxCubes[i]);
                if (intersect.nearFar.x > 1.0 && intersect.nearFar.x < intersect.nearFar.y && intersect.nearFar.x < t) {
                    t = intersect.nearFar.x;
                    hit = pointAt(ray, t);
                    normal = normalForCube(hit, boxCubes[i]);
                    material = boxCubes[i].material;
                }
            }
            
                
            intersect.t = t;
            if (t ==  ${PassVariable.infinity}) {
                intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
                intersect.t = ${PassVariable.infinity};
                return intersect;
            }
            
            intersect.hit = hit;
            intersect.normal = normal;
            intersect.material = material;
            
            return intersect;
        }
        
        $intersectShadowRayFuncHead {
            Intersection intersect;
            float shadow = 1.0;
            
//            for (int i = 0; i < SPHERE_NUMS; i++) {
//                intersect = intersectSphere(shadowRay, spheres[i]);
//
//                if (intersect.nearFar.x < 1.0) {
//                    shadow = 0.0;
//                }   
//            }
//            
//            for (int i = 0; i < BOX_NUMS; i++) {
//                intersect = intersectCube(shadowRay, boxCubes[i]);
//                if (intersect.nearFar.x < 1.0) {
//                    shadow = 0.0;
//                } 
//            }

            return shadow;
        }
""".trimIndent()