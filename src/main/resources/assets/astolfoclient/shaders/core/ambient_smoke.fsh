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




vec3 safe_tanh(vec3 x) {
    return 1.0 - 2.0 / (exp(2.0 * x) + 1.0);
}

float orb(vec3 p) {
    float t = time * 4.0;
    return length(p - vec3(
            sin(sin(t * 0.2) + t * 0.4) * 6.0,
            12.0 + time + cos(t * 0.3) * 8.0,
            1.0 + sin(sin(t * 0.5) + t * 0.2) * 4.0));
}

void main() {
    vec3 dir = normalize(lookDir);
    float d = 0.0;
    float e = 0.0;
    float s = 0.0;
    float t = time;
    vec3 o = vec3(0.0);
    
    for (int i = 0; i < 72; i++) {
        vec3 p = dir * d + vec3(0.0, t, 0.0);
        e = orb(p) - 0.1;
        
        vec4 c = cos(0.1 * t + p.y / 8.0 + vec4(0.0, 33.0, 11.0, 0.0));
        p.xz *= mat2(c.x, c.y, c.z, c.w);
        s = 4.0 - abs(p.z);
        
        float a = 0.8;
        for (int j = 0; j < 4; j++) {
            p += cos(0.7 * t + p.yzx) * 0.2;
            s -= abs(dot(sin(0.1 * t + p * a), vec3(0.6))) / a;
            a += a;
        }
        
        e = max(0.5 * e, 0.01);
        s = min(0.03 + 0.2 * abs(s), e);
        d += s;
        o += themeColor * (1.0 / (s + e * 3.0));
        
        if (max(o.x, max(o.y, o.z)) > 25.0) break;
    }
    
    o = safe_tanh(o / 10.0);
    fragColor = vec4(o, alpha);
}