package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin {
    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"}, at={@At(value="HEAD")})
    private void shrinkToBabySize(LivingEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue vertexConsumerProvider, CameraRenderState cameraState, CallbackInfo ci) {
        if (state instanceof PlayerEntityRenderState) {
            Module babyMod;
            PlayerEntityRenderState playerState = (PlayerEntityRenderState)state;
            if (MinecraftClient.getInstance().player != null && playerState.id == MinecraftClient.getInstance().player.getId() && AstolfoclientClient.moduleManager != null && (babyMod = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer")) != null && babyMod.isEnabled()) {
                matrixStack.scale(0.5f, 0.5f, 0.5f);
            }
        }
    }
}

