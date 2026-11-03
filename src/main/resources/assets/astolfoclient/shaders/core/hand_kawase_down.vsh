#version 150
layout(std140) uniform EffectData {
    vec2 uOffset;
    vec2 uHalfPixel;
};

in vec2 Position;
in vec2 UV0;
out vec2 texCoord;
void main() {
    texCoord = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}