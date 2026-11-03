#version 150
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
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;





out vec4 vertexColor;
out vec2 texCoord0;
out vec3 worldPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
    worldPos = Position;
}
