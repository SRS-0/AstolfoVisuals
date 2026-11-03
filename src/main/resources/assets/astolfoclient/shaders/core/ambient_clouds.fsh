#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float time;
    vec3 themeColor;
    float alpha;
};

in vec3 lookDir;
out vec4 fragColor;




float hash3(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    return mix(
        mix(mix(hash3(i + vec3(0.0,0.0,0.0)), hash3(i + vec3(1.0,0.0,0.0)), u.x),
            mix(hash3(i + vec3(0.0,1.0,0.0)), hash3(i + vec3(1.0,1.0,0.0)), u.x), u.y),
        mix(mix(hash3(i + vec3(0.0,0.0,1.0)), hash3(i + vec3(1.0,0.0,1.0)), u.x),
            mix(hash3(i + vec3(0.0,1.0,1.0)), hash3(i + vec3(1.0,1.0,1.0)), u.x), u.y), u.z);
}

float fbm3(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    vec3 shift = vec3(100.0);
    mat3 rot = mat3(0.00,  0.80,  0.60,
                    -0.80,  0.36, -0.48,
                    -0.60, -0.48,  0.64);
    for (int i = 0; i < 5; i++) {
        value += amplitude * noise3(p);
        p = rot * p * 2.2 + shift;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec3 dir = normalize(lookDir);

    vec3 skyTop    = themeColor * 0.15;
    vec3 skyBottom = themeColor * 0.02;
    vec3 skyColor  = mix(skyBottom, skyTop, smoothstep(-1.0, 1.0, dir.y));

    vec3 wind = vec3(time * 0.015, time * 0.008, time * 0.01);
    vec3 q = vec3(fbm3(dir * 3.6 + wind), fbm3(dir * 2.4 - wind), fbm3(dir * 3.0 + wind * 0.5));
    vec3 r = vec3(fbm3(dir * 4.5 + q + vec3(1.2, 4.3, 2.8) + wind * 0.5), 
                  fbm3(dir * 3.9 + q + vec3(7.4, 2.9, 1.5) + wind * 0.3),
                  fbm3(dir * 3.3 + q + vec3(3.1, 8.6, 5.2) + wind * 0.4));
    float cloudDensity = fbm3(dir * 5.4 + r);

    vec3 crimsonGlow  = themeColor;
    vec3 magmaHotSpot = clamp(themeColor * 1.3 + vec3(0.1, 0.1, 0.0), 0.0, 1.0);
    vec3 darkSmoke    = themeColor * 0.12;

    float horizonBand = exp(-abs(dir.y - 0.1) * 1.5);
    vec3 cloudColor = mix(darkSmoke, crimsonGlow, smoothstep(0.15, 0.6, cloudDensity));
    cloudColor += magmaHotSpot * pow(max(0.0, cloudDensity), 3.0) * 1.2;
    cloudColor += crimsonGlow * horizonBand * 0.5;

    float cloudAlpha = smoothstep(0.2, 0.65, cloudDensity);
    vec3 finalSky = mix(skyColor, cloudColor, cloudAlpha * 0.9);
    finalSky += crimsonGlow * (pow(max(0.0, cloudDensity), 2.0) * 0.25 + horizonBand * 0.3);

    float horizonMist = exp(-abs(dir.y + 0.4) * 2.5);
    finalSky = mix(finalSky, themeColor * 0.4, horizonMist * 0.4);

    vec3 starGrid = dir * 60.0;
    vec3 gridId = floor(starGrid);
    vec3 gridFract = fract(starGrid) - 0.5;
    float starRand = hash3(gridId);
    float starDot = smoothstep(0.25, 0.0, length(gridFract)) * step(0.992, starRand);
    starDot *= sin(time * 2.5 + starRand * 6.28) * 0.5 + 0.5;
    starDot *= smoothstep(-0.1, 0.6, dir.y);
    starDot *= (1.0 - cloudAlpha * 0.85);
    finalSky += vec3(1.0, 0.9, 0.9) * starDot * 1.5;

    finalSky = 1.0 - exp(-finalSky * 1.5);
    fragColor = vec4(finalSky, alpha);
}