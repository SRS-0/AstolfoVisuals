#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
};

in vec3 Position;


void main() {
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = pos.xyww;
}