#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float Time;
    vec3 Accent;
    vec4 ColorModulator;
    vec4 uDepthRange;
};


layout(location = 0) in vec3 Direction;
layout(location = 1) in vec4 vColor;





layout(location = 0) out vec4 OutColor;

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
    v += noise(p) * a;
    p = p * 2.03 + vec3(7.1, 3.2, 5.4);
    a *= 0.52;
    v += noise(p) * a;
    p = p * 2.11 + vec3(1.9, 8.3, 2.7);
    a *= 0.52;
    v += noise(p) * a;
    return v;
}

vec3 rotateY(vec3 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec3(p.x * c - p.z * s, p.y, p.x * s + p.z * c);
}

vec3 starLayer(vec3 d, float density, float threshold, vec3 tint) {
    vec3 p = d * density;
    vec3 cell = floor(p);
    float h = hash13(cell);
    if (h < threshold) {
        return vec3(0.0);
    }

    vec3 f = fract(p) - 0.5;
    float disc = 1.0 - smoothstep(0.0, 0.045, length(f));
    float twinkle = 0.65 + 0.35 * sin(Time * 2.4 + h * 71.0);
    return mix(vec3(1.0), tint, 0.22) * disc * twinkle * ((h - threshold) / (1.0 - threshold));
}

void main() {
    vec3 d = normalize(Direction);
    vec3 accent = max(Accent, vec3(0.05));

    float vertical = abs(d.y);
    vec3 deep = accent * 0.16 + vec3(0.004, 0.003, 0.014);
    vec3 horizon = mix(deep, accent * 0.55, 0.55);
    vec3 color = mix(horizon, deep, smoothstep(0.0, 0.9, vertical));

    vec3 rd = rotateY(d, Time * 0.035);
    vec3 q = rd * 2.0 + vec3(Time * 0.035, Time * 0.018, -Time * 0.025);
    float clouds = fbm3(q);
    float filaments = fbm3(rd * 5.0 + vec3(-Time * 0.02, Time * 0.03, Time * 0.015));

    float nebula = smoothstep(0.42, 0.92, clouds) * 0.78 + smoothstep(0.55, 0.96, filaments) * 0.32;
    float band = 1.0 - abs(dot(d, normalize(vec3(0.52, 0.25, 0.82)))) * 1.35;
    band = smoothstep(0.18, 0.9, band);
    nebula += band * (0.18 + 0.45 * clouds);

    vec3 warm = mix(accent, vec3(1.0, 0.54, 0.2), 0.42);
    vec3 cool = mix(accent, vec3(0.16, 0.52, 1.0), 0.45);
    vec3 nebColor = mix(cool, warm, smoothstep(0.2, 0.95, clouds));
    vec3 core = mix(accent * 1.45, vec3(1.0, 0.95, 0.88), 0.35);
    color += nebColor * nebula * 1.2;
    color += core * pow(max(nebula - 0.52, 0.0), 1.7) * 1.25;

    float horizonGlow = pow(1.0 - smoothstep(0.0, 0.42, abs(d.y)), 1.8);
    color += mix(deep * 2.0, warm, 0.55) * horizonGlow * 0.35;

    vec3 starDir = rotateY(d, Time * 0.008);
    color += starLayer(starDir, 180.0, 0.989, mix(accent, vec3(1.0), 0.65)) * (1.0 - nebula * 0.45);
    color += starLayer(starDir, 72.0, 0.994, accent) * 1.25;

    color = color / (1.0 + color * 0.45);
    color = pow(color, vec3(0.92));

    OutColor = vec4(color, 1.0) * ColorModulator * vColor;
}
