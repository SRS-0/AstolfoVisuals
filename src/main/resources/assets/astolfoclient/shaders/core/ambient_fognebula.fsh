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







float hash13(vec3 p) {
    p = fract(p * vec3(443.8975, 397.2973, 491.1871));
    p += dot(p, p.yxz + 19.19);
    return fract((p.x + p.y) * p.z);
}

float noise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float a = mix(hash13(i + vec3(0.0, 0.0, 0.0)), hash13(i + vec3(1.0, 0.0, 0.0)), f.x);
    float b = mix(hash13(i + vec3(0.0, 1.0, 0.0)), hash13(i + vec3(1.0, 1.0, 0.0)), f.x);
    float c = mix(hash13(i + vec3(0.0, 0.0, 1.0)), hash13(i + vec3(1.0, 0.0, 1.0)), f.x);
    float d = mix(hash13(i + vec3(0.0, 1.0, 1.0)), hash13(i + vec3(1.0, 1.0, 1.0)), f.x);
    return mix(mix(a, b, f.y), mix(c, d, f.y), f.z);
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.05 + vec3(3.1, 7.4, 2.8);
        a *= 0.5;
    }
    return v;
}

void main() {
    vec3 dir = normalize(lookDir);
    float t = time * 0.2 * speed;
    float hBand = exp(-abs(dir.y * 2.2) * max(0.2, (1.8 - horizon * 0.6)));
    float ground = smoothstep(0.2, -0.6, dir.y) * 0.5;
    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;
    vec3 p = dir * 3.5 + vec3(t * 0.1, t * 0.03, -t * 0.07);
    float clouds = fbm3(p);
    float filaments = fbm3(p * 2.2 + vec3(-t * 0.05, t * 0.08, 0.0));
    float neb = smoothstep(0.35, 0.85, clouds) * 0.75 + smoothstep(0.45, 0.9, filaments) * 0.35;
    vec3 warm = mix(themeColor, vec3(1.0, 0.6, 0.3), 0.35);
    vec3 cool = mix(themeColor * 0.6, vec3(0.2, 0.5, 1.0), 0.4);
    vec3 nebCol = mix(cool, warm, smoothstep(0.2, 0.8, clouds));
    vec3 core = mix(themeColor * 1.4, vec3(1.0), 0.4);
    vec3 finalCol = nebCol * neb * 1.3 + core * pow(max(neb - 0.45, 0.0), 1.6) * 1.2;
    float nebAlpha = neb * envelope * alpha;
    fragColor = vec4(finalCol, clamp(nebAlpha, 0.0, 1.0));
}