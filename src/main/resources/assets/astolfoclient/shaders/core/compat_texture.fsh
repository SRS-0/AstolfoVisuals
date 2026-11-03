#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
};
#define ColorModulator ColorModulatorData
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#include <minecraft:projection.glsl>
layout(location = 1) in vec4 vertexColor;
layout(location = 0) in vec2 texCoord;
uniform sampler2D Sampler0;
layout(location = 0) out vec4 fragColor;
void main(){fragColor=vertexColor*texture(Sampler0,texCoord);}
