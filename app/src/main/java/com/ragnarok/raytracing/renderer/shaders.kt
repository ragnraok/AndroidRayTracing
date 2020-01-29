package com.ragnarok.raytracing.renderer

import org.intellij.lang.annotations.Language

const val eps = 0.0001
const val bounces = 5
const val infinity = 10000.0

object PassConstants {
    var eachPassOutputWidth = 1024.0
    var eachPassOutputHeight = 1024.0
}

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

//// rays pdf

// simple pseudorandom-looking function in glsl from
// https://stackoverflow.com/questions/4200224/random-noise-functions-for-glsl
@Language("glsl")
val randomFunc = """
    float random(vec2 co, float bias){
        return saturateMediump(fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453 + bias));
    }
""".trimIndent()

@Language("glsl")
val randomFunc2 = """
    float random2(vec3 scale, float seed) {
        return saturateMediump(fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed));
    }
""".trimIndent()

@Language("glsl")
val randomFunc3 = """
    // uniform hash function https://www.shadertoy.com/view/4tXyWN
    float random3(int bias) {
        uvec2 x = uvec2(gl_FragCoord) + uint(${PassConstants.eachPassOutputWidth}) * uint(${PassConstants.eachPassOutputHeight}) * (uint(frame) + uint(bias));
        uvec2 q = 1103515245U * ( (x>>1U) ^ (x.yx   ) );
        uint  n = 1103515245U * ( (q.x  ) ^ (q.y>>3U) );
        return float(n) * (1.0/float(0xffffffffU));
    }
""".trimIndent()


const val piVal = "3.141592654"

@Language("glsl")
const val randomVec1a = "vec2(gl_FragCoord.x + time, gl_FragCoord.y + time)"

@Language("glsl")
const val randomVec2b = "vec2(gl_FragCoord.y + time, gl_FragCoord.x + time)"

@Language("glsl")
const val randomVec3a = "vec3(12.9898, 78.233, 151.7182)"

@Language("glsl")
const val randomVec3b = "vec3(63.7264, 10.873, 623.6736)"

// check https://raytracing.github.io/books/RayTracingTheRestOfYourLife.html#generatingrandomdirections
@Language("glsl")
val uniformRandomDirection = """
    vec3 uniformRandomDirection() {
        float r1 = random($randomVec1a, time);
        float r2 = random($randomVec2b, time);
        
        float z = 1.0 - 2.0 * r2;
        float phi = 2.0 * $piVal * r1;
        float x = cos(phi) * sqrt(r2);
        float y = sin(phi) * sqrt(r2);
        return vec3(x, y, z);
    }
""".trimIndent()

@Language("glsl")
val cosineWeightDirection = """
    #define N_POINTS 32.0
    vec3 cosineWeightDirection(vec3 normal, int bias) {
//        float r1 = random3(bias);
//        float r2 = random3(0);
//        float r = sqrt(r1);
//        float theta = 2.0 * $piVal * r2;
        
        float i = floor(N_POINTS * random3(0)) + (random3(0) * 0.5);
        // the Golden angle in radians
        float theta = i * 2.39996322972865332 + mod(float(frame), 2.0*$piVal);
        theta = mod(theta, 2.0*$piVal);
        float r = sqrt(i / N_POINTS); // sqrt pushes points outward to prevent clumping in center of disk


        float x = r * cos(theta);
        float y = r * sin(theta);
        float z = sqrt(1.0 - x * x - y * y); // unit sphere
        // calc new ortho normal basic
        vec3 s,t;
        if (abs(normal.x) < 0.5) {
            s = cross(normal, vec3(1, 0, 0));
        } else {
            s = cross(normal, vec3(0, 1, 0));
        }
        t = cross(normal, s);
        return x * s + y * t + z * normal;
    }
""".trimIndent()

val randomRayFunc = """
$randomFunc
$randomFunc2
$randomFunc3
$uniformRandomDirection
$cosineWeightDirection
""".trimIndent()

//// cornell box scene
@Language("glsl")
val intersectCubeFunc = """
    vec2 intersectCube(vec3 origin, vec3 ray, vec3 cubeMin, vec3 cubeMax) {
        vec3 tMin = (cubeMin - origin) / ray;
        vec3 tMax = (cubeMax - origin) / ray;
        vec3 t1 = min(tMin, tMax);
        vec3 t2 = max(tMin, tMax);
        float tNear = max(max(t1.x, t1.y), t1.z);
        float tFar = min(min(t2.x, t2.y), t2.z);
        return vec2(tNear, tFar);
    }
""".trimIndent()

@Language("glsl")
val cubeNormalFs = """
    vec3 normalForCube(vec3 hit, vec3 cubeMin, vec3 cubeMax) {
        if(hit.x < cubeMin.x + $eps) return vec3(-1.0, 0.0, 0.0);
        else if (hit.x > cubeMax.x - $eps) return vec3(1.0, 0.0, 0.0);
        else if (hit.y < cubeMin.y + $eps) return vec3(0.0, -1.0, 0.0);
        else if (hit.y > cubeMax.y - $eps) return vec3(0.0, 1.0, 0.0);
        else if (hit.z < cubeMin.z + $eps) return vec3(0.0, 0.0, -1.0);
        else return vec3(0.0, 0.0, 1.0);
    }
""".trimIndent()

@Language("glsl")
val roomCubeDefine = """
    vec3 roomCubeMin = vec3(-1.0, -1.0, -1.0);
    vec3 roomCubeMax = vec3(1.0, 1.0, 1.0);
""".trimIndent()

val cornellBoxFunc = """
$intersectCubeFunc
$cubeNormalFs
$roomCubeDefine
"""

@Language("glsl")
val testCubeDefine = """
    vec3 cubeAMin = vec3(-0.25, -1.0, -0.25);
    vec3 cubeAMax = vec3(0.25, -0.75, 0.25);
    
    vec3 cubeBMin = vec3(0.3, -0.5, -0.25);
    vec3 cubeBMax = vec3(0.8, -0.25, 0.25);
    
    vec3 cubeCMin = vec3(-0.8, 0.0, -0.25);
    vec3 cubeCMax = vec3(-0.3, 0.25, 0.25);
""".trimIndent()

@Language("glsl")
const val  backgroundColor = "vec3(0.75)"

@Language("glsl")
const val lightColor = "vec3(0.5, 0.5, 0.5)"

@Language("glsl")
const val lightPos = "vec3(-0.5, 0.5, 0.0)"

@Language("glsl")
val  calcColorFs = """
    vec3 calcColor(vec3 origin, vec3 ray, vec3 light) {
        vec3 colorMask = vec3(1.0);
        vec3 finalColor = vec3(0.0);

        for (int pass = 0; pass < $bounces; pass++) {
            vec2 tRoom = intersectCube(origin, ray, roomCubeMin, roomCubeMax);
            
            vec2 tCubeA = intersectCube(origin, ray, cubeAMin, cubeAMax);
            vec2 tCubeB = intersectCube(origin, ray, cubeBMin, cubeBMax);
            vec2 tCubeC = intersectCube(origin, ray, cubeCMin, cubeCMax);
            
            float t = $infinity;
            if (tRoom.x < tRoom.y) {
                t = tRoom.y;
            }

            if (tCubeA.x > 1.0 && tCubeA.x < tCubeA.y && tCubeA.x < t) {
                t = tCubeA.x;
            }
            
            if (tCubeB.x > 1.0 && tCubeB.x < tCubeB.y && tCubeB.x < t) {
                t = tCubeB.x;
            }
            
            if (tCubeC.x > 1.0 && tCubeC.x < tCubeC.y && tCubeC.x < t) {
                t = tCubeC.x;
            }
            
            if (t == $infinity) {
                break;
            }
            
            vec3 hit = origin + ray * t;
            vec3 normal = vec3(0.0);
            vec3 surfaceColor = $backgroundColor;
            bool hasCubeA = false;
            if (t == tRoom.y) {
                normal = -normalForCube(hit, roomCubeMin, roomCubeMax);
                
                float delta = 0.9999;
                if (hit.x < -1.0 * delta) {
                    surfaceColor = vec3(1.0, 0.3, 0.1);
                } else if (hit.x > delta) {
                    surfaceColor = vec3(0.3, 1.0, 0.1);
                }
                // create a new diffuse ray
                ray = normalize(cosineWeightDirection(normal, pass));
            } else {
                if (t == tCubeA.x && tCubeA.x < tCubeA.y) {
                    normal = normalForCube(hit, cubeAMin, cubeAMax);
                }
                if (t == tCubeB.x && tCubeB.x < tCubeB.y) {
                    normal = normalForCube(hit, cubeBMin, cubeBMax);
                }
                if (t == tCubeC.x && tCubeC.x < tCubeC.y) {
                    normal = normalForCube(hit, cubeCMin, cubeCMax);
                }
                
                ray = normalize(cosineWeightDirection(normal, pass));
            }
            
            vec3 lightDir = normalize(light - hit);
            float NdotL = max(dot(normal, lightDir), 0.0);

            float shadow = 1.0;
            
            // shadow ray test
            tCubeA = intersectCube(hit, lightDir, cubeAMin, cubeAMax);
            if (tCubeA.x > 0.0 && tCubeA.y > 0.0 && tCubeA.x < tCubeA.y) {
                shadow = 0.0;
            }
            
            tCubeB = intersectCube(hit, lightDir, cubeBMin, cubeBMax);
            if (tCubeB.x > 0.0 && tCubeB.y > 0.0 && tCubeB.x < tCubeB.y) {
                shadow = 0.0;
            }
            
            tCubeC = intersectCube(hit, lightDir, cubeCMin, cubeCMax);
            if (tCubeC.x > 0.0 && tCubeC.y > 0.0 && tCubeC.x < tCubeC.y) {
                shadow = 0.0;
            }
            
            colorMask *= surfaceColor;
            finalColor += colorMask * ($lightColor * NdotL * shadow);
            
            origin = hit;
        }
        
        return finalColor;
    }
""".trimIndent()

@Language("glsl")
val mathDefine = """
    #define MEDIUMP_FLT_MAX    65504.0
    #define saturateMediump(x) min(x, MEDIUMP_FLT_MAX)
""".trimIndent()

@Language("glsl")
val highpPrecisionDefine = """
    precision highp float;
    precision highp int;
""".trimIndent()

@Language("glsl")
val tracerFs = """
    #version 300 es
    $highpPrecisionDefine
    
    $mathDefine
    
    out vec4 FragColor;
    
    in vec3 traceRay;
    in vec3 eyePos;
    
    uniform float weight; // current render output weight mix with last pass output
    uniform float time; // tick to create diffuse/glossy ray
    uniform int frame;
    
    uniform sampler2D previous; // last pass output
    
    $randomRayFunc
        
    $cornellBoxFunc
    
    $testCubeDefine
    
    $calcColorFs

    
    void main() {
        float lightArea = 4.0 * $piVal * 0.2 * 0.2;
        vec3 lightRay = normalize($lightPos + uniformRandomDirection() * lightArea);
        vec3 color = calcColor(eyePos, traceRay, lightRay);
        vec2 coord = vec2(gl_FragCoord.x / ${PassConstants.eachPassOutputWidth}, gl_FragCoord.y / ${PassConstants.eachPassOutputHeight});
        vec3 previousColor = texture(previous, coord).rgb;
        FragColor = vec4(mix(color, previousColor, weight), 1.0);
//        FragColor = vec4(color, 1.0);
    }
""".trimIndent()
