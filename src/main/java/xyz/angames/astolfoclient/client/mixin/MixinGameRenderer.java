package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={GameRenderer.class})
public class MixinGameRenderer {
    @Inject(method={"showFloatingItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.totem.get()) {
            ci.cancel();
        }
    }

    @Inject(method={"tiltViewWhenHurt"}, at={@At(value="HEAD")}, cancellable=true)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.hurtCam.get()) {
            ci.cancel();
        }
    }
}

