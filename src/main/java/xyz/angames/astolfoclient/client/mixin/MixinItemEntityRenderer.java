package xyz.angames.astolfoclient.client.mixin;

import java.util.WeakHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
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

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void onUpdateRenderState(ItemEntity itemEntity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ENTITY_LINK.put(state, itemEntity);
    }

    @Inject(method={"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRender(ItemEntityRenderState state, MatrixStack matrixStack, OrderedRenderCommandQueue vertexConsumerProvider, net.minecraft.client.render.state.CameraRenderState cameraState, CallbackInfo ci) {
        ItemPhysicsModule physicsModule = (ItemPhysicsModule)AstolfoclientClient.moduleManager.getModuleByName("ItemPhysics");
        if (physicsModule == null || !physicsModule.isEnabled()) {
            return;
        }
        ItemEntity itemEntity = ENTITY_LINK.get(state);
        if (itemEntity == null) {
            return;
        }
        ItemStack itemStack = itemEntity.getStack();
        if (itemStack.isEmpty()) {
            return;
        }
        Item item = itemStack.getItem();
        boolean isBlock = item instanceof BlockItem;
        matrixStack.push();
        float customScale = (float)physicsModule.scale.get();
        matrixStack.scale(customScale, customScale, customScale);
        boolean isOnGround = itemEntity.isOnGround();
        float speed = (float)physicsModule.spinSpeed.get();
        float age = isOnGround ? (float)itemEntity.getItemAge() : ((float)itemEntity.getItemAge() + MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true)) * speed * 10.0f;
        matrixStack.translate(0.0f, 0.1f, 0.0f);
        if (isBlock) {
            matrixStack.translate(0.0f, -0.05f, 0.0f);
            if (!isOnGround) {
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age));
            } else {
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)itemEntity.getId() * 45.0f));
            }
        } else {
            matrixStack.translate(0.0f, -0.1f, 0.0f);
            if (!isOnGround) {
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(age));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(age));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(age));
            } else {
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)itemEntity.getId() * 73.0f));
            }
        }
        state.itemRenderState.render(matrixStack, vertexConsumerProvider, state.light, OverlayTexture.DEFAULT_UV, state.outlineColor);
        matrixStack.pop();
        ci.cancel();
    }
}

