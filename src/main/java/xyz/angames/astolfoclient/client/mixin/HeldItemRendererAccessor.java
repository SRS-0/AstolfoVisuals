package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ItemInHandRenderer.class})
public interface HeldItemRendererAccessor {
    @Invoker(value="renderPlayerArm")
    public void invokeRenderArmHoldingItem(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6);
}

