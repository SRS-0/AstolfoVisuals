#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float Time;
    vec3 Accent;
    vec4 ColorModulator;
    vec4 uDepthRange;
};

in vec3 Position;
out vec3 Direction;
out vec4 vColor;


void main() {
    Direction = Position;
    vColor = vec4(1.0);
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, mix(-pos.w, 0.0, uDepthRange.x) * 0.9999, pos.w);
}