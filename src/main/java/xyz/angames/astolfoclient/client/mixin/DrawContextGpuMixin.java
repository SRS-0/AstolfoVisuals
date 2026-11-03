package xyz.angames.astolfoclient.client.mixin;
import dev.sxmurxy.mre.renderers.GuiContext;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DrawContext.class)
public class DrawContextGpuMixin {
    @Inject(method="<init>",at=@At("TAIL")) private void astolfo$context(CallbackInfo ci){GuiContext.begin((DrawContext)(Object)this);}
    @Inject(method="enableScissor",at=@At("TAIL")) private void astolfo$clip(int x1,int y1,int x2,int y2,CallbackInfo ci){GuiContext.pushClip((DrawContext)(Object)this,x1,y1,x2,y2);}
    @Inject(method="disableScissor",at=@At("TAIL")) private void astolfo$unclip(CallbackInfo ci){GuiContext.popClip((DrawContext)(Object)this);}
}
