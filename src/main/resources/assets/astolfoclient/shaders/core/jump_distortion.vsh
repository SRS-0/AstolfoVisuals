#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uInvViewProjMat;
    mat4 uViewProjMat;
    vec2 uResolution;
    float uDistortionStrength;
    int uChromatic;
    int uCircleCount;
    vec3 uCirclePos[16];
    float uCircleRadius[16];
    float uCircleProgress[16];
    float uCircleAlpha[16];
    float uDepthZeroToOne;
};

layout(location = 0) in vec2 Position;
layout(location = 1) in vec2 UV0;
layout(location = 0) out vec2 vTexCoord;
void main() {
    vTexCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}