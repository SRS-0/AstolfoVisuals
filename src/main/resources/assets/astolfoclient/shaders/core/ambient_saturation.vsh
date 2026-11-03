#version 150
layout(std140) uniform EffectData {
    float uSaturation;
};

in vec2 Position;
in vec2 UV0;
out vec2 vTexCoord;
void main() {
    vTexCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}