package xyz.angames.astolfoclient.client.mixin;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import com.mojang.renderpearl.api.device.GpuBackend;
import net.minecraft.client.PreferredGraphicsApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The mod issues raw GL calls and custom GLSL programs, so the Vulkan backend must never be selected. */
@Mixin(PreferredGraphicsApi.class)
public class PreferredGraphicsApiMixin {
   @Inject(method = "getBackendsToTry", at = @At("HEAD"), cancellable = true)
   private void astolfo$forceOpenGl(CallbackInfoReturnable<GpuBackend[]> cir) {
      cir.setReturnValue(new GpuBackend[]{new GlBackend()});
   }
}
