#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform EffectData {
    vec4 SizeData;
    vec4 RadiusData;
    vec4 ThicknessData;
    vec4 SmoothnessData;
};
#define Size SizeData.xy
#define Radius RadiusData
#define Thickness ThicknessData.x
#define Smoothness SmoothnessData.xy


#include <mre:common.glsl>

layout(location = 1) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 0) in vec4 FragColor;






layout(location = 0) out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = rdist(center - (FragCoord.xy * Size), center - 1.0, Radius);
    float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y,
        1.0 - Thickness - Smoothness.y, dist); // internal edge
    alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, dist); // external edge
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}