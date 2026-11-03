#version 150

layout(std140) uniform EffectData {
    vec4 RangeData;
    vec4 ThicknessData;
    vec4 SmoothnessData;
    vec4 OutlineData;
    vec4 OutlineThicknessData;
    vec4 OutlineColorData;
};
#define Range RangeData.x
#define Thickness ThicknessData.x
#define Smoothness SmoothnessData.x
#define Outline (OutlineData.x != 0.0)
#define OutlineThickness OutlineThicknessData.x
#define OutlineColor OutlineColorData


in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;







out vec4 OutColor;

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