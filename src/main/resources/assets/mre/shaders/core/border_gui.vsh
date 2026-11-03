#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec2 EffectLocal;
layout(location = 1) in vec4 Effect0;
layout(location = 0) flat out vec4 Effect0Value;
layout(location = 2) in vec4 Effect1;
layout(location = 1) flat out vec4 Effect1Value;
layout(location = 3) in vec4 Effect2;
layout(location = 2) flat out vec4 Effect2Value;
layout(location = 4) in vec4 Effect3;
layout(location = 3) flat out vec4 Effect3Value;

#include <mre:common.glsl>

layout(location = 5) in vec3 Position; // POSITION_COLOR vertex attributes
layout(location = 6) in vec4 Color;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 5) out vec2 FragCoord;
layout(location = 4) out vec4 FragColor;

void main() {
    Effect0Value = Effect0;
    Effect1Value = Effect1;
    Effect2Value = Effect2;
    Effect3Value = Effect3;
    FragCoord = EffectLocal;
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}