#version 150
in vec2 EffectLocal;
in vec4 Effect0;
flat out vec4 Effect0Value;
in vec4 Effect1;
flat out vec4 Effect1Value;
in vec4 Effect2;
flat out vec4 Effect2Value;
in vec4 Effect3;
flat out vec4 Effect3Value;

#moj_import <mre:common.glsl>

in vec3 Position; // POSITION_COLOR vertex attributes
in vec4 Color;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;

void main() {
    Effect0Value = Effect0;
    Effect1Value = Effect1;
    Effect2Value = Effect2;
    Effect3Value = Effect3;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    FragCoord = EffectLocal;
    TexCoord = gl_Position.xy * 0.5 + 0.5;
    FragColor = Color;
}