#version 330
#extension GL_ARB_separate_shader_objects : require

#include <mre:common.glsl>
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position; // POSITION_COLOR vertex attributes
layout(location = 1) in vec4 Color;


layout(location = 0) out vec2 FragCoord;
layout(location = 1) out vec4 FragColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexIndex);
    FragColor = Color;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}