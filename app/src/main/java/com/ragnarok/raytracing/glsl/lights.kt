package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// lights related function define

// http://www.rorydriscoll.com/2009/01/07/better-sampling/
@Language("glsl")
val cosineWeightDirection = """
    #define N_POINTS 32.0
    vec3 cosineWeightDirection(vec3 normal, int bias) {
//        float r1 = random(bias);
//        float r2 = random(0);
//        float r = sqrt(r2);
//        float theta = 2.0 * ${PassVariable.pi} * r1;
        
        // why this one better??
        float i = floor(N_POINTS * random(0)) + (random(0) * 0.5);
        // the Golden angle in radians
        float theta = i * 2.39996322972865332 + mod(float(frame), 2.0*${PassVariable.pi});
        theta = mod(theta, 2.0*${PassVariable.pi});
        float r = sqrt(i / N_POINTS); // sqrt pushes points outward to prevent clumping in center of disk


        float x = r * cos(theta);
        float y = r * sin(theta);
        float z = sqrt(1.0 - x * x - y * y); // unit sphere
        // calc new ortho normal basic
        vec3 s,t;
        if (abs(normal.x) < 0.5) {
            s = normalize(cross(normal, vec3(1, 0, 0)));
        } else {
            s = normalize(cross(normal, vec3(0, 1, 0)));
        }
        t = normalize(cross(normal, s));
        return x * s + y * t + z * normal;
    }
""".trimIndent()

@Language("glsl")
val pointLightDirection = """
    vec3 pointLightDir(PointLight pointLight) {
        float lightArea = 4.0 * ${PassVariable.pi} * pointLight.radius * pointLight.radius;
        vec3 lightRay = normalize(pointLight.position + uniformRandomDirection() * lightArea);
        return lightRay;
    }
""".trimIndent()

@Language("glsl")
val pointLightAttenuation = """
    float pointLightAttenuation(PointLight pointLight, vec3 position) {
        float dist = length(pointLight.position - position);
        float attenuation = 1.0 / (1.0 + dist * dist);
        return attenuation;
    }
""".trimIndent()

@Language("glsl")
val directionLightDir = """
    vec3 directionLightDir(DirectionLight directionLight) {
        return normalize(directionLight.direction + uniformRandomDirection() * 0.1);
    }
""".trimIndent()

@Language("glsl")
val materialRay = """
    Ray materialRay(Ray ray, Intersection intersection, vec3 lightDir, int bias, out float specular, out bool isBRDFDiffuseRay) {
        if (intersection.material.type == PBR_BRDF) {
            bool isDiffuse = false;
            if (intersection.material.glass == false) {
                vec3 viewDir = normalize(reflect(ray.direction, intersection.normal));
                ray.direction = brdfRayDir(intersection.normal, viewDir, intersection.material, bias, isDiffuse);
                isBRDFDiffuseRay = isDiffuse;
            } else {
                ray.direction = btdfRayDir(intersection.normal, intersection.material, bias, ray.direction);
                isBRDFDiffuseRay = false;
            }
        }
        return ray;
    }
""".trimIndent()

val lights = """
    $brdf
    $cosineWeightDirection
    $pointLightDirection
    $pointLightAttenuation
    $directionLightDir
    $materialRay
""".trimIndent()
