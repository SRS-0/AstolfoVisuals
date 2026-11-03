package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={FirstPersonHandsAndItemsRenderer.class})
public interface HeldItemRendererAccessor {
    @Invoker(value="renderPlayerArm")
    void invokeRenderArmHoldingItem(PoseStack matrices, SubmitNodeCollector queue,int light,float equip,float swing,HumanoidArm arm,net.minecraft.client.renderer.state.level.PlayerRenderState state);
}
