package xyz.angames.astolfoclient.client.mixin;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
   @Inject(method = "startAttack", at = @At("HEAD"))
   private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
      if (AstolfoclientClient.moduleManager != null) {
         Minecraft client = (Minecraft)(Object)this;
         if (client.hitResult instanceof EntityHitResult hitResult) {
            Entity target = hitResult.getEntity();
            Module hitEspModule = AstolfoclientClient.moduleManager.getModuleByName("HitESP");
            if (hitEspModule != null && hitEspModule.isEnabled() && client.player != null) {
               Camera camera = client.gameRenderer.mainCamera();
               Quaternionf orientation = new Quaternionf(camera.rotation());
               Vec3 viewVec = client.player.getViewVector(0.0F);
               Vec3 targetVec = target.position().subtract(client.player.position());
               float rotationDirection = (float)Math.signum(viewVec.cross(targetVec).y);
               if (rotationDirection == 0.0F) {
                  rotationDirection = 1.0F;
               }

               Vec3 playerEyePos = client.player.getEyePosition();
               Vec3 hitPos = hitResult.getLocation();
               Vec3 direction = hitPos.subtract(playerEyePos).normalize();
               Vec3 offset = direction.scale(0.3);
               Vec3 spawnPos = hitPos.subtract(offset);
               if (AstolfoclientClient.hitEspManager != null) {
                  AstolfoclientClient.hitEspManager.addEffect(spawnPos, rotationDirection, orientation);
               }
            }
         }
      }
   }
}
