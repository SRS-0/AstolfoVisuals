#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 EffectLocal;
layout(location = 1) in vec4 Effect0;
layout(location = 0) flat out vec4 Effect0Value;
layout(location = 2) in vec4 Effect1;
layout(location = 3) flat out vec4 Effect1Value;
layout(location = 3) in vec4 Effect2;
layout(location = 4) flat out vec4 Effect2Value;
layout(location = 4) in vec4 Effect3;
layout(location = 5) flat out vec4 Effect3Value;
layout(location = 5) in vec4 Effect4;
layout(location = 6) flat out vec4 Effect4Value;
layout(location = 6) in vec4 Effect5;
layout(location = 7) flat out vec4 Effect5Value;
layout(location = 7) in vec4 Effect6;
layout(location = 8) flat out vec4 Effect6Value;
layout(location = 8) in vec4 Effect7;
layout(location = 9) flat out vec4 Effect7Value;
layout(location = 9) in vec4 Effect8;
layout(location = 10) flat out vec4 Effect8Value;
layout(location = 10) in vec4 Effect9;
layout(location = 11) flat out vec4 Effect9Value;
layout(location = 11) in vec4 Effect10;
layout(location = 1) flat out vec4 Effect10Value;
layout(location = 12) in vec4 Effect11;
layout(location = 2) flat out vec4 Effect11Value;

#include <mre:common.glsl>

layout(location = 13) in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
layout(location = 14) in vec2 UV0;
layout(location = 15) in vec4 Color;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 13) out vec2 FragCoord;
layout(location = 14) out vec2 TexCoord;
layout(location = 12) out vec4 FragColor;

void main() {
    Effect0Value = Effect0;
    Effect1Value = Effect1;
    Effect2Value = Effect2;
    Effect3Value = Effect3;
    Effect4Value = Effect4;
    Effect5Value = Effect5;
    Effect6Value = Effect6;
    Effect7Value = Effect7;
    Effect8Value = Effect8;
    Effect9Value = Effect9;
    Effect10Value = Effect10;
    Effect11Value = Effect11;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    FragCoord = EffectLocal;
    TexCoord = UV0;
    FragColor = Color;
} 