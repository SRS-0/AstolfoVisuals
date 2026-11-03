package xyz.angames.astolfoclient.client.mixin;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MinecraftClient.class})
public class MinecraftClientMixin {
    @Inject(method={"getResourcePackDir"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetResourcePackDir(CallbackInfoReturnable<Path> cir) {
    }

    @Inject(method={"getFramebuffer"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
        ShaderHand mod;
        if (ShaderHand.rendering && (mod = ShaderHand.getInstance()) != null && mod.getHandsBuffer() != null) {
            cir.setReturnValue(mod.getHandsBuffer());
        }
    }

    @Inject(method={"doAttack"}, at={@At(value="HEAD")})
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        MinecraftClient client = (MinecraftClient)(Object)this;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult instanceof EntityHitResult) {
            EntityHitResult hitResult2 = (EntityHitResult)hitResult;
            Entity target = hitResult2.getEntity();
            Module hitEspModule = AstolfoclientClient.moduleManager.getModuleByName("HitESP");
            if (hitEspModule != null && hitEspModule.isEnabled() && client.player != null) {
                Camera camera = client.gameRenderer.getCamera();
                Quaternionf orientation = new Quaternionf((Quaternionfc)camera.getRotation());
                Vec3d viewVec = client.player.getRotationVec(0.0f);
                Vec3d targetVec = target.getEntityPos().subtract(client.player.getEntityPos());
                float rotationDirection = (float)Math.signum(viewVec.crossProduct((Vec3d)targetVec).y);
                if (rotationDirection == 0.0f) {
                    rotationDirection = 1.0f;
                }
                Vec3d playerEyePos = client.player.getEyePos();
                Vec3d hitPos = hitResult2.getPos();
                Vec3d direction = hitPos.subtract(playerEyePos).normalize();
                Vec3d offset = direction.multiply(0.3);
                Vec3d spawnPos = hitPos.subtract(offset);
                if (AstolfoclientClient.hitEspManager != null) {
                    AstolfoclientClient.hitEspManager.addEffect(spawnPos, rotationDirection, orientation);
                }
            }
        }
    }
}

