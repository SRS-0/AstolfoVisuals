package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(Objective.class)
public class ScoreboardObjectiveMixin {
   @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
   public void onGetDisplayName(CallbackInfoReturnable<Component> cir) {
      cir.setReturnValue(NameProtectModule.getProtectedText((Component)cir.getReturnValue()));
   }
}
