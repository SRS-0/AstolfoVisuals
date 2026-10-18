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
};

layout(location = 0) out vec4 OutColor;

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}