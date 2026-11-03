package xyz.angames.astolfoclient.client.mixin;

import dev.sxmurxy.mre.renderers.GuiContext;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GuiExtractionMixin {
    @Inject(method="extract", at=@At("HEAD"))
    private void astolfo$beginInputFrame(CallbackInfo ci) {
        var client = net.minecraft.client.Minecraft.getInstance();
        xyz.angames.astolfoclient.client.util.HudMouseInput.beginFrame(client.gui.screen(), client.isWindowActive());
    }

    @Inject(method="extract", at=@At("RETURN"))
    private void astolfo$endExtraction(CallbackInfo ci) { GuiContext.endExtraction(); }
}
