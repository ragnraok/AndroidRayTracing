package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

// some util shader function defines

// simple pseudorandom-looking function in glsl from
// https://stackoverflow.com/questions/4200224/random-noise-functions-for-glsl
@Language("glsl")
val randomFunc = """
    float random1(vec2 co, float bias){
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
    float random(int bias) {
        uvec2 x = uvec2(gl_FragCoord) + uint(${PassVariable.eachPassOutputWidth}) * uint(${PassVariable.eachPassOutputHeight}) * (uint(frame) + uint(bias));
        uvec2 q = 1103515245U * ( (x>>1U) ^ (x.yx   ) );
        uint  n = 1103515245U * ( (q.x  ) ^ (q.y>>3U) );
        return float(n) * (1.0/float(0xffffffffU));
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

val random = """
    $randomFunc3
""".trimIndent()