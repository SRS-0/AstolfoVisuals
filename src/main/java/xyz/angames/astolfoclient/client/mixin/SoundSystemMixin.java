package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={SoundSystem.class})
public class SoundSystemMixin {
    @Shadow
    @Final
    private GameOptions options;

    @Inject(method={"getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetAdjustedVolume(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (sound != null && sound.getId() != null && "astolfoclient".equals(sound.getId().getNamespace())) {
            float mcMaster = this.options != null ? this.options.getSoundVolume(SoundCategory.MASTER) : 1.0f;
            float clientVol = sound.getVolume();
            if (mcMaster > 0.001f) {
                cir.setReturnValue(Float.valueOf(clientVol / mcMaster));
            } else {
                cir.setReturnValue(Float.valueOf(clientVol));
            }
        }
    }
}

