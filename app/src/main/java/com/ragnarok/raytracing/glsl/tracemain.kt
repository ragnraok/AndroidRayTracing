package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

@Language("glsl")
val traceVS = """
    #version 320 es

    layout (location = 0) in vec3 aPos;
    layout (location = 1) in vec2 aTexCoords;
    
    uniform mat4 model;
    uniform mat4 view;
    uniform mat4 projection;
    
    uniform vec3 eye;

    out vec3 traceRay;
    out vec3 eyePos;
    out vec2 vPos;
    
    void main()
    {
        vec2 percent = aPos.xy * 0.5 + 0.5; // [-1, 1] to [1, 1]
        vPos = aPos.xy;
        eyePos = eye;
        gl_Position =  vec4(aPos, 1.0);
    }
""".trimIndent()

val traceFS = { scene: String ->
    @Language("glsl")
    val shader = """
    #version 320 es
    precision highp float;
    precision highp int;
    
    $commonTraceInputOutput
    
    $commonDataFunc
    
    $scene
    
    $shadow

    $calcColor
    
    $getRay

    void main() {
        Ray ray;
        if (cameraAperture > 0.0 && cameraFocusLength > 0.0) {
            ray = getInitRayWithDepthOfField();
        } else {
            ray = getInitRay();
        }
        if (cameraShutterOpenTime >= 0.0 && cameraShutterCloseTime > 0.0 && cameraShutterCloseTime > cameraShutterOpenTime) {
            ray.time = mix(cameraShutterOpenTime, cameraShutterCloseTime, randSeed());
        }
        vec3 color = calcColor(ray);
        color = max(vec3(0.0), color);
        vec2 coord = vec2(gl_FragCoord.x / ${PassVariable.eachPassOutputWidth}, gl_FragCoord.y / ${PassVariable.eachPassOutputHeight});
        vec3 previousColor = texture(previous, coord).rgb;
        FragColor = vec4(mix(color, previousColor, weight), 1.0);
    }
    
    """
    shader
}