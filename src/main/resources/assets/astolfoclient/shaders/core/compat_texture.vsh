#version 150
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
};
#define ColorModulator ColorModulatorData
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#moj_import <minecraft:projection.glsl>
in vec3 Position;
in vec4 Color;
in vec2 UV0;
out vec2 texCoord;



out vec4 vertexColor;
void main(){gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);vertexColor=Color*ColorModulator;texCoord=UV0;}
