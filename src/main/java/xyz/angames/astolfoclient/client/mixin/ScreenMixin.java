package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.util.IASAccountHelper;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Screen.class})
public abstract class ScreenMixin {
    // Keep the scene/panorama clear behind menus, including the HUD editor.
    @Inject(method={
        "extractBlurredBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
        "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
        "extractMenuBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIII)V",
        "extractTransparentBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"
    }, at=@At("HEAD"), cancellable=true)
    private void astolfo$clearScreenBackdrop(CallbackInfo ci) {
        ci.cancel();
    }

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable> T addRenderableWidget(T var1);

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void onInit(CallbackInfo ci) {
        Screen screen = (Screen)(Object)this;
        IASAccountHelper.onScreenInit(screen, this::addRenderableWidget);
    }
}

