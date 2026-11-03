package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;
@Mixin(Camera.class)
public abstract class GameRendererMixin {
 @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
 private void astolfo$animatedFov(float partialTick,CallbackInfoReturnable<Float> cir) {
  var module=CameraUtilsModule.getInstance();
  if(module!=null&&module.isEnabled())cir.setReturnValue((float)module.getAnimatedFov(cir.getReturnValue()));
 }
}
