#version 150
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
};
#define ColorModulator ColorModulatorData
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#moj_import <minecraft:projection.glsl>
in vec4 vertexColor;
in vec2 texCoord;
uniform sampler2D Sampler0;
out vec4 fragColor;
void main(){fragColor=vertexColor*texture(Sampler0,texCoord);}
