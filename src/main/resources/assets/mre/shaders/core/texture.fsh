#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform EffectData {
    vec4 SizeData;
    vec4 RadiusData;
    vec4 SmoothnessData;
};
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x


#include <mre:common.glsl>

layout(location = 1) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 2) in vec2 TexCoord;
layout(location = 0) in vec4 FragColor;

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