package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.infinity
import com.ragnarok.raytracing.glsl.PassVariable.pi
import org.intellij.lang.annotations.Language

@Language("glsl")
val outputVs = """
    #version 300 es
    
    layout (location = 0) in vec3 aPos;
    layout (location = 1) in vec2 aTexCoords;

    out vec2 TexCoords;
    void main() {
        TexCoords = aTexCoords;
        gl_Position = vec4(aPos, 1.0);
    }
""".trimIndent()

@Language("glsl")
val outputFs = """
    #version 300 es
    precision highp float;
    precision highp int;
    
    out vec4 FragColor;
    in vec2 TexCoords;
    uniform sampler2D texture;
    
    uniform int toneMapping;
    
    vec3 toneMap(vec3 src) {
    	vec3 color = src / (1.0 + src);
    	color = pow(color,vec3(1.0/2.2,1.0/2.2,1.0/2.2));
    	return color;
    }

    void main() {
        vec3 color = texture(texture, TexCoords).rgb;
        if (toneMapping == 1) {
            color = toneMap(color);
        }
        FragColor = vec4(color, 1.0);
    }
""".trimIndent()

@Language("glsl")
val tracerVs = """
    #version 300 es

    layout (location = 0) in vec3 aPos;
    layout (location = 1) in vec2 aTexCoords;
    
    uniform mat4 model;
    uniform mat4 view;
    uniform mat4 projection;
    
    uniform vec3 eye;
    uniform vec3 ray00;
    uniform vec3 ray01;
    uniform vec3 ray10;
    uniform vec3 ray11;

    out vec3 traceRay;
    out vec3 eyePos;
    
    void main()
    {
        vec2 percent = aPos.xy * 0.5 + 0.5; // [-1, 1] to [0, 1]
        vec3 dir = mix(mix(ray00, ray01, percent.y), mix(ray10, ray11, percent.y), percent.x);
        eyePos = eye;
        traceRay = dir;
        gl_Position =  vec4(aPos, 1.0);
    }
""".trimIndent()

// main path tracing loop
@Language("glsl")
val traceLoop = """
    vec3 calcColor(Ray ray) {
        vec3 colorMask = vec3(1.0);
        vec3 finalColor = vec3(0.0);
        
        vec3 directionDir = directionLightDir(directionLight);
        vec3 pointDir = pointLightDir(pointLight);
        
        Material material;
        Intersection lastIntersect;
        Ray lastRay;
        float pdf = 1.0;
        for (int pass = 0; pass < ${PassVariable.bounces}; pass++) {
            Intersection intersect = intersectScene(ray);
            
            if (intersect.t == $infinity) {
                vec3 ambient = getSkyboxColorByRay(ray);
                Intersection lightIntersect = intersectPointLight(ray, pointLight);
                if (lightIntersect.nearFar.x > 0.0 && lightIntersect.nearFar.x < intersect.t) {
                    float t = lightIntersect.nearFar.x;
                    float lightPdf = (t * t) / (4.0 * $pi * pointLight.radius * pointLight.radius);
                    vec3 color = samplePointLight(pass, false, pdf, lightPdf, pointLight.color);
                    finalColor += color * colorMask * ambient;
                } else {
                    finalColor += ambient * colorMask;
                }
                break;
            }
            
            lastIntersect = intersect;
            vec3 pointLightDir = intersect.hit - pointDir; // point light to intersection

            vec3 directionLightDir = -directionDir;
            
            float shadow = 1.0;
            float specular = 0.0;
            bool isBRDFDiffuseRay = false;
            material = intersect.material;
            vec3 color = material.color;
            
            Ray newRay = materialRay(ray, intersect, directionLightDir, pass, specular, isBRDFDiffuseRay);
            
            shadow = getShadow(intersect, directionLightDir);
            
            // light color
            vec3 radiance = vec3(0.0);
            
            vec3 pointLightColor = pointLight.color * pointLightAttenuation(pointLight, intersect.hit) * pointLight.intensity;
            vec3 directionLightColor = directionLight.color;
            
            if (intersect.material.type == PBR_BRDF) {
                ray.pbrBRDF = true;
                newRay.pbrBRDF = true;
                newRay.pbrDiffuseRay = isBRDFDiffuseRay;
                vec3 viewDir = normalize(ray.origin - intersect.hit);
                // point light and direction light color
                radiance += brdfLightColor(intersect.normal, pointLightDir, viewDir, pointLightColor, intersect.material);
                radiance += brdfLightColor(intersect.normal, directionLightDir, viewDir, directionLightColor, intersect.material);
                
                // material diffuse and specular color
                colorMask *= brdfMaterialColor(intersect.normal, -ray.direction, newRay.origin, intersect.material, isBRDFDiffuseRay);
                pdf = brdfMaterialPdf(intersect.normal, -ray.direction, newRay.origin, intersect.material, isBRDFDiffuseRay);
                finalColor += colorMask * (radiance * shadow);
            } else {
                // diffuse
                colorMask *= color;
                
                // point light and direction light color
                float pointNdotL = max(dot(intersect.normal, -pointLightDir), 0.0);
                float directionNdotL = max(dot(intersect.normal, directionLightDir), 0.0);
                radiance = pointLightColor * pointNdotL;
                radiance += directionLightColor * directionNdotL;
                
                finalColor += colorMask * (radiance * shadow);
                finalColor += colorMask * specular * shadow;
                
                pdf = 1.0;
            }
            
            lastRay = ray;
            newRay.origin = intersect.hit;
            ray = newRay;
        }
        return finalColor;
    }
""".trimIndent()

val commonDataFunc = """
    $random
    $primitives
    $lights
    $intersections
    $skybox
""".trimIndent()

val tracerFs = { scene: String ->
    @Language("glsl")
    val shader = """
    #version 300 es
    precision highp float;
    precision highp int;
    
    out vec4 FragColor;
    
    in vec3 traceRay;
    in vec3 eyePos;
    
    uniform float weight; // current render output weight mix with last pass output
    uniform float time; // tick to create diffuse/glossy ray
    uniform int frame;
    
    uniform sampler2D previous; // last pass output
    
    uniform sampler2D skybox; // background skybox
    
    $commonDataFunc
    
    $scene
    
    $shadow

    $traceLoop

    void main() {
        Ray ray = Ray(eyePos, traceRay, false, false);
        vec3 color = calcColor(ray);
        color = max(vec3(0.0), color);
        vec2 coord = vec2(gl_FragCoord.x / ${PassVariable.eachPassOutputWidth}, gl_FragCoord.y / ${PassVariable.eachPassOutputHeight});
        vec3 previousColor = texture(previous, coord).rgb;
        FragColor = vec4(mix(color, previousColor, weight), 1.0);
    }
    
    """
    shader
}