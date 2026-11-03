#version 150
layout(std140) uniform EffectData {
    mat4 uViewProjMat;
    vec2 uResolution;
    float uDistortionStrength;
    int uChromatic;
    int uNodeCount;
    vec3 uNodePos[36];
    float uNodeRadius[36];
    float uNodeAlpha[36];
};

in vec2 Position;
in vec2 UV0;
out vec2 vTexCoord;
void main() {
    vTexCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}