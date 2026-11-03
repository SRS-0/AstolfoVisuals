package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Fullscreen effect that uses the active GPU backend and explicit source views. */
public final class ScreenPass {
    private static final Map<String,ShaderProgram> PROGRAMS=new HashMap<>();
    private final ShaderProgram program;
    private final Map<Integer, RenderSystem.TextureBinding> inputs = new HashMap<>();
    private com.mojang.blaze3d.pipeline.BlendFunction blendFunction=com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT;
    public ScreenPass(String name) {program=PROGRAMS.computeIfAbsent(name,n->new ShaderProgram(new ShaderProgramKey(Identifier.of("astolfoclient","core/"+n),VertexFormats.POSITION_TEXTURE,Defines.EMPTY)));}
    public ScreenPass uniform(String name,float... values) {var uniform=program.getUniform(name);if(uniform!=null)uniform.set(values);return this;}
    public ScreenPass blend(com.mojang.blaze3d.pipeline.BlendFunction value) {blendFunction=value;return this;}
    public ScreenPass texture(int slot,GpuTextureView texture,boolean linear) {
        var sampler=com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(linear?FilterMode.LINEAR:FilterMode.NEAREST);
        inputs.put(slot,new RenderSystem.TextureBinding(texture,sampler));return this;
    }
    public void draw(Framebuffer target) {draw(target,false);}
    public void draw(Framebuffer target,boolean blending) {
        var mesh=Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,VertexFormats.POSITION_TEXTURE);
        mesh.vertex(-1,-1,0).texture(0,0);mesh.vertex(1,-1,0).texture(1,0);
        mesh.vertex(1,1,0).texture(1,1);mesh.vertex(-1,1,0).texture(0,1);
        drawMesh(mesh.end(),target,false,com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST,blending);
    }
    public void drawMesh(net.minecraft.client.render.BuiltBuffer mesh,Framebuffer target,boolean depthTest,com.mojang.blaze3d.platform.DepthTestFunction depthFunction,boolean blending) {
        var savedProgram=RenderSystem.program;var savedTarget=RenderSystem.target;
        var savedTextures=new HashMap<>(RenderSystem.textures);
        boolean blend=RenderSystem.blend,depth=RenderSystem.depth,write=RenderSystem.depthWrite,cull=RenderSystem.cull;
        var savedDepthFunction=RenderSystem.depthFunction;
        var savedBlendFunction=RenderSystem.blendFunction;
        try {
            RenderSystem.program=program;RenderSystem.target=target;
            RenderSystem.textures.clear();RenderSystem.textures.putAll(inputs);
            RenderSystem.blend=blending;RenderSystem.depth=depthTest;RenderSystem.depthWrite=false;RenderSystem.cull=false;
            RenderSystem.depthFunction=depthFunction;
            RenderSystem.blendFunction=blendFunction;
            BufferRenderer.drawWithGlobalProgram(mesh);
        } finally {
            RenderSystem.program=savedProgram;RenderSystem.target=savedTarget;
            RenderSystem.textures.clear();RenderSystem.textures.putAll(savedTextures);
            RenderSystem.blend=blend;RenderSystem.depth=depth;RenderSystem.depthWrite=write;RenderSystem.cull=cull;
            RenderSystem.depthFunction=savedDepthFunction;
            RenderSystem.blendFunction=savedBlendFunction;
        }
    }
}
