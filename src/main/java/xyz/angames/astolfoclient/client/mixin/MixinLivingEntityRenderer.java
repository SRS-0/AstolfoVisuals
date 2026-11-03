package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.ModelsModule;
import xyz.angames.astolfoclient.client.render.CustomModelRenderer;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={LivingEntityRenderer.class})
public abstract class MixinLivingEntityRenderer {
    @Unique
    private CustomModelRenderer customModelRenderer;

    @Shadow
    public abstract EntityModel<?> getModel();

    @Unique
    private CustomModelRenderer getCustomModelRenderer() {
        if (this.customModelRenderer == null) {
            this.customModelRenderer = new CustomModelRenderer();
        }
        return this.customModelRenderer;
    }

    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderCustomModel(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, CameraRenderState cameraState, CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState)) {
            return;
        }
        PlayerEntityRenderState playerState = (PlayerEntityRenderState)state;
        if (!((Object)this instanceof PlayerEntityRenderer)) {
            return;
        }
        ModelsModule modelsModule = (ModelsModule)AstolfoclientClient.moduleManager.getModuleByName("Models");
        if (modelsModule == null || !modelsModule.isEnabled()) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String renderedName = playerState.playerName != null ? playerState.playerName.getString() : "";
        boolean isSelf = false;
        boolean isFriend = false;
        if (mc.player != null) {
            String myUsername = mc.getSession().getUsername();
            if (renderedName.toLowerCase().contains(myUsername.toLowerCase())) {
                isSelf = true;
            }
        }
        for (String friendName : FriendManager.getFriends()) {
            if (!renderedName.toLowerCase().contains(friendName.toLowerCase())) continue;
            isFriend = true;
            break;
        }
        boolean shouldRenderCustom = false;
        if (isSelf) {
            shouldRenderCustom = true;
        } else if (isFriend && modelsModule.friends.get()) {
            shouldRenderCustom = true;
        }
        if (!shouldRenderCustom) {
            return;
        }
        EntityModel<?> model = this.getModel();
        if (model instanceof PlayerEntityModel) {
            PlayerEntityModel playerModel = (PlayerEntityModel)model;
            playerModel.setAngles(playerState);
        }
        matrices.push();
        matrices.translate(0.0, (double)playerState.height / 2.0, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - playerState.bodyYaw));
        String mode = modelsModule.mode.get();
        if (mode.equals("Amogus") || mode.equals("Rabbit") || mode.equals("Cow")) {
            matrices.multiply(new Quaternionf().rotationX((float)Math.PI));
            if (modelsModule.changeZ.get()) {
                matrices.multiply(new Quaternionf().rotationY((float)Math.PI));
            }
        }
        matrices.translate(0.0, (double)(-playerState.height) / 2.0, 0.0);
        if (mode.equals("Rabbit")) {
            matrices.translate(0.0, -0.05, 0.0);
        } else if (mode.equals("Amogus")) {
            matrices.translate(0.0, -0.5, 0.0);
        } else if (mode.equals("Cow")) {
            matrices.translate(0.0, -0.2, 0.0);
        }
        float scale = 1.0f;
        if (mode.equals("Rabbit")) {
            scale = 1.25f;
        } else if (mode.equals("Amogus")) {
            scale = 1.8f;
        } else if (mode.equals("Cow")) {
            scale = 1.0f;
        }
        matrices.scale(scale, scale, scale);
        if (model instanceof PlayerEntityModel) {
            PlayerEntityModel playerModel = (PlayerEntityModel)model;
            this.getCustomModelRenderer().render(playerState, matrices, vertexConsumers, state.light, mode, playerModel);
        }
        matrices.pop();
        ci.cancel();
    }
}

