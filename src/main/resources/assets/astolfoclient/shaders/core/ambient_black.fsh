#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
};

out vec4 fragColor;

void main() {
    fragColor = vec4(0.0, 0.0, 0.0, alpha);
}