#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    vec2 uOffset;
    vec2 uHalfPixel;
};

uniform sampler2D inTexture;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec2 halfPixel = uHalfPixel * uOffset;
    vec4 sum = texture(inTexture, uv) * 4.0;
    sum += texture(inTexture, uv - halfPixel);
    sum += texture(inTexture, uv + halfPixel);
    sum += texture(inTexture, uv + vec2(halfPixel.x, -halfPixel.y));
    sum += texture(inTexture, uv - vec2(halfPixel.x, -halfPixel.y));
    fragColor = sum / 8.0;
}