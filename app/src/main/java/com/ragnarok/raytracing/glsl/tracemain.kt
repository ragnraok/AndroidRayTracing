package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.infinity
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
    
    out vec4 FragColor;
    in vec2 TexCoords;
    uniform sampler2D texture;

    void main() {
        FragColor = texture(texture, TexCoords);
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
    vec3 calcColor(Ray ray, PointLight pointLight) {
        vec3 colorMask = vec3(1.0);
        vec3 finalColor = vec3(0.0);
        
        vec3 light = pointLightDir(pointLight);
        
        for (int pass = 0; pass < ${PassVariable.bounces}; pass++) {
            Intersection intersect = intersectScene(ray);
            if (intersect.t == $infinity) {
                break;
            }
            
            vec3 lightDir = light - intersect.hit;
            float shadow = 1.0;
            float specular = 0.0;
            vec3 color = intersect.color;
            
            ray = materialRay(ray, intersect, lightDir, pass, specular);
            
            shadow = getShadow(intersect, lightDir);
            
            colorMask *= intersect.color;
                        
            float NdotL = max(dot(intersect.normal, lightDir), 0.0);
            vec3 radiance = pointLight.color * pointLightAttenuation(pointLight, intersect.hit);
            finalColor += colorMask * (radiance * NdotL * shadow);
            finalColor += colorMask * specular * shadow;
            
            ray.origin = intersect.hit;
        }
        return finalColor;
    }
""".trimIndent()

val commonDataFunc = """
    $random
    $primitives
    $lights
    $intersections
""".trimIndent()

val scene = """
    $cornellBoxScene
    
""".trimIndent()

@Language("glsl")
val tracerFs = """
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
    
    $commonDataFunc
    
    $scene
    
    $shadow

    $traceLoop

    void main() {
        Ray ray = Ray(eyePos, traceRay);
        vec3 color = calcColor(ray, pointLight);
        color = max(vec3(0.0), color);
        vec2 coord = vec2(gl_FragCoord.x / ${PassVariable.eachPassOutputWidth}, gl_FragCoord.y / ${PassVariable.eachPassOutputHeight});
        vec3 previousColor = texture(previous, coord).rgb;
        FragColor = vec4(mix(color, previousColor, weight), 1.0);
    }
    
"""