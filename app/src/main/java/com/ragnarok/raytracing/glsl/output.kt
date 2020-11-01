package com.ragnarok.raytracing.glsl

import org.intellij.lang.annotations.Language

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
    precision highp float;
    precision highp int;
    
    out vec4 FragColor;
    in vec2 TexCoords;
    uniform sampler2D texture;
    
    uniform int toneMapping;
    
    vec3 toneMap(vec3 src) {
    	vec3 color = src / (1.0 + src);
    	color = pow(color,vec3(1.0/2.2,1.0/2.2,1.0/2.2));
    	return color;
    }

    void main() {
        vec3 color = texture(texture, TexCoords).rgb;
        if (toneMapping == 1) {
            color = toneMap(color);
        }
        FragColor = vec4(color, 1.0);
    }
""".trimIndent()