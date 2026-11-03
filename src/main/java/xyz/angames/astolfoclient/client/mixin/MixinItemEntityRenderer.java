package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.ItemPhysicsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ItemEntityRenderer.class})
public abstract class MixinItemEntityRenderer {
    private static final WeakHashMap<ItemEntityRenderState, ItemEntity> ENTITY_LINK = new WeakHashMap();

    @Inject(method={"extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void onUpdateRenderState(ItemEntity itemEntity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ENTITY_LINK.put(state, itemEntity);
    }

    @Inject(method={"submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRender(ItemEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector vertexConsumerProvider, net.minecraft.client.renderer.state.level.CameraRenderState cameraState, CallbackInfo ci) {
        ItemPhysicsModule physicsModule = (ItemPhysicsModule)AstolfoclientClient.moduleManager.getModuleByName("ItemPhysics");
        if (physicsModule == null || !physicsModule.isEnabled()) {
            return;
        }
        ItemEntity itemEntity = ENTITY_LINK.get(state);
        if (itemEntity == null) {
            return;
        }
        ItemStack itemStack = itemEntity.getItem();
        if (itemStack.isEmpty()) {
            return;
        }
        Item item = itemStack.getItem();
        boolean isBlock = item instanceof BlockItem;
        matrixStack.pushPose();
        float customScale = (float)physicsModule.scale.get();
        matrixStack.scale(customScale, customScale, customScale);
        boolean isOnGround = itemEntity.onGround();
        float speed = (float)physicsModule.spinSpeed.get();
        float age = isOnGround ? (float)itemEntity.getAge() : ((float)itemEntity.getAge() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true)) * speed * 10.0f;
        matrixStack.translate(0.0f, 0.1f, 0.0f);
        if (isBlock) {
            matrixStack.translate(0.0f, -0.05f, 0.0f);
            if (!isOnGround) {
                matrixStack.rotate(Axis.XP.rotationDegrees(age));
                matrixStack.rotate(Axis.YP.rotationDegrees(age));
            } else {
                matrixStack.rotate(Axis.YP.rotationDegrees((float)itemEntity.getId() * 45.0f));
            }
        } else {
            matrixStack.translate(0.0f, -0.1f, 0.0f);
            if (!isOnGround) {
                matrixStack.rotate(Axis.XP.rotationDegrees(age));
                matrixStack.rotate(Axis.YP.rotationDegrees(age));
                matrixStack.rotate(Axis.ZP.rotationDegrees(age));
            } else {
                matrixStack.rotate(Axis.XP.rotationDegrees(90.0f));
                matrixStack.rotate(Axis.ZP.rotationDegrees((float)itemEntity.getId() * 73.0f));
            }
        }
        state.item.submit(matrixStack, vertexConsumerProvider, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        matrixStack.popPose();
        ci.cancel();
    }
}

