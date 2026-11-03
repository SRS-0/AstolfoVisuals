#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) flat in vec4 Effect0Value;
#define SizeData Effect0Value
layout(location = 1) flat in vec4 Effect1Value;
#define RadiusData Effect1Value
layout(location = 2) flat in vec4 Effect2Value;
#define SmoothnessData Effect2Value
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x


#include <mre:common.glsl>

layout(location = 4) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 5) in vec2 TexCoord;
layout(location = 3) in vec4 FragColor;

uniform sampler2D Sampler0;




layout(location = 0) out vec4 OutColor;

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}