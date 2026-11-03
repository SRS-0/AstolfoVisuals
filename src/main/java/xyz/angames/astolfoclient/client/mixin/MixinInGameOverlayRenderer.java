package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={InGameOverlayRenderer.class})
public class MixinInGameOverlayRenderer {
    @Inject(method={"renderFireOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void onRenderFireOverlay(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Sprite sprite, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.fire.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"renderInWallOverlay"}, at={@At(value="HEAD")}, cancellable=true)
    private static void onRenderInWallOverlay(Sprite sprite, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.blockOverlay.get()) {
            ci.cancel();
        }
    }
}

