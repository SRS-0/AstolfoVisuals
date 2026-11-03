#version 150

#moj_import <mre:common.glsl>

in vec3 Position; // POSITION_TEXTURE_COLOR vertex attributes
in vec2 UV0;
in vec4 Color;

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

out vec2 FragCoord;
out vec2 TexCoord;
out vec4 FragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    FragCoord = rvertexcoord(gl_VertexID);
    TexCoord = UV0;
    FragColor = Color;
} 