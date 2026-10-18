package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.module.modules.render.HitGlowModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
@Mixin(Player.class)
public class PlayerEntityMixin {
   @Inject(method = "attack", at = @At("HEAD"))
   private void onAttack(Entity target, CallbackInfo ci) {
      if ((Object)this == Minecraft.getInstance().player && target instanceof LivingEntity livingTarget) {
         if (!TargetUtils.isInvisible(livingTarget)) {
            TargetEspModule.addTargetAttack(target);
            if (AstolfoclientClient.targetHudManager != null) {
               AstolfoclientClient.targetHudManager.setTarget(livingTarget);
            }
         }

         HitGlowModule.addWave(target.position());
         if (AstolfoclientClient.particleManager != null) {
            AstolfoclientClient.particleManager.addEffects(target.getEyePosition());
         }
      }
   }
}
