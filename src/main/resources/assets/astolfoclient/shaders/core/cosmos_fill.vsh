#version 150
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
