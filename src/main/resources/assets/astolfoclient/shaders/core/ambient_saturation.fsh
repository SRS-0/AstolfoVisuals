#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    float uSaturation;
};

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;
uniform sampler2D uColorTexture;

void main() {
    vec4 baseColor = texture(uColorTexture, vTexCoord);
    float luma = dot(baseColor.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 sat = mix(vec3(luma), baseColor.rgb, uSaturation);
    fragColor = vec4(clamp(sat, 0.0, 1.0), baseColor.a);
}