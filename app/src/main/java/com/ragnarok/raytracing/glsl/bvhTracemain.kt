package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

val bvhTraceFS = { vertexNum: Int, bvhNodeNum: Int ->
    @Language("glsl")
    val shader = """
    #version 320 es
    #extension GL_EXT_texture_buffer : require
    precision highp float;
    precision highp int;
    precision highp samplerBuffer;
    
    $commonTraceInputOutput
    
    $commonDataFunc
    
    Material material = createEmissiveMaterial(vec3(0.75), vec3(0.75), 0.01, 1.0);
    
    PointLight pointLight = PointLight(vec3(0.0, 0.95, 0.0), 0.1, vec3(1.0), 3.0);
    
    ${bvhTraceInput(vertexNum, bvhNodeNum)}

    $bvhCalcColor
    
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