package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.scores.Objective;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public class MixinInGameHud {
   @Inject(
      method = "displayScoreboardSidebar",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onRenderScoreboard(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
      NoRenderModule noRender = NoRenderModule.getInstance();
      if (noRender != null && noRender.isEnabled() && noRender.scoreboard.get()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractPortalOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderPortalOverlay(GuiGraphicsExtractor context, float f, CallbackInfo ci) {
      NoRenderModule noRender = NoRenderModule.getInstance();
      if (noRender != null && noRender.isEnabled() && noRender.portal.get()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractTextureOverlay", at = @At("HEAD"), cancellable = true)
   private void onRenderPumpkinOverlay(GuiGraphicsExtractor context, Identifier texture, float opacity, CallbackInfo ci) {
      NoRenderModule noRender = NoRenderModule.getInstance();
      if (noRender != null && noRender.isEnabled() && noRender.pumpkin.get() && texture.getPath().contains("pumpkin")) {
         ci.cancel();
      }
   }
}
