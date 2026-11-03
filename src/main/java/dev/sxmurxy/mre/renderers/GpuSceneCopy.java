package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.SimpleFramebuffer;

/** Separate source texture prevents sampling the attachment currently being drawn. */
public final class GpuSceneCopy {
    private static SimpleFramebuffer copy;
    static {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (copy != null) { copy.delete(); copy = null; }
        });
    }
    private GpuSceneCopy() {}
    public static SimpleFramebuffer capture() {
        RenderSystem.assertOnRenderThread();
        var source = MinecraftClient.getInstance().getFramebuffer();
        if (copy == null) copy = new SimpleFramebuffer("Astolfo scene copy", source.textureWidth, source.textureHeight, false);
        else if (copy.textureWidth != source.textureWidth || copy.textureHeight != source.textureHeight) copy.resize(source.textureWidth, source.textureHeight);
        if(GuiContext.current()!=null)GuiContext.requestScene();
        else RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source.getColorAttachment(), copy.getColorAttachment(), 0, 0, 0, 0, 0, source.textureWidth, source.textureHeight);
        return copy;
    }
}
