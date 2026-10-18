package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityMixin {
   @Inject(method = "getCustomName", at = @At("RETURN"), cancellable = true)
   private void onGetCustomName(CallbackInfoReturnable<Component> cir) {
      Component original = (Component)cir.getReturnValue();
      if (original != null) {
         Component protectedText = NameProtectModule.getProtectedText(original);
         if (protectedText != null && protectedText != original) {
            cir.setReturnValue(protectedText);
         }
      }
   }
}
