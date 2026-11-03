package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin {
    @Inject(method={"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"}, at={@At(value="HEAD")})
    private void shrinkToBabySize(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector vertexConsumerProvider, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof AvatarRenderState) {
            Module babyMod;
            AvatarRenderState playerState = (AvatarRenderState)state;
            if (Minecraft.getInstance().player != null && playerState.id == Minecraft.getInstance().player.getId() && AstolfoclientClient.moduleManager != null && (babyMod = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer")) != null && babyMod.isEnabled()) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }
}

