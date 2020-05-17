package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.pi
import org.intellij.lang.annotations.Language

// some util shader function defines

// simple pseudorandom-looking function in glsl from
// https://stackoverflow.com/questions/4200224/random-noise-functions-for-glsl
@Language("glsl")
val randomFunc = """
    float random1(vec2 co, float bias){
        return fract(sin(dot(co.xy, vec2(12.9898,78.233))) * 43758.5453 + bias);
    }
""".trimIndent()

@Language("glsl")
val randomFunc2 = """
    float random2(vec3 scale, float seed) {
        return fract(sin(dot(gl_FragCoord.xyz + seed, scale)) * 43758.5453 + seed);
    }
""".trimIndent()

@Language("glsl")
val randomFunc3 = """
    // uniform hash function https://www.shadertoy.com/view/4tXyWN
    float random(int bias) {
        uvec2 x = uvec2(gl_FragCoord) + uint(${PassVariable.eachPassOutputWidth}) * uint(${PassVariable.eachPassOutputHeight}) * (uint(frame) + uint(bias));
        uvec2 q = 1103515245U * ( (x>>1U) ^ (x.yx   ) );
        uint  n = 1103515245U * ( (q.x  ) ^ (q.y>>3U) );
        return float(n) * (1.0/float(0xffffffffU));
    }
""".trimIndent()

@Language("glsl")
val ranomdFunc4 = """
    vec2 seed;
    uniform vec2 ran;
    float randSeed(){
        seed -= vec2(ran.x * ran.y);
        return fract(sin(dot(seed, vec2(12.9898, 78.233))) * 43758.5453);
    }
""".trimIndent()

@Language("glsl")
const val randomVec1a = "vec2(gl_FragCoord.x + time, gl_FragCoord.y + time)"

@Language("glsl")
const val randomVec2b = "vec2(gl_FragCoord.y + time, gl_FragCoord.x + time)"

@Language("glsl")
const val randomVec3a = "vec3(12.9898, 78.233, 151.7182)"

@Language("glsl")
const val randomVec3b = "vec3(63.7264, 10.873, 623.6736)"

@Language("glsl")
val sampleCircle = """
    // http://www.pbr-book.org/3ed-2018/Monte_Carlo_Integration/2D_Sampling_with_Multidimensional_Transformations.html#SamplingaUnitDisk
    vec2 sampleCircle(vec2 p) {
      p = 2.0 * p - 1.0;

      bool greater = abs(p.x) > abs(p.y);

      float r = greater ? p.x : p.y;
      float theta = greater ? 0.25 * $pi * p.y / p.x : $pi * (0.5 - 0.25 * p.x / p.y);

      return r * vec2(cos(theta), sin(theta));
    }
""".trimIndent()

@Language("glsl")
val cosineSampleHemisphere = """
    vec3 CosineSampleHemisphere(float u1, float u2){
        vec3 dir;
        float r = sqrt(u1);
        float phi = 2.0 * ${PassVariable.pi} * u2;
        dir.x = r * cos(phi);
        dir.y = r * sin(phi);
        dir.z = sqrt(max(0.0, 1.0 - dir.x*dir.x - dir.y*dir.y));

        return dir;
    }
""".trimIndent()

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

@Language("glsl")
val commonDefine = """
    #define MEDIUMP_FLT_MAX    65504.0
    #define MEDIUMP_FLT_MIN    0.00006103515625
    #define saturateMediump(x) min(x, MEDIUMP_FLT_MAX)
""".trimIndent()

val random = """
    $randomFunc3
    $commonDefine
    $ranomdFunc4
    $cosineSampleHemisphere
    $uniformRandomDirection
    $sampleCircle
""".trimIndent()