#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float time;
    vec3 themeColor;
    float alpha;
    float speed;
    float density;
    float horizon;
};

in vec3 lookDir;
out vec4 fragColor;







float hash3(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash3(i + vec3(0.0,0.0,0.0)), hash3(i + vec3(1.0,0.0,0.0)), u.x),
            mix(hash3(i + vec3(0.0,1.0,0.0)), hash3(i + vec3(1.0,1.0,0.0)), u.x), u.y),
        mix(mix(hash3(i + vec3(0.0,0.0,1.0)), hash3(i + vec3(1.0,0.0,1.0)), u.x),
            mix(hash3(i + vec3(0.0,1.0,1.0)), hash3(i + vec3(1.0,1.0,1.0)), u.x), u.y), u.z);
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    mat3 rot = mat3(0.00,  0.80,  0.60,
                    -0.80,  0.36, -0.48,
                    -0.60, -0.48,  0.64);
    for (int i = 0; i < 4; i++) {
        v += a * noise3(p);
        p = rot * p * 2.1 + vec3(1.2, 3.4, 5.6);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec3 dir = normalize(lookDir);
    float t = time * 0.25 * speed;
    float hBand = exp(-abs(dir.y * 3.0) * max(0.2, (2.2 - horizon * 0.8)));
    float ground = smoothstep(0.3, -0.6, dir.y) * 0.6;
    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;
    vec3 wind = vec3(t * 0.15, t * 0.05, t * 0.1);
    vec3 p = dir * 4.0 + wind;
    vec3 q = vec3(fbm3(p + wind * 0.5), fbm3(p - wind * 0.3), fbm3(p + vec3(2.1, 4.3, 1.7)));
    float mistFbm = fbm3(p + q * 1.8);
    vec3 baseColor = themeColor * 0.35 + vec3(0.02, 0.02, 0.03);
    vec3 glowColor = themeColor * 1.2 + vec3(0.1);
    vec3 finalColor = mix(baseColor, glowColor, smoothstep(0.3, 0.8, mistFbm));
    float mistAlpha = smoothstep(0.15, 0.75, mistFbm) * envelope * alpha;
    fragColor = vec4(finalColor, clamp(mistAlpha, 0.0, 1.0));
}