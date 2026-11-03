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
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;




out vec4 vertexColor;
out vec2 texCoord0;
out vec3 vNormal;
out vec3 vPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = UV0;

    // Extract View Position and Normal for 3D Fresnel math
    vPos = (ModelViewMat * vec4(Position, 1.0)).xyz;
    vNormal = normalize(mat3(ModelViewMat) * Normal);
}