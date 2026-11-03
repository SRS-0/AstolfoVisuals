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
    float t = time * 0.8 * speed;
    float hBand = exp(-abs(dir.y * 3.0) * max(0.2, (2.2 - horizon * 0.8)));
    float ground = smoothstep(0.15, -0.5, dir.y) * 0.4;
    float envelope = clamp(hBand + ground, 0.0, 1.0) * density;
    float angle = atan(dir.z, dir.x);
    float wave1 = sin(angle * 4.0 + dir.y * 8.0 + t) * 0.5 + 0.5;
    float wave2 = sin(angle * 7.0 - dir.y * 12.0 - t * 1.3) * 0.5 + 0.5;
    float wave3 = sin(angle * 11.0 + t * 0.7) * 0.5 + 0.5;
    float waveTotal = wave1 * 0.5 + wave2 * 0.3 + wave3 * 0.2;
    float curtain = pow(waveTotal, 2.2);
    vec3 c1 = themeColor;
    vec3 c2 = mix(themeColor, vec3(0.2, 1.0, 0.8), 0.5);
    vec3 c3 = mix(themeColor, vec3(1.0, 0.3, 0.9), 0.4);
    vec3 waveColor = mix(c1, c2, wave1);
    waveColor = mix(waveColor, c3, wave2 * 0.6) * (curtain * 1.5 + 0.2);
    float waveAlpha = (curtain * 0.85 + 0.15) * envelope * alpha;
    fragColor = vec4(waveColor, clamp(waveAlpha, 0.0, 1.0));
}