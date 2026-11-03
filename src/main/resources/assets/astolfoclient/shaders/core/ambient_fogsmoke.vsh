#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float time;
    vec3 themeColor;
    float alpha;
    float speed;
    float density;
    float horizon;
};

in vec3 Position;
out vec3 lookDir;


void main() {
    lookDir = Position;
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);
}