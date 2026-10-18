#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 centerPixel = texture(Sampler0, texCoord0);
    vec2 texelSize = 1.0 / textureSize(Sampler0, 0);

    float alphaAccumulator = 0.0;
    float samples = 0.0;

    const float PI = 3.14159265359;
    const float DIRS = 16.0;
    const float Radius = 5.0;


    for (float d = 0.0; d < PI * 2.0; d += (PI * 2.0) / DIRS) {
        for (float i = 1.0; i <= Radius; i++) {
            vec2 offset = vec2(cos(d), sin(d)) * texelSize * i;
            float neighborAlpha = texture(Sampler0, texCoord0 + offset).a;


            alphaAccumulator += step(0.1, neighborAlpha);
            samples += 1.0;
        }
    }


    float density = alphaAccumulator / samples;

    if (centerPixel.a > 0.1) {

        float innerFade = smoothstep(0.4, 1.0, density);

        innerFade = pow(innerFade, 1.5);

        vec3 finalColor = mix(ColorModulator.rgb, vec3(1.0), innerFade);

        float finalAlpha = mix(0.4, 0.8, innerFade) * ColorModulator.a;

        fragColor = vec4(finalColor, finalAlpha);

    } else if (density > 0.0) {

        float glowAlpha = smoothstep(0.0, 0.6, density);

        fragColor = vec4(ColorModulator.rgb, glowAlpha * ColorModulator.a);

    } else {
        discard;
    }
}