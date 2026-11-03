#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    float alpha;
    int doBlur;
    float blurStrength;
    vec2 resolution;
    float time;
    vec3 themeColor;
    int useHandColor;
    int mode;
};

layout(location = 0) in vec2 Position;
layout(location = 1) in vec2 UV0;
layout(location = 0) out vec2 texCoord;
void main() {
    texCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}