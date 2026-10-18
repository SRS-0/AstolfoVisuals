package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientClockManager.ClientClockInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;

@Environment(EnvType.CLIENT)
@Mixin(ClientClockInstance.class)
public class ClientWorldPropertiesMixin {
   @Inject(method = "totalTicks", at = @At("HEAD"), cancellable = true)
   private void onTotalTicks(CallbackInfoReturnable<Long> cir) {
      if (AstolfoclientClient.moduleManager != null) {
         AmbientsModule ambients = (AmbientsModule)AstolfoclientClient.moduleManager.getModuleByName("Ambients");
         if (ambients != null && ambients.isEnabled() && ambients.customTime.get()) {
            cir.setReturnValue((long)ambients.time.get());
         }
      }
   }
}
