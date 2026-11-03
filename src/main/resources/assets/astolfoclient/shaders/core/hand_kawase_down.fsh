#version 150
layout(std140) uniform EffectData {
    vec2 uOffset;
    vec2 uHalfPixel;
};

uniform sampler2D inTexture;

in vec2 texCoord;
out vec4 fragColor;
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