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
in vec4 Effect4;
flat out vec4 Effect4Value;
in vec4 Effect5;
flat out vec4 Effect5Value;
in vec4 Effect6;
flat out vec4 Effect6Value;
in vec4 Effect7;
flat out vec4 Effect7Value;
in vec4 Effect8;
flat out vec4 Effect8Value;
in vec4 Effect9;
flat out vec4 Effect9Value;
in vec4 Effect10;
flat out vec4 Effect10Value;
in vec4 Effect11;
flat out vec4 Effect11Value;

#moj_import <mre:common.glsl>

in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
in vec2 UV0;
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
    Effect4Value = Effect4;
    Effect5Value = Effect5;
    Effect6Value = Effect6;
    Effect7Value = Effect7;
    Effect8Value = Effect8;
    Effect9Value = Effect9;
    Effect10Value = Effect10;
    Effect11Value = Effect11;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    FragCoord = EffectLocal;
    TexCoord = UV0;
    FragColor = Color;
} 