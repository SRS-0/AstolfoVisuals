package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ExperienceBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
@Mixin(ExperienceBar.class) public class ExperienceBarMixin {
 @Inject(method={"extractBackground","extractRenderState"},at=@At("HEAD")) private void astolfo$push(GuiGraphicsExtractor context,DeltaTracker counter,CallbackInfo ci){context.pose().pushMatrix();context.pose().translate(0,-LogoRenderer.getHotbarYOffset());}
 @Inject(method={"extractBackground","extractRenderState"},at=@At("RETURN")) private void astolfo$pop(GuiGraphicsExtractor context,DeltaTracker counter,CallbackInfo ci){context.pose().popMatrix();}
}
