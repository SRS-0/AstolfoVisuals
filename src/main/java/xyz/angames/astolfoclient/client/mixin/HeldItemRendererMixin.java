package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
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
@Mixin(value={FirstPersonHandsAndItemsRenderer.class})
public abstract class HeldItemRendererMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"))
    private void astolfo$physics(CallbackInfo ci) {var player=Minecraft.getInstance().player;var mod=(SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);if(mod!=null&&player!=null)mod.updatePhysics(player,Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));}

    @Inject(method={"submitArmWithItem"}, at={@At(value="HEAD")}, cancellable=true)
    private void onRenderFirstPersonItem(net.minecraft.client.renderer.state.level.PlayerRenderState playerState, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState handState, float tickDelta, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, CallbackInfo ci) {
        var player=Minecraft.getInstance().player;
        if(player==null)return;
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
    private void onRenderArmHoldingItemHead(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, float equipProgress, float swingProgress, HumanoidArm arm, net.minecraft.client.renderer.state.level.PlayerRenderState playerState, CallbackInfo ci) {
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

    @Inject(method={"renderPlayerHand"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")})
    private void onRenderRightArm(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, HumanoidArm arm, net.minecraft.client.renderer.state.level.PlayerRenderState playerState, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getRightX(), mod.getRightZ(), mod.getRightY());
        }
    }

    @Inject(method={"renderPlayerHand"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V")})
    private void onRenderLeftArm(PoseStack matrices, SubmitNodeCollector vertexConsumers, int light, HumanoidArm arm, net.minecraft.client.renderer.state.level.PlayerRenderState playerState, CallbackInfo ci) {
        SwingAnimationModule mod = (SwingAnimationModule)ModuleManager.getModule(SwingAnimationModule.class);
        if (mod != null && mod.isHoldMyItemsEnabled()) {
            matrices.translate(mod.getLeftX(), mod.getLeftZ(), mod.getLeftY());
        }
    }
}

