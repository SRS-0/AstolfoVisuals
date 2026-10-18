#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 texCoord0;
layout(location = 2) in vec3 vNormal;
layout(location = 3) in vec3 vPos;

layout(location = 0) out vec4 fragColor;

void main() {
    // Calculate direction from pixel to camera
    vec3 viewDir = normalize(-vPos);

    // Fresnel Math: The 3D equivalent of your 2D SDF edge glow
    float fresnel = pow(1.0 - max(dot(viewDir, vNormal), 0.0), 2.5);

    // GLASS DISTORTION: Warps the texture coordinates based on the 3D normal curve
    vec2 distortedTexCoord = texCoord0 + (vNormal.xy * fresnel * 0.04);

    vec4 texColor = texture(Sampler0, distortedTexCoord);
    if (texColor.a < 0.1) discard;

    // Mix the base texture with the theme color passed from Java
    vec4 baseColor = texColor * vertexColor * ColorModulator;

    // Blend base color with pure white gloss on the outer edges
    vec3 finalColor = mix(baseColor.rgb, vec3(1.0, 1.0, 1.0), fresnel * 0.85);

    // Increase alpha on edges for a thick 3D glass illusion (BaseAlpha -> FresnelAlpha)
    float finalAlpha = mix(baseColor.a, 1.0, fresnel * 0.8);

    fragColor = vec4(finalColor, finalAlpha);
}