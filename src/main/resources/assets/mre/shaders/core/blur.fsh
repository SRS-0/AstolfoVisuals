#version 330
#extension GL_ARB_separate_shader_objects : require

#include <mre:common.glsl>

layout(location = 0) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 1) in vec2 TexCoord;
layout(location = 2) in vec4 FragColor;

uniform sampler2D Sampler0;
layout(std140) uniform MreParams {
    vec4 Radius;
    vec2 Size;
    float Smoothness;
    float BlurRadius;
};

layout(location = 0) out vec4 OutColor;

const float DPI = 6.28318530718;
const float STEP = DPI / 16.0;

void main() {
    vec2 multiplier = BlurRadius / textureSize(Sampler0, 0);
    
    vec3 average = texture(Sampler0, TexCoord).rgb;
    for (float d = 0.0; d < DPI; d += STEP) {
        for (float i = 0.2; i <= 1.0; i += 0.2) {
            average += texture(Sampler0, TexCoord + vec2(cos(d), sin(d)) * multiplier * i).rgb;
        }
    }
    average /= 80.0;

    vec4 color = vec4(average, 1.0) * FragColor;
    color.a *= ralpha(Size, FragCoord, Radius, Smoothness);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}