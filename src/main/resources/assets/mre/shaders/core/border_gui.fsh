#version 150

flat in vec4 Effect0Value;
#define SizeData Effect0Value
flat in vec4 Effect1Value;
#define RadiusData Effect1Value
flat in vec4 Effect2Value;
#define ThicknessData Effect2Value
flat in vec4 Effect3Value;
#define SmoothnessData Effect3Value
#define Size SizeData.xy
#define Radius RadiusData
#define Thickness ThicknessData.x
#define Smoothness SmoothnessData.xy


#moj_import <mre:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;






out vec4 OutColor;

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