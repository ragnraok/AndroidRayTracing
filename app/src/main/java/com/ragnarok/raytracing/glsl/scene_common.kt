package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

const val intersectScene = "intersectScene"

const val intersectShadowRay = "intersectShadowRay"

@Language("glsl")
val intersectSceneFuncHead = "Intersection $intersectScene(Ray ray)"

@Language("glsl")
val intersectShadowRayFuncHead = "float $intersectShadowRay(Ray shadowRay)"

