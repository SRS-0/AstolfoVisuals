package xyz.angames.astolfoclient.client.mixin;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
@Mixin(GameRenderer.class)
public abstract class HandRenderTargetMixin {
 @Inject(method="mainRenderTarget",at=@At("HEAD"),cancellable=true)
 private void astolfo$handTarget(CallbackInfoReturnable<RenderTarget> cir) {
  var module=ShaderHand.getInstance();
  if(ShaderHand.rendering&&module!=null)cir.setReturnValue(module.getHandsBuffer());
 }
}
