#version 150
layout(std140) uniform EffectData {
    vec2 uOffset;
    vec2 uHalfPixel;
    vec3 color;
};

uniform sampler2D inTexture;


in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec2 halfPixel = uHalfPixel * uOffset;
    vec4 sum = texture(inTexture, uv + vec2(-halfPixel.x * 2.0, 0.0));
    sum += texture(inTexture, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(inTexture, uv + vec2(0.0, halfPixel.y * 2.0));
    sum += texture(inTexture, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;
    sum += texture(inTexture, uv + vec2(halfPixel.x * 2.0, 0.0));
    sum += texture(inTexture, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;
    sum += texture(inTexture, uv + vec2(0.0, -halfPixel.y * 2.0));
    sum += texture(inTexture, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;
    fragColor = vec4((sum / 12.0).rgb * color, (sum / 12.0).a);
}