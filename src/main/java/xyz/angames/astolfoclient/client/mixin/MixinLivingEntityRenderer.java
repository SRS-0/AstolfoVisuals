package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.player.PlayerModel;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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

@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
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

   @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
   private void renderCustomModel(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector vertexConsumers, CameraRenderState camera, CallbackInfo ci) {
      int light = state.lightCoords;
      if (state instanceof AvatarRenderState playerState) {
         if ((Object)this instanceof AvatarRenderer) {
            ModelsModule modelsModule = (ModelsModule)AstolfoclientClient.moduleManager.getModuleByName("Models");
            if (modelsModule != null && modelsModule.isEnabled()) {
               Minecraft mc = Minecraft.getInstance();
               String renderedName = playerState.nameTag != null ? playerState.nameTag.getString() : "";
               boolean isSelf = false;
               boolean isFriend = false;
               if (mc.player != null) {
                  String myUsername = mc.getUser().getName();
                  if (playerState.id == mc.player.getId() || renderedName.toLowerCase().contains(myUsername.toLowerCase())) {
                     isSelf = true;
                  }
               }

               for (String friendName : FriendManager.getFriends()) {
                  if (renderedName.toLowerCase().contains(friendName.toLowerCase())) {
                     isFriend = true;
                     break;
                  }
               }

               boolean shouldRenderCustom = false;
               if (isSelf) {
                  shouldRenderCustom = true;
               } else if (isFriend && modelsModule.friends.get()) {
                  shouldRenderCustom = true;
               }

               if (shouldRenderCustom) {
                  EntityModel<?> model = this.getModel();
                  if (model instanceof PlayerModel playerModel) {
                     playerModel.setupAnim(playerState);
                  }

                  matrices.pushPose();
                  matrices.translate(0.0, playerState.boundingBoxHeight / 2.0, 0.0);
                  matrices.rotate(Axis.YP.rotationDegrees(180.0F - playerState.bodyRot));
                  String mode = modelsModule.mode.get();
                  if (mode.equals("Amogus") || mode.equals("Rabbit") || mode.equals("Cow")) {
                     matrices.rotate(new Quaternionf().rotationX((float) Math.PI));
                     if (modelsModule.changeZ.get()) {
                        matrices.rotate(new Quaternionf().rotationY((float) Math.PI));
                     }
                  }

                  matrices.translate(0.0, -playerState.boundingBoxHeight / 2.0, 0.0);
                  if (mode.equals("Rabbit")) {
                     matrices.translate(0.0, -0.05, 0.0);
                  } else if (mode.equals("Amogus")) {
                     matrices.translate(0.0, -0.5, 0.0);
                  } else if (mode.equals("Cow")) {
                     matrices.translate(0.0, -0.2, 0.0);
                  }

                  float scale = 1.0F;
                  if (mode.equals("Rabbit")) {
                     scale = 1.25F;
                  } else if (mode.equals("Amogus")) {
                     scale = 1.8F;
                  } else if (mode.equals("Cow")) {
                     scale = 1.0F;
                  }

                  matrices.scale(scale, scale, scale);
                  if (model instanceof PlayerModel playerModel) {
                     this.getCustomModelRenderer().render(playerState, matrices, vertexConsumers, light, mode, playerModel);
                  }

                  matrices.popPose();
                  ci.cancel();
               }
            }
         }
      }
   }
}
