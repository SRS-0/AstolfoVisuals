#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec4 uDepthRange;
};

in vec3 Position;


void main() {
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, mix(-pos.w, 0.0, uDepthRange.x), pos.w);
}