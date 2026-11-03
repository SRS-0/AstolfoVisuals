#version 150
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

in vec2 Position;
in vec2 UV0;
out vec2 texCoord;
void main() {
    texCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}