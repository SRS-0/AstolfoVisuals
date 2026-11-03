#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
    vec4 uResolutionData;
    vec4 uTimeData;
    vec4 uColor1Data;
    vec4 uColor2Data;
    vec4 uBlockCenterData;
    vec4 uAlphaModeData;
};
#define ColorModulator ColorModulatorData
#define uResolution uResolutionData.xy
#define uTime uTimeData.x
#define uColor1 uColor1Data.xyz
#define uColor2 uColor2Data.xyz
#define uBlockCenter uBlockCenterData.xyz
#define uAlphaMode uAlphaModeData.x
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(location = 2) in vec4 Color;





layout(location = 1) out vec4 vertexColor;
layout(location = 0) out vec2 texCoord0;
layout(location = 2) out vec3 worldPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
    worldPos = Position;
}
