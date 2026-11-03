#version 150

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; vec2 DepthSize; };
layout(std140) uniform DepthBlurConfig { vec2 BlurDir; float Radius; float Near; float Far; float FogStart; float FogEnd; };


out vec4 fragColor;

float linearizeDepth(float depth, float near, float far) {
    float ndc = depth * 2.0 - 1.0;
    return (2.0 * near * far) / (far + near - ndc * (far - near));
}

void main() {
    float rawDepth = texture(DepthSampler, texCoord).r;
    
    // Skip blurring for sky/background pixels to keep the skybox/stars sharp
    if (rawDepth >= 0.999999) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    float depth = rawDepth;
    float z = linearizeDepth(depth, Near, Far);
    
    // Scale blur based on custom fog start and end
    float weight = clamp((z - FogStart) / max(0.0001, FogEnd - FogStart), 0.0, 1.0);

    if (weight <= 0.005 || Radius < 0.5) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec4 blurred = vec4(0.0);
    float totalWeight = 0.0;
    float actualRadius = round(Radius);
    vec2 step = BlurDir / InSize * weight;

    for (float a = -actualRadius; a <= actualRadius; a += 1.0) {
        float sigma = actualRadius * 0.5;
        float gWeight = exp(-(a * a) / (2.0 * sigma * sigma));
        blurred += texture(InSampler, texCoord + step * a) * gWeight;
        totalWeight += gWeight;
    }
    
    fragColor = blurred / totalWeight;
}
