package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(PlayerTeam.class)
public class ScoreboardTeamMixin {
   @Inject(method = "getPlayerPrefix", at = @At("RETURN"), cancellable = true)
   public void onGetPrefix(CallbackInfoReturnable<Component> cir) {
      cir.setReturnValue(NameProtectModule.getProtectedText((Component)cir.getReturnValue()));
   }

   @Inject(method = "getPlayerSuffix", at = @At("RETURN"), cancellable = true)
   public void onGetSuffix(CallbackInfoReturnable<Component> cir) {
      cir.setReturnValue(NameProtectModule.getProtectedText((Component)cir.getReturnValue()));
   }

   @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
   private static void onDecorateName(net.minecraft.world.scores.Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
      Component protectedText = NameProtectModule.getProtectedText((Component)cir.getReturnValue());
      if (protectedText != null) {
         cir.setReturnValue(protectedText.copy());
      }
   }
}
