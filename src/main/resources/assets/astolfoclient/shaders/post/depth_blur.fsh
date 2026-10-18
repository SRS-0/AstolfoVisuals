#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform DepthBlurConfig {
    vec2 BlurDir;
    float Radius;
    float Near;
    float Far;
    float FogStart;
    float FogEnd;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float linearizeDepth(float depth, float near, float far) {
    float ndc = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndc * (far - near));
}

void main() {
    vec2 sampleStep = (1.0 / InSize) * BlurDir;
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Skip blurring for sky/background pixels to keep the skybox/stars sharp
    if (rawDepth == 0.0) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    float depth = 1.0 - rawDepth;
    float z = linearizeDepth(depth, Near, Far);
    
    // Scale blur based on custom fog start and end
    float weight = clamp((z - FogStart) / (FogEnd - FogStart), 0.0, 1.0);

    if (weight <= 0.005 || Radius < 0.5) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec4 blurred = vec4(0.0);
    float totalWeight = 0.0;
    float actualRadius = round(Radius);
    vec2 step = sampleStep * weight;

    for (float a = -actualRadius; a <= actualRadius; a += 1.0) {
        float sigma = actualRadius * 0.5;
        float gWeight = exp(-(a * a) / (2.0 * sigma * sigma));
        blurred += texture(InSampler, texCoord + step * a) * gWeight;
        totalWeight += gWeight;
    }
    
    fragColor = blurred / totalWeight;
}
