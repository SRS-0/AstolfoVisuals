#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec4 uDepthRange;
};

layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, alpha);
}