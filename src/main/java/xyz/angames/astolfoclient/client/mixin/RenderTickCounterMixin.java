package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.util.TimerManager;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class RenderTickCounterMixin {
   /** Scales the tick length, which is how the old timer multiplier sped up or slowed down the game. */
   @Inject(method = "getTickTargetMillis", at = @At("RETURN"), cancellable = true)
   private void onGetTickTargetMillis(float defaultTickTargetMillis, CallbackInfoReturnable<Float> cir) {
      if (AstolfoclientClient.getInstance() != null) {
         float multiplier = TimerManager.getTimer();
         if (multiplier > 0.0F) {
            cir.setReturnValue(cir.getReturnValue() / multiplier);
         }
      }
   }
}
