#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    float alpha;
    int doBlur;
    float blurStrength;
    vec2 resolution;
    float time;
    vec3 themeColor;
    int useHandColor;
    int mode;
};

uniform sampler2D inTexture;
uniform sampler2D bgTexture;








layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

float wave(float x, float y, float time) {
    return sin(10.0*x + 10.0*y + time) / 5.0 +
           sin(20.0*x + 15.0*y - time * 1.2) / 3.0 +
           sin(4.0*x + 10.0*y + time * 0.8) / -4.0 +
           sin(y + time * 0.5) / 2.0 +
           sin(x*x*y*20.0 + time * 1.4) +
           sin(x * 20.0 + 4.0 - time) / 5.0 +
           sin(y * 30.0 + time * 1.1) / 5.0 +
           sin(x - time * 0.6) / 4.0;
}

void main() {
    vec4 tex = texture(inTexture, texCoord);
    if(tex.a < 0.01) discard;
    vec3 colorToUse = (useHandColor == 1) ? tex.rgb : themeColor;
    if (mode == 1) {
        vec2 uv = (2.0 * (texCoord * resolution) - resolution) / min(resolution.x, resolution.y);
        for(float i = 1.0; i < 12.0; i++){
            uv.x += 0.6 / i * cos(i * 2.5 * uv.y + time);
            uv.y += 0.6 / i * cos(i * 1.5 * uv.x + time);
        }
        float intensity = 0.1 / abs(sin(time - uv.y - uv.x));
        fragColor = vec4(intensity * colorToUse, tex.a * alpha);
    } else if (mode == 2) {
        vec2 uv = texCoord;
        float z = wave(uv.x, uv.y, time) + 2.0;
        z *= 5.0 + 2.0 * sin(time * 0.4);
        float d = fract(z);
        if (mod(z, 2.0) > 1.0) d = 1.0 - d;
        d = d / fwidth(z);
        float lineVal = clamp(1.0 - d, 0.0, 1.0);
        fragColor = vec4(lineVal * colorToUse, tex.a * alpha);
    } else if (mode == 3) {
        float epsX = 2.0 / resolution.x;
        float epsY = 2.0 / resolution.y;
        float aLeft  = texture(inTexture, texCoord + vec2(-epsX, 0.0)).a;
        float aRight = texture(inTexture, texCoord + vec2(epsX, 0.0)).a;
        float aDown  = texture(inTexture, texCoord + vec2(0.0, -epsY)).a;
        float aUp    = texture(inTexture, texCoord + vec2(0.0, epsY)).a;
        vec2 grad = vec2(aRight - aLeft, aUp - aDown);
        vec2 dir = length(grad) > 0.001 ? normalize(grad) : vec2(0.0);
        vec2 mirrorCoord = vec2(1.0 - texCoord.x, texCoord.y);
        vec2 distortedMirror = mirrorCoord + vec2(-dir.x, dir.y) * 0.05 * (1.0 - tex.a);
        distortedMirror = clamp(distortedMirror, 0.001, 0.999);
        vec3 reflected = texture(bgTexture, distortedMirror).rgb;
        float shine = pow(max(0.0, 1.0 - length(texCoord - vec2(0.5))), 4.0);
        vec3 glassColor = mix(reflected, colorToUse, 0.25) + vec3(shine * 0.15);
        float edgeGlow = smoothstep(0.0, 1.0, length(grad) * 2.0);
        glassColor = mix(glassColor, colorToUse * 1.5, edgeGlow * 0.5);
        fragColor = vec4(glassColor, tex.a * alpha);
    } else if (doBlur == 1) {
        vec4 sum = vec4(0.0);
        vec2 offset = (blurStrength * 0.5) / resolution;
        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, -offset.y * 1.5)) * 0.09;
        sum += texture(bgTexture, texCoord + vec2(0.0, -offset.y * 1.5)) * 0.12;
        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, -offset.y * 1.5)) * 0.09;
        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, 0.0)) * 0.12;
        sum += texture(bgTexture, texCoord) * 0.16;
        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, 0.0)) * 0.12;
        sum += texture(bgTexture, texCoord + vec2(-offset.x * 1.5, offset.y * 1.5)) * 0.09;
        sum += texture(bgTexture, texCoord + vec2(0.0, offset.y * 1.5)) * 0.12;
        sum += texture(bgTexture, texCoord + vec2(offset.x * 1.5, offset.y * 1.5)) * 0.09;
        vec4 bgColor = sum;
        vec3 glassColor = mix(bgColor.rgb, tex.rgb, 0.4) + vec3(tex.a * 0.1);
        fragColor = vec4(glassColor, tex.a * alpha);
    } else {
        fragColor = vec4(tex.rgb, tex.a * alpha);
    }
}
