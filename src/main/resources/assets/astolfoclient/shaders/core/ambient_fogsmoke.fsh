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







vec3 safe_tanh(vec3 x) {
    return 1.0 - 2.0 / (exp(2.0 * x) + 1.0);
}

float orb(vec3 p, float t) {
    return length(p - vec3(
            sin(t * 0.3) * 4.0,
            cos(t * 0.25) * 3.0,
            sin(t * 0.4) * 4.0));
}

void main() {
    vec3 dir = normalize(lookDir);
    float t = time * 0.8 * speed;
    float hBand = exp(-abs(dir.y * 2.8) * max(0.2, (2.0 - horizon * 0.7)));
    float ground = smoothstep(0.2, -0.5, dir.y) * 0.5;
    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;
    float d = 0.0;
    float e = 0.0;
    float s = 0.0;
    vec3 o = vec3(0.0);
    for (int i = 0; i < 36; i++) {
        vec3 p = dir * d + vec3(0.0, t * 0.5, 0.0);
        e = orb(p, t) - 0.1;
        vec4 c = cos(0.15 * t + p.y * 0.2 + vec4(0.0, 33.0, 11.0, 0.0));
        p.xz = mat2(c.x, c.y, c.z, c.w) * p.xz;
        s = 3.0 - abs(p.z);
        float a = 0.7;
        for (int j = 0; j < 3; j++) {
            p += cos(0.6 * t + p.yzx) * 0.25;
            s -= abs(dot(sin(0.12 * t + p * a), vec3(0.6))) / a;
            a += a;
        }
        e = max(0.5 * e, 0.02);
        s = min(0.05 + 0.2 * abs(s), e);
        d += s;
        o += themeColor * (1.0 / (s + e * 4.0));
        if (max(o.x, max(o.y, o.z)) > 18.0) break;
    }
    o = safe_tanh(o * 0.06);
    float smokeAlpha = length(o) * 0.6 * envelope * alpha;
    fragColor = vec4(o, clamp(smokeAlpha, 0.0, 1.0));
}