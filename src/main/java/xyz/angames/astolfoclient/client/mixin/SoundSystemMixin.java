package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.Options;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(SoundEngine.class)
public class SoundSystemMixin {
   @Shadow
   @Final
   private Options options;

   @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("HEAD"), cancellable = true)
   private void onGetAdjustedVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
      if (sound != null && sound.getIdentifier() != null && "astolfoclient".equals(sound.getIdentifier().getNamespace())) {
         float mcMaster = this.options != null ? this.options.getFinalSoundSourceVolume(SoundSource.MASTER) : 1.0F;
         float clientVol = sound.getVolume();
         if (mcMaster > 0.001F) {
            cir.setReturnValue(clientVol / mcMaster);
         } else {
            cir.setReturnValue(clientVol);
         }
      }
   }
}
