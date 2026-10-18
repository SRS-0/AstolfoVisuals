#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform CustomUniforms {
    vec2 uResolution;
    float uDistortion;
    float uProgress;
    float uAlpha;
    vec3 uThemeColor;
};

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(location = 103) in vec2 texCoord;
layout(location = 103) in vec2 vCenterScreen;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 circleTex = texture(Sampler1, texCoord);
    float texMask = circleTex.a;
    if (texMask <= 0.01) discard;

    float life = clamp(1.0 - uProgress, 0.0, 1.0);

    vec2 screenUV = gl_FragCoord.xy / max(uResolution, vec2(1.0, 1.0));
    vec2 diff = vCenterScreen - screenUV;
    float dist = length(diff * uResolution);

    vec2 warpDir = (dist > 0.001) ? normalize(diff) : vec2(0.0);
    float warpAmount = pow(max(dist, 8.0), -1.8) * (uDistortion * 4000.0 * life);
    vec2 warpedUV = clamp(screenUV + warpDir * warpAmount, vec2(0.001), vec2(0.999));

    vec4 sceneColor = texture(Sampler0, warpedUV);
    vec3 themedTexRgb = circleTex.rgb * uThemeColor;
    vec3 finalRgb = sceneColor.rgb + themedTexRgb * 1.3;

    float finalAlpha = texMask * life * uAlpha;
    fragColor = vec4(finalRgb, clamp(finalAlpha, 0.0, 1.0));
}
