package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
   @Inject(method = "submit", at = @At("HEAD"))
   private void shrinkToBabySize(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      if (state instanceof AvatarRenderState playerState
         && Minecraft.getInstance().player != null
         && playerState.id == Minecraft.getInstance().player.getId()
         && AstolfoclientClient.moduleManager != null) {
         Module babyMod = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer");
         if (babyMod != null && babyMod.isEnabled()) {
            matrixStack.scale(0.5F, 0.5F, 0.5F);
         }
      }
   }
}
