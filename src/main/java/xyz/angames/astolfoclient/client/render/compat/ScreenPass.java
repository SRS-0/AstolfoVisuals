package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

/** Fullscreen effect that uses the active GPU backend and explicit source views. */
public final class ScreenPass {
    private static final Map<String,ShaderProgram> PROGRAMS=new HashMap<>();
    private final ShaderProgram program;
    private final Map<Integer, RenderSystem.TextureBinding> inputs = new HashMap<>();
    private com.mojang.renderpearl.api.pipeline.BlendFunction blendFunction=com.mojang.renderpearl.api.pipeline.BlendFunction.TRANSLUCENT;
    public ScreenPass(String name) {program=PROGRAMS.computeIfAbsent(name,n->new ShaderProgram(new ShaderProgramKey(Identifier.fromNamespaceAndPath("astolfoclient","core/"+n),DefaultVertexFormat.POSITION_TEX,ShaderDefines.EMPTY)));}
    public ScreenPass uniform(String name,float... values) {var uniform=program.getUniform(name);if(uniform!=null)uniform.set(values);return this;}
    public ScreenPass blend(com.mojang.renderpearl.api.pipeline.BlendFunction value) {blendFunction=value;return this;}
    public ScreenPass texture(int slot,GpuTextureView texture,boolean linear) {
        var sampler=com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(linear?FilterMode.LINEAR:FilterMode.NEAREST);
        inputs.put(slot,new RenderSystem.TextureBinding(texture,sampler));return this;
    }
    public void draw(RenderTarget target) {draw(target,false);}
    public void draw(RenderTarget target,boolean blending) {
        var mesh=Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS,DefaultVertexFormat.POSITION_TEX);
        mesh.addVertex(-1,-1,0).setUv(0,0);mesh.addVertex(1,-1,0).setUv(1,0);
        mesh.addVertex(1,1,0).setUv(1,1);mesh.addVertex(-1,1,0).setUv(0,1);
        drawMesh(mesh.buildOrThrow(),target,false,com.mojang.renderpearl.api.pipeline.CompareOp.ALWAYS_PASS,blending);
    }
    public void drawMesh(com.mojang.blaze3d.vertex.MeshData mesh,RenderTarget target,boolean depthTest,com.mojang.renderpearl.api.pipeline.CompareOp depthFunction,boolean blending) {
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
