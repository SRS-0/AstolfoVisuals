#version 150
layout(std140) uniform EffectData {
    vec4 uAlphaData;
    vec4 ColorModulatorData;
    vec4 uResolutionData;
    vec4 uCenterData;
    vec4 uTimeData;
    vec4 uProgressData;
    vec4 uThemeColorData;
    vec4 uDistortionData;
    vec4 uShapeData;
};
#define uAlpha uAlphaData.x
#define ColorModulator ColorModulatorData
#define uResolution uResolutionData.xy
#define uCenter uCenterData.xy
#define uTime uTimeData.x
#define uProgress uProgressData.x
#define uThemeColor uThemeColorData.xyz
#define uDistortion uDistortionData.x
#define uShape uShapeData.x
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
