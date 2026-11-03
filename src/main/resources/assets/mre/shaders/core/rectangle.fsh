#version 150

layout(std140) uniform EffectData {
    vec4 SizeData;
    vec4 RadiusData;
    vec4 SmoothnessData;
};
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x


#moj_import <mre:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec4 FragColor;





out vec4 OutColor;

void main() {
    float alpha = ralpha(Size, FragCoord, Radius, Smoothness);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}