#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 1) out vec2 TexCoord;
layout(location = 0) out vec4 FragColor;

void main() {
    TexCoord = UV0;
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}