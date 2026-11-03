package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.render.compat.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ScreenEffectRenderer.class})
public class MixinInGameOverlayRenderer {
    @Inject(method={"submitFire"}, at={@At(value="HEAD")}, cancellable=true)
    private static void onRenderFireOverlay(CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.fire.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"submitBlockSprite"}, at={@At(value="HEAD")}, cancellable=true)
    private static void onRenderInWallOverlay(CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.blockOverlay.get()) {
            ci.cancel();
        }
    }
}

