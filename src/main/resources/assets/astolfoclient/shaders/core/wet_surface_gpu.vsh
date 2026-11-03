#version 150
layout(std140) uniform EffectData {
    vec2 u_Resolution;
    mat4 u_ViewMatrix;
    mat4 u_ProjectionMatrix;
    mat4 u_InverseProjectionMatrix;
    mat4 u_InverseViewMatrix;
    vec3 u_SunDir;
    vec3 u_CameraPos;
    float u_Time;
    float u_Wetness;
    float u_PuddleCoverage;
    float u_ReflectionStrength;
    float u_MaxDistance;
    float u_RippleStrength;
    float u_RainAmount;
    float u_AmbientLight;
    float u_LightningFlash;
    int u_ReflectionSteps;
    int u_Ripples;
    vec3 u_ThemeColor;
    int u_UseThemeColor;
};


in vec2 Position;
in vec2 UV0;

out vec2 vUv;

void main() {
    vUv = UV0;
    gl_Position = vec4(Position, 0.0, 1.0);
}
