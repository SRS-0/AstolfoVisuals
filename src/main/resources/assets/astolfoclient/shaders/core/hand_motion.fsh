#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    float motionStrength;
};

uniform sampler2D currentTexture;
uniform sampler2D historyTexture;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 current = texture(currentTexture, texCoord);
    vec4 history = texture(historyTexture, texCoord);
    vec4 fadedHistory = history * motionStrength;
    if (fadedHistory.a < 0.02) {
        fadedHistory = vec4(0.0);
    }
    fragColor = current + fadedHistory * (1.0 - current.a);
}