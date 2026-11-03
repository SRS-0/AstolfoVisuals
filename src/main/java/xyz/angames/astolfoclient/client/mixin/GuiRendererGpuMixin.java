package xyz.angames.astolfoclient.client.mixin;
import dev.sxmurxy.mre.renderers.GuiContext;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiRenderer.class)
public class GuiRendererGpuMixin {
    @Inject(method="render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",at=@At("HEAD")) private void astolfo$prepare(CallbackInfo ci){GuiContext.finish();}
}
