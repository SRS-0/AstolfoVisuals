#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
    vec4 uTimeData;
    vec4 uColor1Data;
    vec4 uColor2Data;
    vec4 uBlockCenterData;
    vec4 uGlowIntensityData;
    vec4 uPulseSpeedData;
    vec4 uPulseWidthData;
    vec4 uDistortionData;
    vec4 uChromaticData;
    vec4 uFadeAlphaData;
    vec4 uFillAlphaData;
    ivec4 uModeData;
};
#define ColorModulator ColorModulatorData
#define uTime uTimeData.x
#define uColor1 uColor1Data.xyz
#define uColor2 uColor2Data.xyz
#define uBlockCenter uBlockCenterData.xyz
#define uGlowIntensity uGlowIntensityData.x
#define uPulseSpeed uPulseSpeedData.x
#define uPulseWidth uPulseWidthData.x
#define uDistortion uDistortionData.x
#define uChromatic uChromaticData.x
#define uFadeAlpha uFadeAlphaData.x
#define uFillAlpha uFillAlphaData.x
#define uMode uModeData.x
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
