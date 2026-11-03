#version 150

flat in vec4 Effect0Value;
#define SizeData Effect0Value
flat in vec4 Effect1Value;
#define RadiusData Effect1Value
flat in vec4 Effect2Value;
#define SmoothnessData Effect2Value
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x


#moj_import <mre:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;




out vec4 OutColor;

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(1.0, 1.0, 1.0, alpha) * texture(Sampler0, TexCoord) * FragColor;

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}