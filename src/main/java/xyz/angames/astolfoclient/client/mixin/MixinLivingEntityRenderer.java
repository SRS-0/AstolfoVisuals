package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
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

    @Inject(method={"submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void renderCustomModel(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector vertexConsumers, CameraRenderState cameraState, CallbackInfo ci) {
        if (!(state instanceof AvatarRenderState)) {
            return;
        }
        AvatarRenderState playerState = (AvatarRenderState)state;
        if (!((Object)this instanceof AvatarRenderer)) {
            return;
        }
        ModelsModule modelsModule = (ModelsModule)AstolfoclientClient.moduleManager.getModuleByName("Models");
        if (modelsModule == null || !modelsModule.isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String renderedName = playerState.scoreText != null ? playerState.scoreText.getString() : "";
        boolean isSelf = false;
        boolean isFriend = false;
        if (mc.player != null) {
            String myUsername = mc.getUser().getName();
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
        if (model instanceof PlayerModel) {
            PlayerModel playerModel = (PlayerModel)model;
            playerModel.setupAnim(playerState);
        }
        matrices.pushPose();
        matrices.translate(0.0, (double)playerState.boundingBoxHeight / 2.0, 0.0);
        matrices.rotate(Axis.YP.rotationDegrees(180.0f - playerState.bodyRot));
        String mode = modelsModule.mode.get();
        if (mode.equals("Amogus") || mode.equals("Rabbit") || mode.equals("Cow")) {
            matrices.rotate(new Quaternionf().rotationX((float)Math.PI));
            if (modelsModule.changeZ.get()) {
                matrices.rotate(new Quaternionf().rotationY((float)Math.PI));
            }
        }
        matrices.translate(0.0, (double)(-playerState.boundingBoxHeight) / 2.0, 0.0);
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
        if (model instanceof PlayerModel) {
            PlayerModel playerModel = (PlayerModel)model;
            this.getCustomModelRenderer().render(playerState, matrices, vertexConsumers, state.lightCoords, mode, playerModel);
        }
        matrices.popPose();
        ci.cancel();
    }
}

