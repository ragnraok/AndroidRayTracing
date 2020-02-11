package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

@Language("glsl")
val shadow = """
    float getShadow(Intersection intersection, vec3 lightDir) {
        float shadow = 1.0;
        Ray shadowRay = Ray(intersection.hit + intersection.normal * ${PassVariable.eps}, lightDir, false, false);
        shadow = $intersectShadowRay(shadowRay);
        return shadow;
    }
""".trimIndent()