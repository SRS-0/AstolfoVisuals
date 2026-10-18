package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display.TextDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(TextDisplay.class)
public class TextDisplayEntityMixin {
   @Inject(method = "getText", at = @At("RETURN"), cancellable = true)
   public void onGetText(CallbackInfoReturnable<Component> cir) {
      if (cir.getReturnValue() != null) {
         cir.setReturnValue(NameProtectModule.getProtectedText((Component)cir.getReturnValue()));
      }
   }
}
