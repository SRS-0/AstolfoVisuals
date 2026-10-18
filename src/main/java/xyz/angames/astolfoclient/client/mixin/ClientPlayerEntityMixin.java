package xyz.angames.astolfoclient.client.mixin;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
   public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "aiStep", at = @At("HEAD"))
   public void onTickMovement(CallbackInfo ci) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      NoRenderModule noRender = NoRenderModule.getInstance();
      if (noRender != null && noRender.isEnabled() && noRender.blindness.get()) {
         boolean actuallyBlind = false;

         for (MobEffectInstance effect : player.getActiveEffects()) {
            if (effect.getEffect().equals(MobEffects.BLINDNESS)) {
               actuallyBlind = true;
               break;
            }
         }

         if (actuallyBlind) {
            player.setSprinting(false);
            Minecraft.getInstance().options.keySprint.setDown(false);
         }
      }
   }
}
