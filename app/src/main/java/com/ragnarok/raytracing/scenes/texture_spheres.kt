package com.ragnarok.raytracing.scenes

import com.ragnarok.raytracing.glsl.PassVariable
import com.ragnarok.raytracing.glsl.intersectSceneFuncHead
import com.ragnarok.raytracing.glsl.intersectShadowRayFuncHead
import org.intellij.lang.annotations.Language

@Language("glsl")
val texture_spheres = """
    Plane plane = Plane(vec3(0.0, 0.0, 0.0), normalize(vec3(0.0, 1.0, 0.0)), 1.5, createPBRMaterial(vec3(0.5), 0.01, 1.0));
    const int SPHERE_NUMS = 2;
    Sphere spheres[SPHERE_NUMS] = Sphere[SPHERE_NUMS](
        Sphere(vec3(-0.5, 0.5, -0.5), 0.5, createPBRMaterial(vec3(0.8, 0.7, 0.5), 0.5, 0.05)),
        Sphere(vec3(0.5, 0.5, 1.0), 0.5, createPBRMaterial(vec3(0.8, 1.0, 0.5), 0.7, 0.05))
    );
    uniform MaterialTextures textures[SPHERE_NUMS];
    
    PointLight pointLight = PointLight(vec3(0.0, 1.0, 1.0), 0.1, vec3(1.0), 5.0);    

    $intersectSceneFuncHead {
        float t = ${PassVariable.infinity};
        vec3 hit = vec3(0.0);
        vec3 normal = vec3(0.0);
        vec2 uv = vec2(0.0);
        Material material;
        
        Intersection intersect;
        
        Intersection planeIntersect = intersectPlane(ray, plane);
        if (planeIntersect.nearFar.x > 0.0 && planeIntersect.nearFar.x < t) {
            t = planeIntersect.nearFar.x;
            hit = pointAt(ray, t);
            intersect.nearFar = planeIntersect.nearFar;
            normal = normalForPlane(hit, plane);
            material = plane.material;
            uv = vec2(0.0);
        }

        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(ray, spheres[i]);
            if (intersect.nearFar.x > 0.0 && intersect.nearFar.x < t) {
                t = intersect.nearFar.x;
                hit = pointAt(ray, t);
                normal = normalForSphere(hit, spheres[i]);
                material = spheres[i].material;
                uv = uvForSphere(hit);
//                vec3 color = texture(textures[i].colorTex, uv).rgb;
//                float metallic = texture(textures[i].metallicTex, uv).r;
//                float roughness = texture(textures[i].roughnessTex, uv).r;
//                if (color.r >= 0.0) {
//                    material.color = color;
//                }
//                if (metallic >= 0.0) {
//                    material.metallic = metallic;
//                }
//                if (roughness >= 0.0) {
//                    material.metallic = roughness;
//                }
            }
        }
            
        intersect.t = t;
        if (t == ${PassVariable.infinity}) {
            intersect.nearFar = vec2(${PassVariable.infinity}, ${PassVariable.infinity});
            intersect.t = ${PassVariable.infinity};
            return intersect;
        }
        
        intersect.hit = hit;
        intersect.normal = normal;
        intersect.material = material;
        intersect.uv = uv;
        
        return intersect;
    }
    
    $intersectShadowRayFuncHead {
        Intersection intersect;
        float shadow = 1.0;
        
        for (int i = 0; i < SPHERE_NUMS; i++) {
            intersect = intersectSphere(shadowRay, spheres[i]);

            if (intersect.nearFar.x < 1.0) {
                shadow = 0.0;
            }   
        }

        return shadow;
    }
""".trimIndent()