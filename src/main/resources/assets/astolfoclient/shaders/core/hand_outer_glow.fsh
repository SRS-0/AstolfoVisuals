#version 150
layout(std140) uniform EffectData {
    vec3 glowColor1;
    vec3 glowColor2;
    float alpha;
    int useHandColor;
};

uniform sampler2D bloomTexture;
uniform sampler2D maskTexture;




in vec2 texCoord;
out vec4 fragColor;
void main() {
    vec2 uv = texCoord;
    vec4 bloom = texture(bloomTexture, uv);
    vec4 mask = texture(maskTexture, uv);
    float baseGlow = bloom.a * (1.0 - mask.a);
    vec3 gradientColor = (useHandColor == 1) ? (bloom.rgb / max(bloom.a, 0.001)) : mix(glowColor1, glowColor2, uv.y);
    fragColor = vec4(gradientColor, baseGlow * alpha);
}