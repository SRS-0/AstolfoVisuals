#version 150
layout(std140) uniform EffectData {
    mat4 uInvViewProjMat;
    mat4 uViewProjMat;
    vec2 uResolution;
    float uDistortionStrength;
    int uChromatic;
    int uCircleCount;
    vec3 uCirclePos[16];
    float uCircleRadius[16];
    float uCircleProgress[16];
    float uCircleAlpha[16];
    float uDepthZeroToOne;
};

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uColorTexture;
uniform sampler2D uDepthTexture;






const int MAX_CIRCLES = 16;






void main() {
    vec4 baseColor = texture(uColorTexture, vTexCoord);
    if (uCircleCount <= 0 || uDistortionStrength <= 0.001) {
        fragColor = baseColor;
        return;
    }

    float rawDepth = texture(uDepthTexture, vTexCoord).r;
    if (rawDepth <= 0.000001) {
        // Don't distort sky: Jump circle is a 2D planar floor effect
        fragColor = baseColor;
        return;
    }

    // Reconstruct 3D camera-relative world position
    vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, mix(rawDepth * 2.0 - 1.0, rawDepth, uDepthZeroToOne), 1.0);
    vec4 camRelPosH = uInvViewProjMat * clipPos;
    vec3 camRelPos = camRelPosH.xyz / max(0.0001, camRelPosH.w);

    vec2 totalUVShift = vec2(0.0);
    float totalChromatic = 0.0;

    for (int i = 0; i < uCircleCount; i++) {
        if (i >= MAX_CIRCLES) break;

        vec3 cPos = uCirclePos[i];
        float radius = uCircleRadius[i];
        float progress = uCircleProgress[i];
        float alpha = uCircleAlpha[i];

        if (alpha <= 0.001 || radius <= 0.01) continue;

        // 3D vector from jump circle epicenter to the block surface
        vec3 delta = camRelPos - cPos;
        float dist3D = length(delta);

        // Strictly bounded by jump circle radius in world space
        float maxEffectRadius = radius * 1.05;
        if (dist3D > maxEffectRadius) continue;

        // 1. Expanding Shockwave Wavefront along block surfaces in 3D
        float waveWidth = max(0.12, radius * 0.28);
        float ringDist = abs(dist3D - radius);
        float ringFactor = smoothstep(waveWidth, 0.0, ringDist);
        float ripple = sin((dist3D - radius) / waveWidth * 3.14159265) * ringFactor;

        // 2. Black Hole Gravitational Singularity Pull towards epicenter
        float blackholePull = smoothstep(radius, 0.0, dist3D) * (1.0 - progress);

        // 3. Radial normal and tangential swirl in 3D space
        vec3 normalDir = delta / max(0.001, dist3D);
        vec3 tangent = normalize(cross(vec3(0.0, 1.0, 0.0), delta + vec3(0.0001)));
        float swirl = blackholePull * 0.40 * sin(progress * 3.14159265);

        // Combined 3D world displacement across walls, floors, and holes
        vec3 worldDisplace = (normalDir * (ripple * 0.38 - blackholePull * 0.28) + tangent * swirl)
                             * alpha * uDistortionStrength;

        // Apply 3D shift to block surface in world space
        vec3 displacedWorldPos = camRelPos + worldDisplace;

        // Project displaced 3D point to screen UV coordinates
        vec4 distClip = uViewProjMat * vec4(displacedWorldPos, 1.0);
        if (distClip.w > 0.01) {
            vec2 distUV = (distClip.xy / distClip.w) * 0.5 + 0.5;
            totalUVShift += (distUV - vTexCoord);
        }

        if (uChromatic == 1) {
            totalChromatic += (abs(ripple) * 0.010 + blackholePull * 0.007) * alpha * uDistortionStrength;
        }
    }

    if (length(totalUVShift) < 0.00005 && totalChromatic < 0.00005) {
        fragColor = baseColor;
        return;
    }

    // Pure world refraction with optical chromatic aberration
    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);
    vec2 uvG = vTexCoord + totalUVShift;
    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);

    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;
    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;
    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;

    fragColor = vec4(r, g, b, baseColor.a);
}