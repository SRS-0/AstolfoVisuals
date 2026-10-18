package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Projection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;

/** FOV and aspect ratio now live on the camera in 26.x. */
@Environment(EnvType.CLIENT)
@Mixin(Camera.class)
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Projection projection;

   @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
   private void onGetFov(float partialTicks, CallbackInfoReturnable<Float> cir) {
      CameraUtilsModule camUtils = CameraUtilsModule.getInstance();
      if (camUtils != null && camUtils.isEnabled()) {
         float baseFov = cir.getReturnValue();
         cir.setReturnValue((float)camUtils.getAnimatedFov(baseFov));
      }
   }

   @Inject(method = "setupPerspective", at = @At("HEAD"), cancellable = true)
   private void onSetupPerspective(float zNear, float zFar, float fov, float width, float height, CallbackInfo ci) {
      AspectRatioModule aspectRatioModule = AspectRatioModule.getInstance();
      if (aspectRatioModule != null && aspectRatioModule.isEnabled()) {
         float aspect = aspectRatioModule.getAspectRatio(width / height);
         this.projection.setupPerspective(zNear, zFar, fov, height * aspect, height);
         ci.cancel();
      }
   }
}
