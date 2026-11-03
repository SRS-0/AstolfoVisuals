#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
};
#define ColorModulator ColorModulatorData
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#include <minecraft:projection.glsl>
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 0) out vec2 texCoord;



layout(location = 1) out vec4 vertexColor;
void main(){gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);vertexColor=Color*ColorModulator;texCoord=UV0;}
