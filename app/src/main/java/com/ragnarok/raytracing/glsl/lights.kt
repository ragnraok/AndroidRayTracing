package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// lights related function define

// https://raytracing.github.io/books/RayTracingTheRestOfYourLife.html#generatingrandomdirections
@Language("glsl")
val uniformRandomDirection = """
    vec3 uniformRandomDirection() {
        float r1 = random(frame);
        float r2 = random(0);
        
        float z = 1.0 - 2.0 * r2;
        float phi = 2.0 * ${PassVariable.pi} * r1;
        float x = cos(phi) * sqrt(r2);
        float y = sin(phi) * sqrt(r2);
        return vec3(x, y, z);
    }
""".trimIndent()

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
        float attenuation = 1.0 / (dist * dist);
        return attenuation;
    }
""".trimIndent()

@Language("glsl")
val materialRay = """
    Ray materialRay(Ray ray, Intersection intersection, vec3 lightDir, int bias, out float specular) {
        if (intersection.material.type == DIFFUSE) {
            ray.direction = normalize(cosineWeightDirection(intersection.normal, bias));
        } else if (intersection.material.type == MIRROR) {
            ray.direction = normalize(reflect(ray.direction, intersection.normal));
            vec3 reflectedLight = normalize(reflect(lightDir, intersection.normal));
            vec3 viewDir = normalize(ray.origin - intersection.hit);
            specular = pow(max(0.0, dot(reflectedLight, -viewDir)), 50.0);
            specular = 2.0 * specular;
        } else if (intersection.material.type == GLOSSY) {
            float glossiness = ${PassVariable.glossiness};
            ray.direction = normalize(reflect(ray.direction, intersection.normal)) + uniformRandomDirection() * glossiness;
            vec3 reflectedLight = normalize(reflect(lightDir, intersection.normal));
            vec3 viewDir = normalize(ray.origin - intersection.hit);
            specular = pow(max(0.0, dot(reflectedLight, -viewDir)), 30.0);
            specular = 2.0 * specular;
        }
        return ray;
    }
""".trimIndent()

val lights = """
    $uniformRandomDirection
    $cosineWeightDirection
    $pointLightDirection
    $pointLightAttenuation
    $materialRay
""".trimIndent()
