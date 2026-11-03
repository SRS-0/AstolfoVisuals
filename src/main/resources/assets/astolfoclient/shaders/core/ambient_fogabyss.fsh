#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float time;
    vec3 themeColor;
    float alpha;
    float speed;
    float density;
    float horizon;
    vec4 uDepthRange;
};

layout(location = 0) in vec3 lookDir;
layout(location = 0) out vec4 fragColor;







void main() {
    vec3 dir = normalize(lookDir);
    float t = time * 0.6 * speed;
    float hBand = exp(-abs(dir.y * 2.6) * max(0.2, (2.0 - horizon * 0.7)));
    float ground = smoothstep(0.2, -0.6, dir.y) * 0.6;
    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;
    vec2 p = dir.xz / (abs(dir.y) + 0.25) * 2.0;
    float r = length(p);
    float ripple = sin(r * 8.0 - t * 3.0) * 0.5 + 0.5;
    ripple = pow(ripple, 4.0);
    float grid = abs(sin(p.x * 3.0 + t)) * abs(sin(p.y * 3.0 - t));
    grid = smoothstep(0.4, 0.95, grid);
    vec3 voidDark = themeColor * 0.15 + vec3(0.01, 0.01, 0.02);
    vec3 neonCore = themeColor * 1.5 + vec3(0.2);
    vec3 color = mix(voidDark, neonCore, ripple * 0.7 + grid * 0.5);
    float abyssAlpha = (0.25 + ripple * 0.5 + grid * 0.25) * envelope * alpha;
    fragColor = vec4(color, clamp(abyssAlpha, 0.0, 1.0));
}