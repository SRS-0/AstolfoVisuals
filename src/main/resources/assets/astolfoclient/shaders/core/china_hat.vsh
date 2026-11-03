#version 150
layout(std140) uniform EffectData {
    vec4 ColorModulatorData;
    vec4 uResolutionData;
    vec4 uTimeData;
    vec4 uThemeColorData;
};
#define ColorModulator ColorModulatorData
#define uResolution uResolutionData.xy
#define uTime uTimeData.x
#define uThemeColor uThemeColorData.xyz
layout(std140) uniform DynamicTransforms { mat4 ModelViewMat; };
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;





out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * ColorModulator;
    texCoord0 = UV0;
}
