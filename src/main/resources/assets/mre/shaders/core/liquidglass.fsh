#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform EffectData {
    vec4 SizeData;
    vec4 RadiusData;
    vec4 SmoothnessData;
    vec4 CornerSmoothnessData;
    vec4 GlobalAlphaData;
    vec4 FresnelPowerData;
    vec4 FresnelColorData;
    vec4 FresnelAlphaData;
    vec4 BaseAlphaData;
    vec4 FresnelInvertData;
    vec4 FresnelMixData;
    vec4 DistortStrengthData;
};
#define Size SizeData.xy
#define Radius RadiusData
#define Smoothness SmoothnessData.x
#define CornerSmoothness CornerSmoothnessData.x
#define GlobalAlpha GlobalAlphaData.x
#define FresnelPower FresnelPowerData.x
#define FresnelColor FresnelColorData.xyz
#define FresnelAlpha FresnelAlphaData.x
#define BaseAlpha BaseAlphaData.x
#define FresnelInvert (FresnelInvertData.x != 0.0)
#define FresnelMix FresnelMixData.x
#define DistortStrength DistortStrengthData.x


#include <mre:common.glsl>

layout(location = 1) in vec2 FragCoord;
layout(location = 2) in vec2 TexCoord;
layout(location = 0) in vec4 FragColor;

uniform sampler2D Sampler0;














layout(location = 0) out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0/smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

void main() {
    vec2 center = Size * 0.5;
    vec2 box_half_size = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    float distance = roundedBoxSDF(-pos, box_half_size, Radius, CornerSmoothness);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    float distToEdge = abs(roundedBoxSDF(pos, box_half_size, Radius, CornerSmoothness));

    float max_dist_norm = min(box_half_size.x, box_half_size.y);
    float edge_gradient = 1.0 - clamp(distToEdge / max_dist_norm, 0.0, 1.0);

    float fresnel;
    float base = FresnelInvert ? edge_gradient : (1.0 - edge_gradient);

    if (FresnelPower > 20.0) {
        fresnel = exp(FresnelPower * log(clamp(base, 0.001, 1.0)));
    } else {
        fresnel = pow(base, FresnelPower);
    }
    fresnel = clamp(fresnel, 0.0, 1.0);

    vec2 dir = normalize(pos);
    vec2 distortedTexCoord = TexCoord + dir * fresnel * DistortStrength;

    vec4 texColor = texture(Sampler0, distortedTexCoord) * FragColor;

    vec3 finalColor = mix(texColor.rgb, FresnelColor, fresnel * FresnelMix);
    float finalAlpha = mix(BaseAlpha, FresnelAlpha, fresnel) * alpha;

    if (finalAlpha < 0.001) {
        discard;
    }

    OutColor = vec4(finalColor, finalAlpha * GlobalAlpha);
}
