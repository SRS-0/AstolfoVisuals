package xyz.angames.astolfoclient.client.mixin;

import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(EnvType.CLIENT)
@Mixin(FogRenderer.class)
public class RenderSystemMixin {
   @Inject(method = "setupFog", at = @At("RETURN"))
   private void modifyFog(Camera camera, int renderDistanceInChunks, DeltaTracker deltaTracker, float darkenWorldAmount, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
      if (AstolfoclientClient.moduleManager == null) {
         return;
      }

      AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
      if (ambients != null && ambients.isEnabled() && ambients.customFog.get()) {
         FogData fog = cir.getReturnValue();
         fog.environmentalStart = (float)ambients.fogStart.get();
         fog.environmentalEnd = (float)ambients.fogEnd.get();
         fog.renderDistanceStart = Math.min(fog.renderDistanceStart, fog.environmentalEnd);
         if (ambients.fogColorEnabled.get() && ambients.themeSync.get()) {
            float strength = (float)ambients.fogStrength.get();
            if (strength > 0.0F) {
               Color themeColor = new Color(ThemeManager.getThemedColor(0L));
               fog.color.x = fog.color.x * (1.0F - strength) + themeColor.getRed() / 255.0F * strength;
               fog.color.y = fog.color.y * (1.0F - strength) + themeColor.getGreen() / 255.0F * strength;
               fog.color.z = fog.color.z * (1.0F - strength) + themeColor.getBlue() / 255.0F * strength;
            }
         }
      }
   }
}
