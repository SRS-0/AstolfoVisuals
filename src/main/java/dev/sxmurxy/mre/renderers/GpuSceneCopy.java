package dev.sxmurxy.mre.renderers;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;

/** Separate source texture prevents sampling the attachment currently being drawn. */
public final class GpuSceneCopy {
    private static TextureTarget copy;
    static {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (copy != null) { copy.destroyBuffers(); copy = null; }
        });
    }
    private GpuSceneCopy() {}
    private static TextureTarget target() {
        RenderSystem.assertOnRenderThread();
        var source = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        if (copy == null) copy = new TextureTarget("Astolfo scene copy", source.width, source.height, source.getColorTexture().getFormat(), null);
        else if (copy.width != source.width || copy.height != source.height) copy.resize(source.width, source.height);
        return copy;
    }
    public static TextureTarget capture() {
        if(xyz.angames.astolfoclient.client.render.compat.WorldPass.isActive())return xyz.angames.astolfoclient.client.render.compat.WorldPass.outside(GpuSceneCopy::capture);
        var result = target();
        var source = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source.getColorTexture(), result.getColorTexture(), 0, 0, 0, 0, 0, source.width, source.height);
        return result;
    }
    public static TextureTarget gui() {
        if (GuiContext.current() == null) return capture();
        GuiContext.requestScene();
        return target();
    }
}
