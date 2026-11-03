#version 150
layout(std140) uniform EffectData {
    mat4 uInvViewProjMat;
    vec3 uSphereCenterRelCam;
    float uSphereRadius;
    float uSmoothness;
    float uBlurStrength;
    vec2 uResolution;
    int uSampleCount;
    int uBlurSky;
    vec3 uThemeColor;
    float uThemeTint;
    float uDepthZeroToOne;
};

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uColorTexture;
uniform sampler2D uDepthTexture;











const float GOLDEN_ANGLE = 2.39996323;

void main() {
    vec4 centerColor = texture(uColorTexture, vTexCoord);
    float rawDepth = texture(uDepthTexture, vTexCoord).r;

    float dist = 0.0;
    float weight = 0.0;

    if (rawDepth <= 0.000001) {
        // Far plane / Sky
        if (uBlurSky == 0) {
            fragColor = centerColor;
            return;
        }
        weight = 1.0;
    } else {
        // Reconstruct camera-relative world position from depth buffer
        vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, mix(rawDepth * 2.0 - 1.0, rawDepth, uDepthZeroToOne), 1.0);
        vec4 camRelPosH = uInvViewProjMat * clipPos;
        vec3 camRelPos = camRelPosH.xyz / camRelPosH.w;

        // Distance from 3D sphere center
        dist = length(camRelPos - uSphereCenterRelCam);

        float innerRadius = max(0.0, uSphereRadius - uSmoothness);
        float factor = clamp((dist - innerRadius) / max(0.001, uSmoothness), 0.0, 1.0);
        weight = smoothstep(0.0, 1.0, factor);
    }

    if (weight <= 0.001 || uBlurStrength <= 0.1) {
        fragColor = centerColor;
        return;
    }

    // Vogel disk golden-angle spiral sampling with Gaussian falloff
    vec2 texel = 1.0 / uResolution;
    float effectiveRadius = uBlurStrength * weight;
    vec4 accum = centerColor;
    float totalWeight = 1.0;

    int samples = clamp(uSampleCount, 8, 64);
    for (int i = 1; i <= samples; i++) {
        float r = sqrt(float(i) / float(samples));
        float theta = float(i) * GOLDEN_ANGLE;
        vec2 offset = vec2(cos(theta), sin(theta)) * (r * effectiveRadius) * texel;
        float sampleW = exp(-2.0 * r * r);
        accum += texture(uColorTexture, vTexCoord + offset) * sampleW;
        totalWeight += sampleW;
    }

    vec4 blurred = accum / totalWeight;

    if (uThemeTint > 0.001) {
        blurred.rgb = mix(blurred.rgb, uThemeColor, uThemeTint * weight);
    }

    fragColor = mix(centerColor, blurred, weight);
}