#version 150
layout(std140) uniform EffectData {
    mat4 uModelViewMat;
    mat4 uProjMat;
    float alpha;
    vec3 themeColor;
    bool themeSync;
};

in vec2 vTexCoord;
out vec4 fragColor;
uniform sampler2D uTexture;



void main() {
    vec4 col = texture(uTexture, vTexCoord);
    if (themeSync) {
        col.rgb *= themeColor;
    }
    fragColor = vec4(col.rgb, col.a * alpha);
}