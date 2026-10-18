package xyz.angames.astolfoclient.client.mixin;

import dev.sxmurxy.mre.utils.MreDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets MRE draws, which are queued and replayed later, honour the GUI scissor active when they were submitted. */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
   @Shadow
   @Final
   public GuiGraphicsExtractor.ScissorStack scissorStack;

   @Inject(method = {"enableScissor", "disableScissor"}, at = @At("TAIL"))
   private void astolfo$trackScissor(CallbackInfo ci) {
      MreDraw.setScissor(this.scissorStack.peek());
   }
}
