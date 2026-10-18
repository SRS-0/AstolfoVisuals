package xyz.angames.astolfoclient.client.mixin;

import dev.sxmurxy.mre.utils.MreDraw;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws everything MRE queued while the GUI was being extracted, on top of the vanilla GUI. */
@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
   @Inject(method = "render", at = @At("TAIL"))
   private void astolfo$flushMre(CallbackInfo ci) {
      MreDraw.flush();
   }
}
