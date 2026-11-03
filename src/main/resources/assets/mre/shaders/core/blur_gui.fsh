#version 150

flat in vec4 Effect0Value;
#define SizeData Effect0Value
flat in vec4 Effect1Value;
#define RadiusData Effect1Value
flat in vec4 Effect2Value;
#define SmoothnessData Effect2Value
flat in vec4 Effect3Value;
#define BlurRadiusData Effect3Value
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x
#define BlurRadius BlurRadiusData.x


#moj_import <mre:common.glsl>

in vec2 FragCoord; // normalized fragment coord relative to the primitive
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;





out vec4 OutColor;

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
