#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec3 themeColor;
    bool themeSync;
    vec4 uDepthRange;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 0) out vec2 vTexCoord;


void main() {
    vTexCoord = UV0;
    vec4 pos = uProjMat * uModelViewMat * vec4(Position, 1.0);
    gl_Position = vec4(pos.xy, mix(-pos.w, 0.0, uDepthRange.x) * 0.9999, pos.w);
}