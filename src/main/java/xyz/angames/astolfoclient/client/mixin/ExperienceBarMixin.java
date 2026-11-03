package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
@Mixin(ExperienceBar.class) public class ExperienceBarMixin {
 @Inject(method={"renderBar","renderAddons"},at=@At("HEAD")) private void astolfo$push(DrawContext context,RenderTickCounter counter,CallbackInfo ci){context.getMatrices().pushMatrix();context.getMatrices().translate(0,-LogoRenderer.getHotbarYOffset());}
 @Inject(method={"renderBar","renderAddons"},at=@At("RETURN")) private void astolfo$pop(DrawContext context,RenderTickCounter counter,CallbackInfo ci){context.getMatrices().popMatrix();}
}
