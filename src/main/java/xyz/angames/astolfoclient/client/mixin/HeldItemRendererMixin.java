package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
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
@Mixin(value={ItemInHandRenderer.class})
public abstract class HeldItemRendererMixin {
    @Inject(method={"submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"}, at={@At(value="HEAD")})
    private void onRenderFirstPersonItemsHead(float tickDelta, PoseStack matrices, SubmitNodeCollector vertexConsumers, LocalPlayer player, int light, CallbackInfo ci) {
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
    protected abstract void renderPlayerArm(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6);

    @Inject(method={"submitArmWithItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderFirstPersonItem(AbstractClientPlayer player, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, CallbackInfo ci) {
        boolean handEnabled;
        SwingAnimationModule swingMod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        HandPositionModule handMod = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
        boolean swingEnabled = swingMod != null && swingMod.isEnabled();
        boolean bl = handEnabled = handMod != null && handMod.isEnabled();
        if ((swingEnabled || handEnabled) && !item.isEmpty() && !(item.getItem() instanceof MapItem)) {
            ci.cancel();
            if (swingMod != null) {
                swingMod.handleRenderItem(player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, vertexConsumers, light);
            }
        }
    }

    @Inject(method={"renderPlayerArm"}, at={@At(value="HEAD")})
    private void onRenderArmHoldingItemHead(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        if (!SwingAnimationModule.renderingCustomItem) {
            HandPositionModule handMod = (HandPositionModule)ModuleManager.getModule(HandPositionModule.class);
            if (handMod != null && handMod.isEnabled()) {
                Minecraft mc = Minecraft.getInstance();
                boolean isMainHand = mc.player != null && arm == mc.player.getMainArm();
                float[] pos = isMainHand ? handMod.getMainHandPos() : handMod.getOffHandPos();
                matrices.translate(pos[0], pos[1], pos[2]);
            }
        }
    }

    @Inject(method={"submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"}, at={@At(value="TAIL")})
    private void onRenderFirstPersonItemsTail(float tickDelta, PoseStack matrices, SubmitNodeCollector vertexConsumers, LocalPlayer player, int light, CallbackInfo ci) {
        ShaderHand mod = ShaderHand.getInstance();
        if (mod != null && ShaderHand.rendering) {
            // Held items are queued in 1.21.11. Execute them while the hand target is active.
            Minecraft client = Minecraft.getInstance();
            try {
                client.gameRenderer.featureRenderDispatcher().renderAllFeatures((net.minecraft.client.renderer.SubmitNodeStorage)vertexConsumers);
    
            } finally {
                mod.restoreTarget();
                ShaderHand.rendering = false;
            }
            ShaderHand.rendering = false;
            mod.draw();
        }
    }

    @Inject(method={"renderPlayerArm"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")})
    private void onRenderRightArm(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getRightX(), mod.getRightZ(), mod.getRightY());
        }
    }

    @Inject(method={"renderPlayerArm"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")})
    private void onRenderLeftArm(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getLeftX(), mod.getLeftZ(), mod.getLeftY());
        }
    }
}

