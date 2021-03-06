package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.pi
import org.intellij.lang.annotations.Language

@Language("glsl")
val DFG = """
    // ----------------------------------------------------------------------------
    float DistributionGGX(vec3 N, vec3 H, float roughness) 
    {
//        float a = roughness*roughness;
//        float a2 = a*a;
//        float NdotH = max(dot(N, H), 0.0);
//        float NdotH2 = NdotH*NdotH;
//
//        float nom   = a2;
//        float denom = (NdotH2 * (a2 - 1.0) + 1.0);
//        denom = PI * denom * denom;
//
//        return saturateMediump(nom / denom);

        // better ndf with spot light shape
        vec3 NxH = cross(N, H);
        float oneMinusNoHSquared = dot(NxH, NxH);
        float NoH = max(dot(N, H), 0.0);
        float a = NoH * roughness;
        float k = roughness / (oneMinusNoHSquared + a * a);
        float d = k * k * (1.0 / $pi);
        return saturateMediump(d);
    }
    // ----------------------------------------------------------------------------
    float GeometrySchlickGGX(float NdotV, float roughness)
    {
        float r = (roughness + 1.0);
        float k = (r*r) / 8.0;

        float nom   = NdotV;
        float denom = NdotV * (1.0 - k) + k;

        return saturateMediump(nom / denom);
    }
    // ----------------------------------------------------------------------------
    float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
    {
        float NdotV = max(dot(N, V), 0.0);
        float NdotL = max(dot(N, L), 0.0);
        float ggx2 = GeometrySchlickGGX(NdotV, roughness);
        float ggx1 = GeometrySchlickGGX(NdotL, roughness);

        return saturateMediump(ggx1 * ggx2);
    }
    // ----------------------------------------------------------------------------
    vec3 fresnelSchlick(float cosTheta, vec3 F0)
    {
        return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
    }
    // ----------------------------------------------------------------------------
    vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness)
    {
        return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(1.0 - cosTheta, 5.0);
    } 
      
    vec3 EnvDFGLazarov( vec3 specularColor, float gloss, float ndotv ) {
        //# [ Lazarov 2013, "Getting More Physical in Call of Duty: Black Ops II" ]
        //# Adaptation to fit our G term.
        vec4 p0 = vec4( 0.5745, 1.548, -0.02397, 1.301 );
        vec4 p1 = vec4( 0.5753, -0.2511, -0.02066, 0.4755 );
        vec4 t = gloss * p0 + p1;
        float bias = clamp( t.x * min( t.y, exp2( -7.672 * ndotv ) ) + t.z, 0.0, 1.0);
        float delta = clamp( t.w, 0.0, 1.0);
        float scale = delta - bias;
        bias *= clamp( 50.0 * specularColor.y, 0.0, 1.0);
        return specularColor * scale + bias;
    }    
    
    float SchlickFresnelFloat(float u) { // F0 is 1.0
        float m = clamp(1.0 - u, 0.0, 1.0);
        float m2 = m * m;
        return m2 * m2*m; // pow(m,5)
    }
    
    float SchlickFresnelFloatR0(float cosTheta, float R0) { // F0 is 1.0
        return mix(SchlickFresnelFloat(cosTheta), 1.0, R0);
    }
    
    // Computes Schlick's approximation of Fresnel factor
    // Accounts for total internal reflection if ray is moving from a more dense to a less dense medium
    float SchlickFresnelFloatR0WithTIR(float cosTheta, float r0, float ni) {

      // moving from a more dense to a less dense medium
      if (cosTheta < 0.0) {
        float inv_eta = ni;
        float SinT2 = inv_eta * inv_eta * (1.0f - cosTheta * cosTheta);
        if (SinT2 > 1.0) {
            return 1.0; // total internal reflection
        }
        cosTheta = sqrt(1.0f - SinT2);
      }

      return mix(SchlickFresnelFloat(cosTheta), 1.0, r0);
    }
""".trimIndent()

@Language("glsl")
val brdfLightColor = """
    // direct light calculation
    vec3 brdfLightColor(vec3 N, vec3 L, vec3 V, vec3 lightColor, Material material) {
        vec3 baseColor = material.color;
        float roughness = material.roughness;
        float metallic = material.metallic;
        
        vec3 F0 = vec3(0.08);
        F0 = mix(F0, baseColor, metallic);
        
        vec3 H = normalize(V + L);
        float NdotL = max(0.0, dot(N, L));
        float NdotH = max(0.0, dot(N, H));
        float VdotH = max(0.0, dot(V, H));
        float NdotV = max(0.0, dot(N, V));
         
        float NDF = DistributionGGX(N, H, roughness);
        float G = GeometrySmith(N, V, L, roughness);
        vec3 F = fresnelSchlick(VdotH, F0);
        
         vec3 nominator    = NDF * G * F;
         float denominator = 4.0 * NdotV * NdotL;
         vec3 specularColor = nominator / max(denominator, 0.001); // prevent divide by zero for NdotV=0.0 or NdotL=0.0
        
        vec3 diffuseColor = baseColor / $pi;
        
        vec3 radiance = (diffuseColor + specularColor) * lightColor * NdotL;
        
        return radiance;
    }
""".trimIndent()

// specular radiance
@Language("glsl")
val importSampleGGX = """
    vec3 ImportanceSampleGGX(vec2 Xi, vec3 N, float roughness)
    {
        float a = roughness * roughness;
    
        float phi = 2.0 * $pi * Xi.x;
        float cosTheta = sqrt((1.0 - Xi.y) / (1.0 + (a*a - 1.0) * Xi.y));
        float sinTheta = sqrt(1.0 - cosTheta*cosTheta);
    
        // from spherical coordinates to cartesian coordinates - halfway vector
        vec3 H;
        H.x = cos(phi) * sinTheta;
        H.y = sin(phi) * sinTheta;
        H.z = cosTheta;
    
        // from tangent-space H vector to world-space sample vector
        vec3 up          = abs(N.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
        vec3 tangent   = normalize(cross(up, N));
        vec3 bitangent = cross(N, tangent);
    
        vec3 sampleVec = tangent * H.x + bitangent * H.y + N * H.z;
        return normalize(sampleVec);
    }    
""".trimIndent()

// diffuse irradiance
@Language("glsl")
val brdfMaterialColor = """
    // irradiance/radiance environment BRDF
    vec3 brdfMaterialColor(vec3 N, vec3 L, vec3 V, Material material, bool diffuse) {
        vec3 baseColor = material.color;
        float metallic = material.metallic;
        float roughness = material.roughness;
        N = normalize(N);
        L = normalize(L);
        V = normalize(V);
        
        vec3 H = normalize(V + L);
        float NdotL = max(0.0, dot(N, L));
        float NdotH = max(0.0, dot(N, H));
        float VdotH = max(0.0, dot(V, H));
        float NdotV = max(0.0, dot(N, V));

        vec3 color = vec3(0);
        
        vec3 F0 = vec3(0.08);
        F0 = mix(F0, baseColor, metallic);
        
        vec3 F = fresnelSchlickRoughness(VdotH, F0, roughness);

        if (diffuse) {
            if (NdotL > 0.0) {
                color = baseColor;
            }
        } else {
            // Specular Environment BRDF
            // https://learnopengl-cn.github.io/07%20PBR/03%20IBL/02%20Specular%20IBL/#brdf
            float NDF = DistributionGGX(N, H, roughness);
            float G = GeometrySmith(N, V, L, roughness);
            float Vis = (G * VdotH) / (NdotH * NdotV);
            
            // specular = D*G*F / (4*NoL*NoV) 
            //          = D*Vis*F
            //      Vis = G / 4 * NdotL * NdotV
            vec3 specularColor =  Vis * F;

            specularColor *= NdotL;

            color = specularColor;
        }
        return color;
    }
""".trimIndent()

@Language("glsl")
val brdfMaterialPdf = """
    float brdfMaterialPdf(vec3 N, vec3 L, vec3 V, Material material, bool diffuse) {
        vec3 baseColor = material.color;
        float metallic = material.metallic;
        float roughness = material.roughness;
        N = normalize(N);
        L = normalize(L);
        V = normalize(V);

        vec3 H = normalize(V + L);
        float NdotL = max(0.0, dot(N, L));
        float NdotH = max(0.0, dot(N, H));
        float VdotH = max(0.0, dot(V, H));

        
        if (diffuse) {
            return NdotL / $pi;
        } else {

            float NDF = DistributionGGX(N, H, roughness);
            return NDF * NdotH / (4.0 * VdotH);
        }
        
    }
""".trimIndent()

@Language("glsl")
val brdfRayDir = """
    vec3 brdfRayDir(Intersection intersection, Ray ray, int bias, out bool isDiffuseRay) {
        float u = random(bias);
        float v = random(frame);
        vec2 uv = vec2(u, v);
        
        vec3 N = intersection.normal;
        vec3 V = normalize(reflect(ray.direction, N));
        
        Material material = intersection.material;
        
        float metallic = material.metallic;
        float roughness = material.roughness;
        vec3 dir;
        float diffuseRatio = 0.5 * (1.0 - metallic);
        
        vec3 ffnormal = dot(N, V) <= 0.0 ? N : N * -1.0;
        float n1 = 1.0;
        float n2 = material.ior;
        float R0 = (n1 - n2) / (n1 + n2);
        R0 *= R0;
        float theta = dot(V * -1.0, ffnormal);
        float specularRatio = SchlickFresnelFloatR0(theta, R0);
        
        float prob = mix(diffuseRatio, 1.0, specularRatio);
        
        if (random(bias) < prob) {
            vec3 up = abs(N.z) < 0.999 ? vec3(0.0, 0.0, 1.0) : vec3(1.0, 0.0, 0.0);
            vec3 tangentX = normalize(cross(up, N));
            vec3 tangentY = normalize(cross(tangentX, N));
            
            // diffuse irradiance sample
            dir = CosineSampleHemisphere(u, v);
            dir = tangentX * dir.x  + tangentY * dir.y + N * dir.z;
            dir = normalize(dir);
            isDiffuseRay = true;
            return dir;
        } else {
            // specular radiance sample
            dir = ImportanceSampleGGX(uv, V, roughness);
            isDiffuseRay = false;
            return dir;
        }
    }
""".trimIndent()

@Language("glsl")
val samplePointLight = """
    // http://www.pbr-book.org/3ed-2018/Monte_Carlo_Integration/Importance_Sampling.html#MultipleImportanceSampling
    float powerHeuristic(float a, float b) {
        float t = a * a;
        return t / (b*b + t);
    }
    
    vec3 samplePointLight(int depth, bool specularBounce, float brdfPdf, float lightPdf, vec3 emission) {
        vec3 Le;

        if (depth == 0 || specularBounce)
            Le = emission;
        else
            Le = powerHeuristic(brdfPdf, lightPdf) * emission / lightPdf;

        return Le;
    }
""".trimIndent()

@Language("glsl")
val btdfRayDir = """
    vec3 btdfRayDir(Intersection intersection, Ray ray, int bias) {
        Material material = intersection.material;
        vec3 N = intersection.normal;
        vec3 V = ray.direction;
        float IOR = material.ior;
        
        vec3 ffnormal = dot(N, V) <= 0.0 ? N : N * -1.0;
        float n1 = 1.0;
        float n2 = IOR;
        float R0 = (n1 - n2) / (n1 + n2);
        R0 *= R0;
        float theta = dot(V * -1.0, ffnormal);
        float prob = SchlickFresnelFloatR0WithTIR(theta, R0, IOR);
        
        vec3 dir;
        float eta = dot(N, ffnormal) > 0.0 ? (n1 / n2) : (n2 / n1);
    
        // something wrong here
        if (random(bias) < prob) // Reflection
        {
            dir = normalize(reflect(ray.direction, ffnormal));
        }
        else
        {
            dir = normalize(refract(ray.direction, ffnormal, eta)); 
        }
        
        return dir;
    }
""".trimIndent()

val brdf = """
    $DFG
    $brdfLightColor
    $importSampleGGX
    $brdfMaterialColor
    $brdfMaterialPdf
    $brdfRayDir
    $samplePointLight
    $btdfRayDir
""".trimIndent()