package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.eps
import org.intellij.lang.annotations.Language

@Language("glsl")
val shadow = """
    float getShadow(Intersection intersection, vec3 lightDir) {
        float shadow = 1.0;
        Ray shadowRay = Ray(intersection.hit + intersection.normal * $eps, lightDir);
        shadow = intersectShadowRay(shadowRay);
        return shadow;
    }
""".trimIndent()