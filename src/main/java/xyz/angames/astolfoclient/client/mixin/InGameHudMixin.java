package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public class InGameHudMixin {
   @Unique
   private boolean astolfo$shifted;

   @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
   private void onRenderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (AstolfoclientClient.moduleManager != null
         && AstolfoclientClient.moduleManager.getModuleByName("Interface") instanceof InterfaceModule iface
         && iface.isEnabled()
         && iface.effectHud.get()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
   private void onRenderCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      Module crosshairModule = AstolfoclientClient.moduleManager.getModuleByName("Crosshair");
      if (crosshairModule != null && crosshairModule.isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
   private void onRenderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      Module hotbarModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
      if (hotbarModule != null && hotbarModule.isEnabled()) {
         ci.cancel();
      }
   }

   /** Hotbar, status bars, xp bar and held item tooltip are shifted together to make room for the logo bar. */
   @Inject(method = "extractHotbarAndDecorations", at = @At("HEAD"))
   private void onBeforeDecorations(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      float offset = LogoRenderer.getHotbarYOffset();
      this.astolfo$shifted = offset > 0.01F;
      if (this.astolfo$shifted) {
         context.pose().pushMatrix();
         context.pose().translate(0.0F, -offset);
      }
   }

   @Inject(method = "extractHotbarAndDecorations", at = @At("RETURN"))
   private void onAfterDecorations(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
      if (this.astolfo$shifted) {
         context.pose().popMatrix();
         this.astolfo$shifted = false;
      }
   }
}
