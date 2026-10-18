#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
layout(std140) uniform CustomUniforms {
    float uTime;
    vec3 uColor1;
    vec3 uColor2;
    vec3 uBlockCenter;
    float uGlowIntensity;
    float uPulseSpeed;
    float uPulseWidth;
    float uDistortion;
    float uChromatic;
    float uFadeAlpha;
    float uFillAlpha;
    int uMode;
};

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in vec3 worldPos;


layout(location = 0) out vec4 fragColor;

// Convert HSV to RGB for Rainbow mode
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    float t = mod(uTime, 628.31853);
    vec3 localPos = worldPos - uBlockCenter;
    float dist3D = length(localPos);

    // Calculate face edge proximity from UVs (0.0 at edge, 0.5 at face center)
    vec2 faceUV = texCoord0;
    vec2 edgeDist = min(faceUV, vec2(1.0) - faceUV);
    float minEdge = min(edgeDist.x, edgeDist.y);
    float edgeFactor = smoothstep(0.12, 0.001, minEdge); // 1.0 at outer edge, 0.0 inside

    vec3 finalGlowColor = uColor1;
    float finalAlpha = uFillAlpha;

    // Distortion ripple factor
    float ripple = 0.0;
    if (uDistortion > 0.5) {
        ripple = sin(dist3D * 12.0 - t * uPulseSpeed * 4.0) * 0.08;
    }

    if (uMode == 0) {
        // === MODE 0: PULSE WAVE (HitGlow style expanding 3D shockwave & inner aura) ===
        float waveCycle = mod(t * uPulseSpeed, 1.8);
        float ringDist = abs(dist3D - waveCycle + ripple);
        float ringWidth = max(0.04, uPulseWidth * 0.22);
        float ringGlow = smoothstep(ringWidth, 0.0, ringDist);
        float innerFill = smoothstep(waveCycle, 0.0, dist3D) * 0.35 * (1.0 - (waveCycle / 1.8));

        float waveStrength = (ringGlow * 2.2 + innerFill + edgeFactor * 0.6) * uGlowIntensity;

        if (uChromatic > 0.5) {
            float chromaR = smoothstep(ringWidth, 0.0, abs(dist3D - waveCycle + 0.04));
            float chromaB = smoothstep(ringWidth, 0.0, abs(dist3D - waveCycle - 0.04));
            vec3 colR = uColor1 * chromaR * 1.5;
            vec3 colG = uColor1 * ringGlow;
            vec3 colB = uColor2 * chromaB * 1.5;
            finalGlowColor = colR + colG + colB + (uColor1 * innerFill);
        } else {
            finalGlowColor = mix(uColor1, uColor2, clamp(ringGlow * 0.7, 0.0, 1.0)) * waveStrength;
        }

        finalAlpha = clamp((ringGlow * 0.75 + innerFill * 0.5 + edgeFactor * 0.4 + uFillAlpha * 0.4) * uGlowIntensity, 0.05, 0.95);

    } else if (uMode == 1) {
        // === MODE 1: COSMOS FRACTAL FLOW ===
        vec2 uv = vec2(localPos.x + localPos.y + ripple, localPos.z + localPos.y) * 2.5;
        for (float i = 1.0; i < 9.0; i++) {
            uv.x += 0.5 / i * cos(i * 2.2 * uv.y + t * uPulseSpeed);
            uv.y += 0.5 / i * cos(i * 1.6 * uv.x + t * uPulseSpeed);
        }
        float cosmosIntensity = 0.12 / max(0.01, abs(sin(t * uPulseSpeed - uv.y - uv.x)));
        vec3 cosmosColor = mix(uColor1, uColor2, clamp(sin(uv.x + uv.y) * 0.5 + 0.5, 0.0, 1.0));
        finalGlowColor = (cosmosColor * cosmosIntensity * uGlowIntensity) + (uColor1 * edgeFactor * 1.2);
        finalAlpha = clamp((cosmosIntensity * 0.5 + edgeFactor * 0.4 + uFillAlpha) * 0.8, 0.05, 0.95);

    } else if (uMode == 2) {
        // === MODE 2: NEON GLOW & RIM HIGHLIGHT ===
        float neonEdge = pow(edgeFactor, 0.8) * 1.8;
        float centerSoftGlow = (1.0 - smoothstep(0.0, 0.9, dist3D)) * 0.4;
        float pulseAnim = sin(t * uPulseSpeed * 3.0) * 0.15 + 0.85;

        finalGlowColor = uColor1 * (neonEdge + centerSoftGlow + 0.2) * uGlowIntensity * pulseAnim;
        finalAlpha = clamp((neonEdge * 0.7 + centerSoftGlow * 0.3 + uFillAlpha) * pulseAnim, 0.05, 0.95);

    } else if (uMode == 3) {
        // === MODE 3: RAINBOW CHROMATIC WAVE ===
        float hue = fract((localPos.x + localPos.y + localPos.z) * 0.35 + t * uPulseSpeed * 0.3 + ripple);
        vec3 rainbowCol = hsv2rgb(vec3(hue, 0.85, 1.0));
        float wavePulse = sin(dist3D * 6.0 - t * uPulseSpeed * 4.0) * 0.5 + 0.5;

        finalGlowColor = rainbowCol * (1.0 + wavePulse * 0.6 + edgeFactor * 1.0) * uGlowIntensity;
        finalAlpha = clamp((wavePulse * 0.4 + edgeFactor * 0.5 + uFillAlpha), 0.05, 0.95);

    } else if (uMode == 4) {
        // === MODE 4: CYBER GRID / SCANLINE ===
        float scanline = sin((worldPos.y + worldPos.x + ripple) * 24.0 - t * uPulseSpeed * 6.0);
        float gridY = step(0.85, fract(faceUV.y * 8.0));
        float gridX = step(0.85, fract(faceUV.x * 8.0));
        float grid = max(gridX, gridY);

        float cyberGlow = (grid * 1.2 + max(0.0, scanline) * 0.8 + edgeFactor * 1.5) * uGlowIntensity;
        finalGlowColor = mix(uColor1, vec3(1.0), grid * 0.4) * cyberGlow;
        finalAlpha = clamp((grid * 0.5 + scanline * 0.3 + edgeFactor * 0.6 + uFillAlpha * 0.4), 0.05, 0.95);

    } else {
        // === MODE 5: CLEAN GLOW FILL ===
        finalGlowColor = uColor1 * (1.0 + edgeFactor * 0.8) * uGlowIntensity;
        finalAlpha = clamp(uFillAlpha + edgeFactor * 0.4, 0.05, 0.95);
    }

    // Apply master fade alpha (for smooth appearing / disappearing animations) and vertex color alpha
    float masterAlpha = clamp(finalAlpha * uFadeAlpha * vertexColor.a, 0.0, 1.0);

    fragColor = vec4(finalGlowColor, masterAlpha);
}
