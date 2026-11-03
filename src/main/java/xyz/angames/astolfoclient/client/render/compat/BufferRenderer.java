package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;

public final class BufferRenderer {
    public static com.mojang.renderpearl.api.commands.RenderPass currentPass;
    private BufferRenderer() {}
    public static void draw(MeshData mesh) {drawWithGlobalProgram(mesh);}
    public static void drawWithGlobalProgram(MeshData mesh) {
        if(WorldPass.isActive()){WorldPass.outside(()->{drawWithGlobalProgram(mesh);return null;});return;}
        if(WideLines.draw(mesh))return;
        var api = com.mojang.blaze3d.systems.RenderSystem.getDevice();
        var program = RenderSystem.program;
        if(program == null) throw new IllegalStateException("No effect selected");
        var depthRange = program.getUniform("uDepthZeroToOne");
        if (depthRange != null) depthRange.set(api.getDeviceInfo().isZZeroToOne() ? 1f : 0f);
        var skyDepthRange = program.getUniform("uDepthRange");
        if (skyDepthRange != null) skyDepthRange.set(api.getDeviceInfo().isZZeroToOne() ? 1f : 0f);
        var data = program.parameters();
        try(mesh; var vertices=api.createBuffer(()->"Astolfo world vertices",GpuBuffer.USAGE_VERTEX,mesh.vertexBuffer());
            var parameters=api.createBuffer(()->"Astolfo world uniforms",GpuBuffer.USAGE_UNIFORM,data)) {
            var state=mesh.drawState();
            var pipeline=program.pipeline(state.format(),state.primitiveTopology());
            var indices=com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(state.primitiveTopology());
            var indexBuffer=indices.getBuffer(state.indexCount());
            var target=RenderSystem.target!=null?RenderSystem.target:Minecraft.getInstance().gameRenderer.mainRenderTarget();
            var color=xyz.angames.astolfoclient.client.render.compat.RenderSystem.outputColorTextureOverride;
            var depth=xyz.angames.astolfoclient.client.render.compat.RenderSystem.outputDepthTextureOverride;
            if(color==null || RenderSystem.target!=null) color=target.getColorTextureView();
            if(depth==null && RenderSystem.depth) depth=target.getDepthTextureView();
            // A post effect samples scene depth; it must not also bind it as an attachment.
            if(!RenderSystem.depth && !RenderSystem.depthWrite) depth=null;
            var transform=com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(),new Vector4f(1),new Vector3f(),new Matrix4f());
            var ownPass=currentPass==null;
            var pass=ownPass?api.createCommandEncoder().createRenderPass(()->"Astolfo world effect",color,java.util.Optional.empty(),depth,OptionalDouble.empty()):currentPass;
            try {
                pass.setPipeline(com.mojang.blaze3d.systems.RenderSystem.getCompiledPipeline(pipeline));
                if(!program.screen) {
                    com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms",transform);
                }
                pass.setUniform("EffectData",parameters);
                for(int slot=0;slot<program.samplers().size();slot++) {
                    var texture=RenderSystem.textures.get(slot);
                    if(texture==null) throw new IllegalStateException("Missing texture slot "+slot);
                    pass.setUniform(program.samplers().get(slot),texture.view(),texture.sampler());
                }
                pass.setVertexBuffer(0,vertices.slice());
                pass.setIndexBuffer(indexBuffer,indices.type());
                pass.drawIndexed(state.indexCount(),1,0,0,0);
            } finally {if(ownPass)pass.close();}
        } finally {MemoryUtil.memFree(data);}
    }
}
