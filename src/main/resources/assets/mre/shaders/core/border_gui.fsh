#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) flat in vec4 Effect0Value;
#define SizeData Effect0Value
layout(location = 1) flat in vec4 Effect1Value;
#define RadiusData Effect1Value
layout(location = 2) flat in vec4 Effect2Value;
#define ThicknessData Effect2Value
layout(location = 3) flat in vec4 Effect3Value;
#define SmoothnessData Effect3Value
#define Size SizeData.xy
#define Radius RadiusData
#define Thickness ThicknessData.x
#define Smoothness SmoothnessData.xy


#include <mre:common.glsl>

layout(location = 5) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 4) in vec4 FragColor;






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