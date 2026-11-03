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
    if (mask.a < 0.01) discard;
    vec3 gradientColor = (useHandColor == 1) ? mask.rgb : mix(glowColor1, glowColor2, uv.y);
    float edgeGlow = 1.0 - bloom.a;
    float totalIntensity = (0.5 + edgeGlow * 0.5) * mask.a;
    fragColor = vec4(gradientColor, totalIntensity * alpha);
}