#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float Time;
    vec3 Accent;
    vec4 ColorModulator;
    vec4 uDepthRange;
};

layout(location = 0) in vec3 Position;
layout(location = 0) out vec3 Direction;
layout(location = 1) out vec4 vColor;


void main() {
    Direction = Position;
    vColor = vec4(1.0);
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, mix(-pos.w, 0.0, uDepthRange.x) * 0.9999, pos.w);
}