package xyz.angames.astolfoclient.client.mixin;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(EnvType.CLIENT)
@Mixin(EnvironmentAttributeSystem.class)
public class ClientWorldMixin {
   @Inject(
      method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;)Ljava/lang/Object;",
      at = @At("RETURN"),
      cancellable = true
   )
   private void onGetSkyColor(EnvironmentAttribute<?> attribute, Vec3 pos, SpatialAttributeInterpolator interpolator, CallbackInfoReturnable<Object> cir) {
      if (attribute == EnvironmentAttributes.SKY_COLOR && cir.getReturnValue() instanceof Integer originalColor && AstolfoclientClient.moduleManager != null) {
         AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
         if (ambients != null && ambients.isEnabled() && ambients.customSkybox.get()) {
            float strength = (float)ambients.skyboxStrength.get();
            if (strength > 0.0F) {
               Color themeColor = new Color(ThemeManager.getThemedColor(0L));
               int origR = originalColor >> 16 & 0xFF;
               int origG = originalColor >> 8 & 0xFF;
               int origB = originalColor & 0xFF;
               int finalR = (int)(origR * (1.0F - strength) + themeColor.getRed() * strength);
               int finalG = (int)(origG * (1.0F - strength) + themeColor.getGreen() * strength);
               int finalB = (int)(origB * (1.0F - strength) + themeColor.getBlue() * strength);
               cir.setReturnValue(originalColor & 0xFF000000 | finalR << 16 | finalG << 8 | finalB);
            }
         }
      }
   }
}
