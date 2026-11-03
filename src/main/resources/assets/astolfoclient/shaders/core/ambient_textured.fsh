#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec3 themeColor;
    bool themeSync;
    vec4 uDepthRange;
};

layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;
uniform sampler2D uTexture;



void main() {
    vec4 col = texture(uTexture, vTexCoord);
    if (themeSync) {
        col.rgb *= themeColor;
    }
    fragColor = vec4(col.rgb, col.a * alpha);
}