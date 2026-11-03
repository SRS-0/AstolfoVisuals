#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform EffectData {
    vec4 SizeData;
    vec4 RadiusData;
    vec4 SmoothnessData;
    vec4 BlurRadiusData;
};
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x
#define BlurRadius BlurRadiusData.x


#include <mre:common.glsl>

layout(location = 1) in vec2 FragCoord; // normalized fragment coord relative to the primitive
layout(location = 2) in vec2 TexCoord;
layout(location = 0) in vec4 FragColor;

uniform sampler2D Sampler0;





layout(location = 0) out vec4 OutColor;

const float DPI = 6.28318530718;
const float STEP = DPI / 16.0;

void main() {
    vec2 multiplier = BlurRadius / textureSize(Sampler0, 0);
    
    vec3 average = texture(Sampler0, TexCoord).rgb;
    for (int direction = 0; direction < 16; ++direction) {
        float d = float(direction) * STEP;
        for (int sampleIndex = 1; sampleIndex <= 5; ++sampleIndex) {
            float i = float(sampleIndex) / 5.0;
            average += texture(Sampler0, TexCoord + vec2(cos(d), sin(d)) * multiplier * i).rgb;
        }
    }
    average /= 81.0;

    vec4 color = vec4(average, 1.0) * FragColor;
    color.a *= ralpha(Size, FragCoord, Radius, Smoothness);

    if (color.a == 0.0) { // alpha test
        discard;
    }

    OutColor = color;
}
