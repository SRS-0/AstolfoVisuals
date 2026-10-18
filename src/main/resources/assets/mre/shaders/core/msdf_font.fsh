#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 TexCoord;
layout(location = 1) in vec4 FragColor;

uniform sampler2D Sampler0;
layout(std140) uniform MreParams {
    vec4 OutlineColor;
    float Range;
    float Thickness;
    float Smoothness;
    int Outline;
    float OutlineThickness;
};

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
    
    if (Outline != 0) {
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness, Smoothness, (dist + OutlineThickness) * pixels);
    }
    
    OutColor = color;
}