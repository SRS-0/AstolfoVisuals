#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) flat in vec4 Effect0Value;
#define RangeData Effect0Value
layout(location = 1) flat in vec4 Effect1Value;
#define ThicknessData Effect1Value
layout(location = 2) flat in vec4 Effect2Value;
#define SmoothnessData Effect2Value
layout(location = 3) flat in vec4 Effect3Value;
#define OutlineData Effect3Value
layout(location = 4) flat in vec4 Effect4Value;
#define OutlineThicknessData Effect4Value
layout(location = 5) flat in vec4 Effect5Value;
#define OutlineColorData Effect5Value
#define Range RangeData.x
#define Thickness ThicknessData.x
#define Smoothness SmoothnessData.x
#define Outline (OutlineData.x != 0.0)
#define OutlineThickness OutlineThicknessData.x
#define OutlineColor OutlineColorData


layout(location = 7) in vec2 TexCoord;
layout(location = 6) in vec4 FragColor;

uniform sampler2D Sampler0;







layout(location = 0) out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    float dist = median(texture(Sampler0, TexCoord).rgb) - 0.5 + Thickness;
    vec2 h = vec2(dFdx(TexCoord.x), dFdy(TexCoord.y)) * textureSize(Sampler0, 0);
    float pixels = Range * inversesqrt(h.x * h.x + h.y * h.y);
    float alpha = smoothstep(-Smoothness, Smoothness, dist * pixels);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);
    
    if (Outline) {
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness, Smoothness, (dist + OutlineThickness) * pixels);
    }
    
    OutColor = color;
}