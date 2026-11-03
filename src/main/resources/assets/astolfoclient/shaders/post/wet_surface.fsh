#version 330
#extension GL_ARB_separate_shader_objects : require core

layout(location = 0) in vec2 vUv;
layout(location = 0) out vec4 fragColor;

uniform sampler2D u_SceneTexture;
uniform sampler2D u_DepthTexture;
uniform vec2 u_Resolution;
uniform mat4 u_ViewMatrix;
uniform mat4 u_ProjectionMatrix;
uniform mat4 u_InverseProjectionMatrix;
uniform mat4 u_InverseViewMatrix;
uniform vec3 u_SunDir;
uniform vec3 u_CameraPos;
uniform float u_Time;
uniform float u_Wetness;
uniform float u_PuddleCoverage;
uniform float u_ReflectionStrength;
uniform float u_MaxDistance;
uniform float u_RippleStrength;
uniform float u_RainAmount;
uniform float u_AmbientLight;
uniform float u_LightningFlash;
uniform int u_ReflectionSteps;
uniform int u_Ripples;
uniform vec3 u_ThemeColor;
uniform int u_UseThemeColor;

const int MAX_REFLECTION_STEPS = 16;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float puddleNoise(vec2 p) {
    float value = valueNoise(p) * 0.54;
    value += valueNoise(p * 2.03 + vec2(17.7, 4.3)) * 0.30;
    value += valueNoise(p * 4.11 + vec2(-8.1, 13.2)) * 0.16;
    return value;
}

vec3 reconstructViewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = u_InverseProjectionMatrix * clip;
    float invW = abs(view.w) > 0.000001 ? 1.0 / view.w : 1.0;
    return view.xyz * invW;
}

vec3 reconstructWorldPosition(vec3 viewPosition) {
    return (u_InverseViewMatrix * vec4(viewPosition, 1.0)).xyz;
}

vec3 reconstructViewNormal(vec2 uv, vec3 centerPosition) {
    vec2 texel = 1.0 / max(u_Resolution, vec2(1.0));

    float depthL = texture(u_DepthTexture, safeUv(uv - vec2(texel.x, 0.0))).r;
    float depthR = texture(u_DepthTexture, safeUv(uv + vec2(texel.x, 0.0))).r;
    float depthD = texture(u_DepthTexture, safeUv(uv - vec2(0.0, texel.y))).r;
    float depthU = texture(u_DepthTexture, safeUv(uv + vec2(0.0, texel.y))).r;

    vec3 posL = reconstructViewPosition(uv - vec2(texel.x, 0.0), min(depthL, 0.999999));
    vec3 posR = reconstructViewPosition(uv + vec2(texel.x, 0.0), min(depthR, 0.999999));
    vec3 posD = reconstructViewPosition(uv - vec2(0.0, texel.y), min(depthD, 0.999999));
    vec3 posU = reconstructViewPosition(uv + vec2(0.0, texel.y), min(depthU, 0.999999));

    vec3 rightA = posR - centerPosition;
    vec3 rightB = centerPosition - posL;
    vec3 upA = posU - centerPosition;
    vec3 upB = centerPosition - posD;
    vec3 tangentX = dot(rightA, rightA) < dot(rightB, rightB) ? rightA : rightB;
    vec3 tangentY = dot(upA, upA) < dot(upB, upB) ? upA : upB;

    vec3 normal = cross(tangentX, tangentY);
    float normalLength = length(normal);
    if (normalLength <= 0.000001) {
        return vec3(0.0, 0.0, 1.0);
    }
    normal /= normalLength;
    if (dot(normal, -centerPosition) < 0.0) normal = -normal;
    return normal;
}

float cellRipple(vec2 worldXZ, float scale, float speed, vec2 offset) {
    vec2 grid = worldXZ * scale + offset;
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float seed = hash21(cell + offset * 3.7);
    vec2 center = vec2(
        hash21(cell + vec2(3.1, 7.9)),
        hash21(cell + vec2(11.7, -5.3))
    );
    center = (center - 0.5) * 0.54;

    float phase = fract(u_Time * speed + seed);
    float radius = phase * 0.72;
    float distanceToDrop = length(local - center);
    float ringDistance = distanceToDrop - radius;
    float envelope = exp(-abs(ringDistance) * 28.0) * (1.0 - phase);
    return sin(ringDistance * 64.0) * envelope * smoothstep(0.02, 0.16, phase);
}

float rippleHeight(vec2 worldXZ) {
    float waves = sin(worldXZ.x * 2.85 + u_Time * 1.65)
                 + sin(worldXZ.y * 3.25 - u_Time * 1.38)
                 + sin((worldXZ.x + worldXZ.y) * 1.82 + u_Time * 0.98);
    waves *= 0.055;

    float rings = cellRipple(worldXZ, 0.44, 0.54, vec2(1.7, 8.2));
    rings += cellRipple(worldXZ, 0.71, 0.67, vec2(-4.3, 2.6)) * 0.58;
    rings += cellRipple(worldXZ, 1.15, 0.82, vec2(9.1, -3.7)) * 0.35;
    return waves + rings * 0.36;
}

vec3 rippleNormalView(vec2 worldXZ) {
    float epsilon = 0.032;
    float center = rippleHeight(worldXZ);
    float dx = (rippleHeight(worldXZ + vec2(epsilon, 0.0)) - center) / epsilon;
    float dz = (rippleHeight(worldXZ + vec2(0.0, epsilon)) - center) / epsilon;
    float amount = u_RippleStrength * mix(0.40, 1.15, saturate(u_RainAmount));
    vec3 worldNormal = normalize(vec3(-dx * amount, 1.0, -dz * amount));
    return normalize(mat3(u_ViewMatrix) * worldNormal);
}

vec2 projectPosition(vec3 position, out float valid) {
    vec4 clip = u_ProjectionMatrix * vec4(position, 1.0);
    if (clip.w <= 0.00001) {
        valid = 0.0;
        return vUv;
    }
    vec2 uv = clip.xy / clip.w * 0.5 + 0.5;
    vec2 inside = step(vec2(0.001), uv) * step(uv, vec2(0.999));
    valid = inside.x * inside.y;
    return safeUv(uv);
}

vec2 traceReflection(vec3 origin, vec3 rayDirection, vec3 surfaceNormal, out float hitMask) {
    vec3 rayPosition = origin + surfaceNormal * max(0.035, length(origin) * 0.0015);
    float stepLength = max(0.10, length(origin) * 0.012);
    float traveled = 0.0;
    vec2 lastUv = vUv;
    hitMask = 0.0;

    for (int i = 0; i < MAX_REFLECTION_STEPS; i++) {
        if (i >= u_ReflectionSteps) break;

        rayPosition += rayDirection * stepLength;
        traveled += stepLength;
        if (traveled > min(36.0, u_MaxDistance * 0.85) || rayPosition.z > -0.025) break;

        float projected = 0.0;
        vec2 rayUv = projectPosition(rayPosition, projected);
        if (projected < 0.5) break;
        lastUv = rayUv;

        float sceneDepth = texture(u_DepthTexture, rayUv).r;
        if (sceneDepth < 0.999995) {
            vec3 scenePosition = reconstructViewPosition(rayUv, sceneDepth);
            float depthDelta = scenePosition.z - rayPosition.z;
            float heightAboveSurface = dot(scenePosition - origin, surfaceNormal);
            float thickness = 0.06 + stepLength * 1.45;

            if (i > 1 && heightAboveSurface > 0.045
                    && depthDelta > 0.0 && depthDelta < thickness) {
                float edge = min(min(rayUv.x, rayUv.y), min(1.0 - rayUv.x, 1.0 - rayUv.y));
                hitMask = smoothstep(0.0, 0.06, edge)
                        * (1.0 - smoothstep(0.0, thickness, depthDelta) * 0.30);
                return rayUv;
            }
        }

        stepLength *= 1.22;
    }

    return lastUv;
}

vec3 blurredReflection(vec2 uv, float blurPixels) {
    vec2 texel = 1.0 / max(u_Resolution, vec2(1.0));
    vec2 horizontal = vec2(texel.x * blurPixels, 0.0);
    vec2 vertical = vec2(0.0, texel.y * blurPixels * 1.85);

    vec3 color = texture(u_SceneTexture, safeUv(uv)).rgb * 0.34;
    color += texture(u_SceneTexture, safeUv(uv + horizontal)).rgb * 0.12;
    color += texture(u_SceneTexture, safeUv(uv - horizontal)).rgb * 0.12;
    color += texture(u_SceneTexture, safeUv(uv + vertical)).rgb * 0.16;
    color += texture(u_SceneTexture, safeUv(uv - vertical)).rgb * 0.16;
    color += texture(u_SceneTexture, safeUv(uv + vertical * 2.15)).rgb * 0.05;
    color += texture(u_SceneTexture, safeUv(uv - vertical * 2.15)).rgb * 0.05;
    return color;
}

// World-Space Atmospheric Sky & Environment Radiance Model (RTX Fallback)
vec3 getEnvironmentRadiance(vec3 worldDir, vec3 worldPos, float ambient, float time, vec3 sunDir, vec3 themeColor, int useTheme, float flash) {
    float up = worldDir.y;

    // Day/Night atmosphere palette
    float dayFactor = clamp(sunDir.y * 1.6 + 0.35, 0.0, 1.0);
    vec3 skyZenithDay = vec3(0.20, 0.44, 0.88);
    vec3 skyHorizonDay = vec3(0.68, 0.80, 0.96);
    vec3 skyZenithNight = vec3(0.025, 0.045, 0.095);
    vec3 skyHorizonNight = vec3(0.07, 0.095, 0.16);

    vec3 zenith = mix(skyZenithNight, skyZenithDay, dayFactor);
    vec3 horizon = mix(skyHorizonNight, skyHorizonDay, dayFactor);

    float horizonFactor = exp(-max(0.0, up) * 3.2);
    vec3 sky = mix(zenith, horizon, horizonFactor);

    // Ground bounce reflection below horizon
    vec3 groundColor = mix(vec3(0.04, 0.05, 0.04), vec3(0.16, 0.19, 0.14), dayFactor);
    if (up < 0.0) {
        sky = mix(horizon, groundColor, clamp(-up * 2.2, 0.0, 1.0));
    }

    // Dynamic Cloud Layers reflected on puddles
    if (up > 0.01) {
        vec2 cloudUv = (worldPos.xz + worldDir.xz * ((140.0 - max(worldPos.y, 60.0)) / max(0.06, up))) * 0.006 + vec2(time * 0.010, time * 0.005);
        float cloud = valueNoise(cloudUv * 2.0) * 0.58 + valueNoise(cloudUv * 4.1 + vec2(1.7, 3.4)) * 0.28 + valueNoise(cloudUv * 8.2) * 0.14;
        float cloudMask = smoothstep(0.40, 0.76, cloud) * smoothstep(0.01, 0.22, up);
        vec3 cloudCol = mix(vec3(0.18, 0.20, 0.26), vec3(0.92, 0.94, 0.98), dayFactor);
        sky = mix(sky, cloudCol, cloudMask * 0.78);
    }

    // Sun / Moon / Lightning Specular Glints in puddles
    vec3 normSun = normalize(length(sunDir) > 0.01 ? sunDir : vec3(0.0, 1.0, 0.0));
    float sunDot = max(0.0, dot(worldDir, normSun));
    float sunSpec = pow(sunDot, 128.0) * 3.2 * dayFactor;
    float moonDot = max(0.0, dot(worldDir, -normSun));
    float moonSpec = pow(moonDot, 96.0) * 1.1 * (1.0 - dayFactor);

    sky += vec3(1.0, 0.96, 0.85) * sunSpec + vec3(0.65, 0.80, 1.0) * moonSpec;

    // Lightning Flash Illumination in puddles
    if (flash > 0.001) {
        sky += vec3(0.90, 0.95, 1.15) * flash * (0.85 + max(0.0, up) * 0.45);
    }

    // Theme Color Modulation
    if (useTheme == 1) {
        sky = mix(sky, sky * themeColor * 1.6, 0.48);
    }

    return sky * (0.45 + ambient * 0.75);
}

void main() {
    vec4 original = texture(u_SceneTexture, vUv);
    float depth = texture(u_DepthTexture, vUv).r;
    if (depth >= 0.999995) {
        fragColor = original;
        return;
    }

    vec3 viewPosition = reconstructViewPosition(vUv, depth);
    float distanceToCamera = length(viewPosition);
    if (distanceToCamera > u_MaxDistance || distanceToCamera < 0.25) {
        fragColor = original;
        return;
    }

    vec3 viewNormal = reconstructViewNormal(vUv, viewPosition);
    vec3 worldNormal = normalize(mat3(u_InverseViewMatrix) * viewNormal);

    // Upward floor mask (with smooth tolerance for slopes, steps, and paths)
    float upwardMask = smoothstep(0.42, 0.80, worldNormal.y);
    if (upwardMask <= 0.002) {
        fragColor = original;
        return;
    }

    vec3 worldPosition = reconstructWorldPosition(viewPosition);
    float macroNoise = puddleNoise(worldPosition.xz * 0.065);
    float detailNoise = valueNoise(worldPosition.xz * 0.285 + vec2(19.4, -7.8));
    float puddleField = macroNoise * 0.76 + detailNoise * 0.24;
    float threshold = mix(0.74, 0.24, saturate(u_PuddleCoverage));
    float puddle = smoothstep(threshold - 0.095, threshold + 0.075, puddleField);

    float rangeFade = 1.0 - smoothstep(u_MaxDistance * 0.75, u_MaxDistance, distanceToCamera);
    float nearFade = smoothstep(0.25, 0.75, distanceToCamera);
    float wetFilm = 0.46 + puddle * 0.54;
    float wetMask = saturate(upwardMask * rangeFade * nearFade * wetFilm * u_Wetness);
    if (wetMask <= 0.002) {
        fragColor = original;
        return;
    }

    vec3 reflectionNormal = viewNormal;
    float rippleHighlight = 0.0;
    if (u_Ripples == 1) {
        vec3 rippled = rippleNormalView(worldPosition.xz);
        float rippleMix = saturate((0.20 + puddle * 0.55) * u_RippleStrength);
        reflectionNormal = normalize(mix(viewNormal, rippled, rippleMix));
        rippleHighlight = max(rippleHeight(worldPosition.xz), 0.0);
    }

    vec3 incident = normalize(viewPosition);
    vec3 reflectedDirection = normalize(reflect(incident, reflectionNormal));
    vec3 worldReflectDir = normalize(mat3(u_InverseViewMatrix) * reflectedDirection);

    // 1. Raymarch local Screen Space Reflection (SSR)
    float rayHit = 0.0;
    vec2 rayUv = traceReflection(viewPosition, reflectedDirection, viewNormal, rayHit);

    float roughness = mix(3.2, 1.25, puddle);
    vec3 ssrColor = blurredReflection(rayUv, roughness);

    // Top-screen color bleeding fallback (for nearby structures above screen)
    vec2 topBleedUv = vec2(clamp(rayUv.x, 0.02, 0.98), 0.96);
    vec3 topBleedColor = blurredReflection(topBleedUv, 4.5);

    // 2. Physical World Sky & Atmospheric Environment Fallback (RTX Model)
    vec3 envRadiance = getEnvironmentRadiance(
        worldReflectDir,
        worldPosition,
        u_AmbientLight,
        u_Time,
        u_SunDir,
        u_ThemeColor,
        u_UseThemeColor,
        u_LightningFlash
    );

    // Smooth edge blending: if ray hits screen boundary or leaves viewport, blend seamlessly into sky & atmosphere
    float screenBorder = min(min(rayUv.x, rayUv.y), min(1.0 - rayUv.x, 1.0 - rayUv.y));
    float ssrHitWeight = rayHit * smoothstep(0.0, 0.06, screenBorder);

    // Blend top screen ambient radiance with sky
    vec3 offScreenReflection = mix(envRadiance, topBleedColor, clamp((rayUv.y - 0.85) * 4.0, 0.0, 0.45));

    // Combined Reflection: SSR when hit, World Atmosphere/Sky when off-screen or looking down
    vec3 reflectedColor = mix(offScreenReflection, ssrColor, ssrHitWeight);

    if (u_UseThemeColor == 1) {
        reflectedColor = mix(reflectedColor, reflectedColor * (u_ThemeColor * 1.3), 0.38);
    } else {
        reflectedColor = mix(reflectedColor, reflectedColor * vec3(0.94, 0.98, 1.06), 0.30);
    }

    // 3. Physical Fresnel Reflection Model
    vec3 viewToCamera = normalize(-viewPosition);
    float noV = saturate(dot(reflectionNormal, viewToCamera));
    // Water base reflectance F0 ~ 0.05; grazing angle ~ 1.0
    float fresnel = 0.06 + 0.94 * pow(1.0 - noV, 4.0);

    // 4. Wet Surface Darkening & Water Absorption
    float darkening = saturate(wetMask * (0.22 + puddle * 0.18));
    vec3 wetBase = original.rgb * (1.0 - darkening);
    float wetLuma = luminance(wetBase);
    wetBase = mix(vec3(wetLuma), wetBase, 1.14);
    if (u_UseThemeColor == 1) {
        wetBase = mix(wetBase, wetBase * (u_ThemeColor * 1.12), 0.18);
    } else {
        wetBase *= vec3(0.96, 0.985, 1.035);
    }

    // 5. Final Puddle & Wet Reflection Blending
    // Crucial: looking straight down maintains rich water reflections through base reflectance + Fresnel
    float reflectivePuddle = smoothstep(0.26, 0.70, puddle);
    float reflectionAmount = wetMask
            * reflectivePuddle
            * (0.35 + fresnel * 0.65)
            * u_ReflectionStrength;
    reflectionAmount = min(reflectionAmount, 0.95);

    vec3 color = mix(original.rgb, wetBase, saturate(wetMask * 0.92));
    color = mix(color, reflectedColor * 1.08, reflectionAmount);

    // Specular Highlight Glints on Puddle Surface
    float reflectedLight = smoothstep(0.55, 1.0, luminance(reflectedColor));
    color += reflectedColor * reflectedLight * wetMask * (0.035 + fresnel * 0.12);

    vec3 highlightTint = (u_UseThemeColor == 1) ? (u_ThemeColor * 0.15) : vec3(0.04, 0.06, 0.085);
    color += highlightTint * fresnel * wetMask * (0.32 + puddle * 0.68);

    vec3 rippleTint = (u_UseThemeColor == 1) ? (u_ThemeColor * 0.40) : vec3(0.20, 0.25, 0.32);
    color += rippleTint * rippleHighlight * wetMask * 0.045;

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
