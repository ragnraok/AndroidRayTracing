package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

val bvhTraceFS = { vertexNum: Int, bvhNodeNum: Int ->
    @Language("glsl")
    val shader = """
    #version 300 es
    precision highp float;
    precision highp int;
    
    $commonTraceInputOutput
    
    $commonDataFunc
    
    ${bvhTraceInput(vertexNum, bvhNodeNum)}

    ${bvhCalcColor(vertexNum, bvhNodeNum)}
    
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