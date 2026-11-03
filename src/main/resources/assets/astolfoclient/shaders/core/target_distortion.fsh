#version 150
layout(std140) uniform EffectData {
    mat4 uViewProjMat;
    vec2 uResolution;
    float uDistortionStrength;
    int uChromatic;
    int uNodeCount;
    vec3 uNodePos[36];
    float uNodeRadius[36];
    float uNodeAlpha[36];
};

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uColorTexture;





const int MAX_NODES = 36;





void main() {
    vec4 baseColor = texture(uColorTexture, vTexCoord);
    if (uNodeCount <= 0 || uDistortionStrength <= 0.001) {
        fragColor = baseColor;
        return;
    }

    vec2 totalUVShift = vec2(0.0);
    float totalChromatic = 0.0;
    float aspect = uResolution.x / uResolution.y;

    for (int i = 0; i < uNodeCount; i++) {
        if (i >= MAX_NODES) break;
        vec3 nPos = uNodePos[i];
        float nRad = uNodeRadius[i];
        float alpha = uNodeAlpha[i];
        if (alpha <= 0.001 || nRad <= 0.01) continue;

        // Project 3D node center to Screen Space
        vec4 clip = uViewProjMat * vec4(nPos, 1.0);
        if (clip.w <= 0.1) continue; // Behind camera

        vec2 screenCenter = (clip.xy / clip.w) * 0.5 + 0.5;
        vec2 diff = (vTexCoord - screenCenter) * vec2(aspect, 1.0);
        float screenDist = length(diff);
        float screenRadius = clamp(nRad / clip.w, 0.02, 0.40);

        if (screenDist < screenRadius) {
            float normDist = screenDist / screenRadius;
            float lens = sin(normDist * 3.14159265);
            float pull = (1.0 - normDist) * 0.035;
            vec2 dir = diff / max(0.0001, screenDist);

            vec2 shift = dir * (lens * 0.022 - pull) * alpha * uDistortionStrength;
            totalUVShift += vec2(shift.x / aspect, shift.y);

            if (uChromatic == 1) {
                totalChromatic += (lens * 0.008 + pull * 0.004) * alpha * uDistortionStrength;
            }
        }
    }

    if (length(totalUVShift) < 0.00005 && totalChromatic < 0.00005) {
        fragColor = baseColor;
        return;
    }

    // Sample with Chromatic Aberration
    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);
    vec2 uvG = vTexCoord + totalUVShift;
    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);

    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;
    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;
    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;

    fragColor = vec4(r, g, b, baseColor.a);
}