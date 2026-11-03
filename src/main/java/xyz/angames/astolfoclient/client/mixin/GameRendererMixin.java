package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={GameRenderer.class})
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;
@Shadow
    public abstract float getFarPlaneDistance();

    @Inject(method={"getFov"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        CameraUtilsModule camUtils = CameraUtilsModule.getInstance();
        if (camUtils != null && camUtils.isEnabled()) {
            float baseFov = ((Float)cir.getReturnValue()).floatValue();
            cir.setReturnValue(Float.valueOf((float)camUtils.getAnimatedFov(baseFov)));
        }
    }

    @Inject(method={"getBasicProjectionMatrix"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetBasicProjectionMatrix(float fov, CallbackInfoReturnable<Matrix4f> cir) {
        AspectRatioModule aspectRatioModule = AspectRatioModule.getInstance();
        if (aspectRatioModule != null && aspectRatioModule.isEnabled()) {
            float defaultAspect = (float)this.client.getWindow().getFramebufferWidth() / (float)this.client.getWindow().getFramebufferHeight();
            float aspect = aspectRatioModule.getAspectRatio(defaultAspect);
            Matrix4f matrix4f = new Matrix4f();

            matrix4f.perspective(fov * ((float)Math.PI / 180), aspect, 0.05f, this.getFarPlaneDistance());
            cir.setReturnValue(matrix4f);
        }
    }
}

