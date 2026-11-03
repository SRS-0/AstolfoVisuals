package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.render.HandPositionModule;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
import xyz.angames.astolfoclient.client.module.modules.render.SwingAnimationModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={HeldItemRenderer.class})
public abstract class HeldItemRendererMixin {
    @Inject(method={"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"}, at={@At(value="HEAD")})
    private void onRenderFirstPersonItemsHead(float tickDelta, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHand mod;
        SwingAnimationModule swingAnim = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (swingAnim != null) {
            swingAnim.updatePhysics(player, tickDelta);
        }
        if ((mod = ShaderHand.getInstance()) != null && mod.shouldRender()) {
            mod.beginRender();
            ShaderHand.rendering = true;
        }
    }

    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);

    @Inject(method={"renderFirstPersonItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, CallbackInfo ci) {
        boolean handEnabled;
        SwingAnimationModule swingMod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        HandPositionModule handMod = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
        boolean swingEnabled = swingMod != null && swingMod.isEnabled();
        boolean bl = handEnabled = handMod != null && handMod.isEnabled();
        if ((swingEnabled || handEnabled) && !item.isEmpty() && !(item.getItem() instanceof FilledMapItem)) {
            ci.cancel();
            if (swingMod != null) {
                swingMod.handleRenderItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
            }
        }
    }

    @Inject(method={"renderArmHoldingItem"}, at={@At(value="HEAD")})
    private void onRenderArmHoldingItemHead(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        if (!SwingAnimationModule.renderingCustomItem) {
            HandPositionModule handMod = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
            if (handMod != null && handMod.isEnabled()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                boolean isMainHand = mc.player != null && arm == mc.player.getMainArm();
                float[] pos = isMainHand ? handMod.getMainHandPos() : handMod.getOffHandPos();
                matrices.translate(pos[0], pos[1], pos[2]);
            }
        }
    }

    @Inject(method={"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"}, at={@At(value="TAIL")})
    private void onRenderFirstPersonItemsTail(float tickDelta, MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ShaderHand mod = ShaderHand.getInstance();
        if (mod != null && ShaderHand.rendering) {
            // Held items are queued in 1.21.11. Execute them while the hand target is active.
            MinecraftClient client = MinecraftClient.getInstance();
            try {
                ((GameRendererAccessor)client.gameRenderer).astolfo$getRenderDispatcher().render();
                client.getBufferBuilders().getEntityVertexConsumers().draw();
            } finally {
                mod.restoreTarget();
                ShaderHand.rendering = false;
            }
            ShaderHand.rendering = false;
            mod.draw();
        }
    }

    @Inject(method={"renderArmHoldingItem"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderRightArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V")})
    private void onRenderRightArm(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getRightX(), mod.getRightZ(), mod.getRightY());
        }
    }

    @Inject(method={"renderArmHoldingItem"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/PlayerEntityRenderer;renderLeftArm(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/util/Identifier;Z)V")})
    private void onRenderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getLeftX(), mod.getLeftZ(), mod.getLeftY());
        }
    }
}

