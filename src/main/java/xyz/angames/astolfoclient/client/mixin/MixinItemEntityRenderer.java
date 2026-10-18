package xyz.angames.astolfoclient.client.mixin;

import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.ItemPhysicsModule;

@Environment(EnvType.CLIENT)
@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {
   private static final WeakHashMap<ItemEntityRenderState, ItemEntity> ENTITY_LINK = new WeakHashMap<>();

   @Inject(method = "extractRenderState", at = @At("TAIL"))
   private void onUpdateRenderState(ItemEntity itemEntity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
      ENTITY_LINK.put(state, itemEntity);
   }

   @Inject(
      method = "submit",
      at = @At("HEAD"),
      cancellable = true
   )
   private void onRender(ItemEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      int light = state.lightCoords;
      ItemPhysicsModule physicsModule = (ItemPhysicsModule)AstolfoclientClient.moduleManager.getModuleByName("ItemPhysics");
      if (physicsModule != null && physicsModule.isEnabled()) {
         ItemEntity itemEntity = ENTITY_LINK.get(state);
         if (itemEntity != null) {
            ItemStack itemStack = itemEntity.getItem();
            if (!itemStack.isEmpty()) {
               Item item = itemStack.getItem();
               boolean isBlock = item instanceof BlockItem;
               matrixStack.pushPose();
               float customScale = (float)physicsModule.scale.get();
               matrixStack.scale(customScale, customScale, customScale);
               boolean isOnGround = itemEntity.onGround();
               float speed = (float)physicsModule.spinSpeed.get();
               float age = isOnGround
                  ? itemEntity.getAge()
                  : (itemEntity.getAge() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) * speed * 10.0F;
               matrixStack.translate(0.0F, 0.1F, 0.0F);
               if (isBlock) {
                  matrixStack.translate(0.0F, -0.05F, 0.0F);
                  if (!isOnGround) {
                     matrixStack.rotate(Axis.XP.rotationDegrees(age));
                     matrixStack.rotate(Axis.YP.rotationDegrees(age));
                  } else {
                     matrixStack.rotate(Axis.YP.rotationDegrees(itemEntity.getId() * 45.0F));
                  }
               } else {
                  matrixStack.translate(0.0F, -0.1F, 0.0F);
                  if (!isOnGround) {
                     matrixStack.rotate(Axis.XP.rotationDegrees(age));
                     matrixStack.rotate(Axis.YP.rotationDegrees(age));
                     matrixStack.rotate(Axis.ZP.rotationDegrees(age));
                  } else {
                     matrixStack.rotate(Axis.XP.rotationDegrees(90.0F));
                     matrixStack.rotate(Axis.ZP.rotationDegrees(itemEntity.getId() * 73.0F));
                  }
               }

               state.item.submit(matrixStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
               matrixStack.popPose();
               ci.cancel();
            }
         }
      }
   }
}
