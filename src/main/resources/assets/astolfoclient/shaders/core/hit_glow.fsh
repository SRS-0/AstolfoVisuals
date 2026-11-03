#version 150
layout(std140) uniform EffectData {
    mat4 uInvViewProjMat;
    mat4 uViewProjMat;
    vec2 uResolution;
    float uGlowIntensity;
    float uRingWidth;
    int uDistortion;
    float uDistortionStrength;
    int uChromatic;
    vec3 uThemeColor;
    vec3 uCameraPos;
    int uBlockCorners;
    float uCornerGlow;
    float uCornerSize;
    int uEdgeLines;
    float uEdgeWidth;
    int uWaveCount;
    vec3 uWavePos[16];
    float uWaveRadius[16];
    float uWaveAlpha[16];
    float uWaveProgress[16];
};

in vec2 vTexCoord;
out vec4 fragColor;

uniform sampler2D uColorTexture;
uniform sampler2D uDepthTexture;

















const int MAX_WAVES = 16;






void main() {
    vec4 baseColor = texture(uColorTexture, vTexCoord);
    if (uWaveCount <= 0 || (uGlowIntensity <= 0.001 && uCornerGlow <= 0.001)) {
        fragColor = baseColor;
        return;
    }

    float rawDepth = texture(uDepthTexture, vTexCoord).r;
    if (rawDepth >= 0.999999) {
        fragColor = baseColor;
        return;
    }

    // Reconstruct 3D camera-relative world position
    vec4 clipPos = vec4(vTexCoord * 2.0 - 1.0, rawDepth * 2.0 - 1.0, 1.0);
    vec4 camRelPosH = uInvViewProjMat * clipPos;
    vec3 camRelPos = camRelPosH.xyz / max(0.0001, camRelPosH.w);
    vec3 worldPos = camRelPos + uCameraPos;

    // Calculate distance to 3D block face boundaries
    vec3 bFract = fract(worldPos);
    vec3 d = min(bFract, vec3(1.0) - bFract);

    // Identify 2D face coordinates on the nearest block face
    vec2 faceCoords;
    if (d.x <= d.y && d.x <= d.z) {
        faceCoords = vec2(d.y, d.z);
    } else if (d.y <= d.x && d.y <= d.z) {
        faceCoords = vec2(d.x, d.z);
    } else {
        faceCoords = vec2(d.x, d.y);
    }

    // 2D distance to the nearest corner on this block face
    float cornerDist = length(faceCoords);
    float cornerFactor = smoothstep(max(0.01, uCornerSize), 0.0, cornerDist);
    float cornerPattern = pow(cornerFactor, 1.3) * 3.0;

    // 2D distance to the nearest edge on this block face
    float edgeDist = min(faceCoords.x, faceCoords.y);
    float edgeFactor = smoothstep(max(0.005, uEdgeWidth), 0.0, edgeDist);

    float blockPattern = (uEdgeLines == 1) ? (cornerPattern + edgeFactor * 1.4) : cornerPattern;

    vec2 totalUVShift = vec2(0.0);
    float totalChromatic = 0.0;
    float totalGlow = 0.0;
    float totalBlockGlow = 0.0;

    for (int i = 0; i < uWaveCount; i++) {
        if (i >= MAX_WAVES) break;

        vec3 wPos = uWavePos[i];
        float radius = uWaveRadius[i];
        float alpha = uWaveAlpha[i];
        float progress = uWaveProgress[i];

        if (alpha <= 0.001 || radius <= 0.1) continue;

        vec3 delta = camRelPos - wPos;
        float dist3D = length(delta);

        float maxEffectRadius = radius + uRingWidth * 2.0;
        if (dist3D > maxEffectRadius) continue;

        // 1. Smooth 3D Expanding Energy Ring across all block surfaces
        float ringDist = abs(dist3D - radius);
        float ringFactor = smoothstep(uRingWidth, 0.0, ringDist);

        // 2. Inner smooth glowing aura
        float innerFill = smoothstep(radius, 0.0, dist3D) * 0.20 * (1.0 - progress);

        // 3. Block Corner Glow strictly synchronized with the expanding wave ring
        if (uBlockCorners == 1) {
            // Peaks at the wave crest and smoothly fades behind the wave with alpha
            float cornerWaveSync = ringFactor * alpha * (1.0 - progress * 0.3);
            totalBlockGlow += blockPattern * cornerWaveSync * uCornerGlow;
        }

        float waveGlow = (ringFactor * 1.6 + innerFill) * alpha * uGlowIntensity;
        totalGlow += waveGlow;

        // 4. Subtle physical refractive distortion wave on the ring crest
        if (uDistortion == 1) {
            float ripple = sin((dist3D - radius) / max(0.1, uRingWidth) * 3.14159265) * ringFactor;
            vec3 normalDir = delta / max(0.001, dist3D);
            vec3 worldDisplace = normalDir * (ripple * 0.22 * uDistortionStrength) * alpha;

            vec3 displacedWorldPos = camRelPos + worldDisplace;
            vec4 distClip = uViewProjMat * vec4(displacedWorldPos, 1.0);
            if (distClip.w > 0.01) {
                vec2 distUV = (distClip.xy / distClip.w) * 0.5 + 0.5;
                totalUVShift += (distUV - vTexCoord);
            }

            if (uChromatic == 1) {
                totalChromatic += abs(ripple) * 0.008 * uDistortionStrength * alpha;
            }
        }
    }

    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);
    vec2 uvG = vTexCoord + totalUVShift;
    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);

    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;
    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;
    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;

    vec3 sceneColor = vec3(r, g, b);

    // Apply smooth neon theme glow directly onto block textures and corners
    float combinedGlow = totalGlow + totalBlockGlow;
    if (combinedGlow > 0.005) {
        vec3 glowColor = uThemeColor * combinedGlow;
        sceneColor = sceneColor + glowColor + (sceneColor * glowColor * 0.45);
    }

    fragColor = vec4(sceneColor, baseColor.a);
}