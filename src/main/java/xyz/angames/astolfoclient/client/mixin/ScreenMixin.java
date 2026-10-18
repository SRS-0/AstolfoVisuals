package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarratableEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.util.IASAccountHelper;

@Environment(EnvType.CLIENT)
@Mixin(Screen.class)
public abstract class ScreenMixin {
   @Shadow
   protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T var1);

   @Inject(method = "init()V", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      Screen screen = (Screen)(Object)this;
      IASAccountHelper.onScreenInit(screen, this::addRenderableWidget);
   }
}
