package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.buffers.GpuBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.*;
import org.lwjgl.system.MemoryUtil;

public final class BufferRenderer {
    private BufferRenderer() {}
    public static void draw(BuiltBuffer mesh) {drawWithGlobalProgram(mesh);}
    public static void drawWithGlobalProgram(BuiltBuffer mesh) {
        if(WideLines.draw(mesh))return;
        var api = com.mojang.blaze3d.systems.RenderSystem.getDevice();
        var program = RenderSystem.program;
        if(program == null) throw new IllegalStateException("No effect selected");
        var data = program.parameters();
        try(mesh; var vertices=api.createBuffer(()->"Astolfo world vertices",GpuBuffer.USAGE_VERTEX,mesh.getBuffer());
            var parameters=api.createBuffer(()->"Astolfo world uniforms",GpuBuffer.USAGE_UNIFORM,data)) {
            var state=mesh.getDrawParameters();
            var pipeline=program.pipeline(state.format(),state.mode());
            var indices=com.mojang.blaze3d.systems.RenderSystem.getSequentialBuffer(state.mode());
            var indexBuffer=indices.getIndexBuffer(state.indexCount());
            var target=RenderSystem.target!=null?RenderSystem.target:MinecraftClient.getInstance().getFramebuffer();
            var color=com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride;
            var depth=com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride;
            if(color==null || RenderSystem.target!=null) color=target.getColorAttachmentView();
            if(depth==null && RenderSystem.depth) depth=target.getDepthAttachmentView();
            var transform=com.mojang.blaze3d.systems.RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(),new Vector4f(1),new Vector3f(),new Matrix4f());
            try(var pass=api.createCommandEncoder().createRenderPass(()->"Astolfo world effect",color,OptionalInt.empty(),depth,OptionalDouble.empty())) {
                pass.setPipeline(pipeline);
                if(!program.screen) {
                    com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms",transform);
                }
                pass.setUniform("EffectData",parameters);
                for(int slot=0;slot<program.samplers().size();slot++) {
                    var texture=RenderSystem.textures.get(slot);
                    if(texture==null) throw new IllegalStateException("Missing texture slot "+slot);
                    pass.bindTexture(program.samplers().get(slot),texture.view(),texture.sampler());
                }
                pass.setVertexBuffer(0,vertices);
                pass.setIndexBuffer(indexBuffer,indices.getIndexType());
                pass.drawIndexed(0,0,state.indexCount(),1);
            }
        } finally {MemoryUtil.memFree(data);}
    }
}
