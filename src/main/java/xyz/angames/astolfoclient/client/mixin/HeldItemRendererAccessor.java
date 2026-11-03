package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={HeldItemRenderer.class})
public interface HeldItemRendererAccessor {
    @Invoker(value="renderArmHoldingItem")
    public void invokeRenderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);
}

