package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

@Language("glsl")
val commonTraceInputOutput = """
    out vec4 FragColor;
    
    in vec3 traceRay;
    in vec3 eyePos;
    in vec2 vPos;
    
    // camera data
    uniform mat4 cameraWorldMatrix;
    uniform float cameraAspect;
    uniform float cameraFov;
    uniform float cameraAperture;
    uniform float cameraFocusLength;
    uniform float cameraShutterOpenTime;
    uniform float cameraShutterCloseTime;
    
    uniform float weight; // current render output weight mix with last pass output
    uniform float time; // tick to create diffuse/glossy ray
    uniform int frame;
    
    uniform sampler2D previous; // last pass output
    
    uniform sampler2D skybox; // background skybox
""".trimIndent()


val bvhTraceInput = { vertexNum: Int, bvhNodeNum: Int ->
@Language("glsl")
val shader = """
    uniform vec3 vertices[$vertexNum];
    uniform vec3 bvhMinBounds[$bvhNodeNum];
    uniform vec3 bvhMaxBounds[$bvhNodeNum]
    uniform int bvhTriangleIndex[$bvhNodeNum];
    
//    uniform vec3 normals[$vertexNum];
//    uniform vec3 texCoords[$vertexNum]
""".trimIndent()

    shader
}

@Language("glsls")
val getRay = """
    Ray getInitRay() {
        vec2 jitter = vec2((ran.x - 0.5) / ${PassVariable.eachPassOutputWidth}, (ran.y - 0.5) / ${PassVariable.eachPassOutputHeight}) * 0.5;
        vec2 vPosJitter = vPos + jitter;
        vec3 direction = vec3(vPosJitter, -1.0) * vec3(cameraAspect, 1.0, cameraFov);
        direction = normalize(direction);
        vec3 origin = cameraWorldMatrix[3].xyz;
        direction = mat3(cameraWorldMatrix) * direction;
        return createRay(origin, direction);
    }
    
    // http://www.pbr-book.org/3ed-2018/Camera_Models/Projective_Camera_Models.html#TheThinLensModelandDepthofField
    Ray getInitRayWithDepthOfField() {
        vec2 jitter = vec2((ran.x - 0.5) / ${PassVariable.eachPassOutputWidth}, (ran.y - 0.5) / ${PassVariable.eachPassOutputHeight}) * 0.5;
        vec2 vPosJitter = vPos + jitter;
        vec3 direction = vec3(vPosJitter, -1.0) * vec3(cameraAspect, 1.0, cameraFov);
        direction = normalize(direction);
        
        vec2 lensPoints = cameraAperture * sampleCircle(vec2(randSeed(), randSeed()));
        // intersect ray with focus plane
        float t = cameraFocusLength / direction.z; // intersect ray distance t
        // calculate the intersect point on focus plane
        // note the inverse direction since the origin direction of the ray is point outward of the lens
        vec3 focusPoint = -direction * t; 
        vec3 origin = vec3(lensPoints, 0.0);
        direction = normalize(focusPoint - origin);
        
        origin = vec3(cameraWorldMatrix * vec4(origin, 1.0));
        direction = mat3(cameraWorldMatrix) * direction;

        return createRay(origin, direction);
    }
""".trimIndent()

val commonDataFunc = """
    $random
    $primitives
    $lights
    $intersections
    $skybox
""".trimIndent()