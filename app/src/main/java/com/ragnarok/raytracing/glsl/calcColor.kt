package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// main path tracing loop
@Language("glsl")
val calcColor = """
    vec3 calcColor(Ray ray) {
        vec3 radiance = vec3(0.0);
        vec3 throughput = vec3(1.0);
        
//        vec3 directionDir = directionLightDir(directionLight);
        vec3 pointDir = pointLightDir(pointLight);
        
        Material material;
        Intersection lastIntersect;
        Ray lastRay;
        float pdf = 1.0;
        bool specularBounce = false;
        vec3 ambient = vec3(0.0);
        for (int pass = 0; pass < ${PassVariable.bounces}; pass++) {
            Intersection lightIntersect = intersectPointLight(ray, pointLight);
            Intersection intersect = intersectScene(ray);
            
            
            if (intersect.t == ${PassVariable.infinity}) {
                ambient = getSkyboxColorByRay(ray);
                if (lightIntersect.nearFar.x > 0.0 && lightIntersect.nearFar.x < intersect.t) {
                    float t = lightIntersect.nearFar.x;
                    float lightPdf = (t * t) / (4.0 * ${PassVariable.pi} * pointLight.radius * pointLight.radius);
                    vec3 color = samplePointLight(pass, specularBounce, pdf, lightPdf, pointLight.color);
                    radiance += color * throughput * ambient;
                    intersect = lightIntersect;
                } else {       
                    radiance += ambient * throughput;
                }
                break;
            }
            
            lastIntersect = intersect;
            vec3 pointLightDir = intersect.hit - pointDir; // point light to intersection

//            vec3 directionLightDir = -directionDir;
            
            float shadow = 1.0;
            float specular = 0.0;
            bool isDiffuseRay = false;
            bool isGlassRay = false;
            material = intersect.material;
            vec3 color = material.color;
            
            Ray newRay = materialRay(ray, intersect, -pointLightDir, pass, specular, isDiffuseRay);
            
            shadow = getShadow(intersect, -pointLightDir);
            
            vec3 pointLightColor = pointLight.color * pointLightAttenuation(pointLight, intersect.hit) * pointLight.intensity;
//            vec3 directionLightColor = directionLight.color;

            ray.pbrBRDF = true;
            newRay.pbrBRDF = true;
            newRay.pbrDiffuseRay = isDiffuseRay;
            vec3 viewDir = normalize(lastIntersect.hit - intersect.hit);
            // point light and direction light color
            pointLightColor = brdfLightColor(intersect.normal, -pointLightDir, viewDir, pointLightColor, intersect.material) * throughput;
//            radiance += brdfLightColor(intersect.normal, directionLightDir, viewDir, directionLightColor, intersect.material);
            
            radiance += throughput * intersect.material.emissive;
            // material diffuse and specular color
            if (intersect.material.glass == false) {
                throughput *= brdfMaterialColor(intersect.normal, -ray.direction, ray.origin, intersect.material, isDiffuseRay);
                pdf = brdfMaterialPdf(intersect.normal, -ray.direction, ray.origin, intersect.material, isDiffuseRay);
                specularBounce = !isDiffuseRay;
            } else {
                throughput *= intersect.material.color;
                pdf = 1.0;
                specularBounce = true;
            }
            radiance += throughput * pointLightColor * shadow;
            
            lastRay = ray;
            newRay.origin = intersect.hit + newRay.direction * ${PassVariable.eps};
            ray = newRay;
            ray.time = mix(cameraShutterOpenTime, cameraShutterCloseTime, randSeed());
            
            // russian roulette
            float p = max(throughput.r, max(throughput.g, throughput.b));
            if (random(pass) > p) {
                break;
            }
            throughput *= 1.0 / p;
        }
        return max(radiance,vec3(0.0));
    }
""".trimIndent()