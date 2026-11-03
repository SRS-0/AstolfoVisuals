#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uInvViewProjMat;
    vec3 uSphereCenterRelCam;
    float uSphereRadius;
    float uSmoothness;
    float uBlurStrength;
    vec2 uResolution;
    int uSampleCount;
    int uBlurSky;
    vec3 uThemeColor;
    float uThemeTint;
    float uDepthZeroToOne;
};

layout(location = 0) in vec2 Position;
layout(location = 1) in vec2 UV0;
layout(location = 0) out vec2 vTexCoord;
void main() {
    vTexCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}