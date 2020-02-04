package com.ragnarok.raytracing.glsl

import com.ragnarok.raytracing.glsl.PassVariable.pi
import org.intellij.lang.annotations.Language

@Language("glsl")
const val skyBoxVs = """
    #version 300 es
    in vec3 aPos;
    uniform mat4 projection;
    uniform mat4 view;
    
    out vec3 TexCoords;
    
    void main()
    {
        TexCoords = aPos;
        vec4 pos = projection * view * vec4(aPos, 1.2);
        gl_Position = pos.xyww;
    } 
"""

@Language("glsl")
const val skyBoxFs = """
    #version 300 es
    out vec4 FragColor;

    in vec3 TexCoords;
    
    uniform samplerCube skybox;
    
    void main()
    {    
        vec3 color = texture(skybox, TexCoords).rgb;
        // HDR tonemapping
        color = color / (color + vec3(1.0));
        // gamma correct
        color = pow(color, vec3(1.0/2.2));
    
        FragColor = vec4(color, 1.0);
    }
"""

@Language("glsl")
const val CubeMapVs = """
    #version 300 es
    layout (location = 0) in vec3 aPos;
    
    out vec3 WorldPos;
    
    uniform mat4 projection;
    uniform mat4 view;
    
    void main()
    {
        WorldPos = aPos;  
        gl_Position =  projection * view * vec4(WorldPos, 1.0);
    }
"""

@Language("glsl")
const val CubeMapConversionFs = """
    #version 300 es
    out vec4 FragColor;
    in vec3 WorldPos;
    
    uniform sampler2D equirectangularMap;
    
    const vec2 invAtan = vec2(0.1591, 0.3183);
    vec2 SampleSphericalMap(vec3 v)
    {
        vec2 uv = vec2(atan(v.z, v.x), -asin(v.y));
        uv *= invAtan;
        uv += 0.5;
        return uv;
    }
    
    void main()
    {		
        vec2 uv = SampleSphericalMap(normalize(WorldPos));
        vec3 color = texture(equirectangularMap, uv).rgb;
        
        FragColor = vec4(color, 1.0);
    }

"""

@Language("glsl")
val getSkyboxColorByRay = """
    vec2 getSkyboxUVByRay(Ray ray) {
        vec3 p = ray.direction;
        // calculate spherical coordinate 
        float theta = acos(p.y);
        float phi = atan(p.z, p.x);
        if (phi < 0.0) {
            phi += 2.0 * $pi;
        }
        // from spherical coordinate to sphere uv
        vec2 s;
        s.x = 1.0 - phi * (1.0 / (2.0 * $pi));
        s.y = theta * (1.0 / $pi);
        return s;
    }
    vec3 getSkyboxColorByRay(Ray ray) {
        vec4 color = texture(skybox, getSkyboxUVByRay(ray));
        return color.rgb;
    }
""".trimIndent()

val skybox = """
    $getSkyboxColorByRay
""".trimIndent()