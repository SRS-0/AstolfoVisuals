#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec3 themeColor;
    bool themeSync;
};

in vec3 Position;
in vec2 UV0;
out vec2 vTexCoord;


void main() {
    vTexCoord = UV0;
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, pos.w * 0.9999, pos.w);
}