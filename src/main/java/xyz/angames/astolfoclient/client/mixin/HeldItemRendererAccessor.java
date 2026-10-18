package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public interface HeldItemRendererAccessor {
   @Invoker("renderPlayerArm")
   void invokeRenderArmHoldingItem(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6, net.minecraft.client.renderer.state.level.PlayerRenderState var7);
}
