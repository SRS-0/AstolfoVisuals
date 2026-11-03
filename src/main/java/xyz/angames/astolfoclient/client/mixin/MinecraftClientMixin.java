package xyz.angames.astolfoclient.client.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
@Mixin(value={Minecraft.class})
public class MinecraftClientMixin {
    @Inject(method={"getResourcePackDirectory"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetResourcePackDir(CallbackInfoReturnable<Path> cir) {
    }

    @Inject(method={"startAttack"}, at={@At(value="HEAD")})
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        Minecraft client = (Minecraft)(Object)this;
        HitResult hitResult = client.hitResult;
        if (hitResult instanceof EntityHitResult) {
            EntityHitResult hitResult2 = (EntityHitResult)hitResult;
            Entity target = hitResult2.getEntity();
            Module hitEspModule = AstolfoclientClient.moduleManager.getModuleByName("HitESP");
            if (hitEspModule != null && hitEspModule.isEnabled() && client.player != null) {
                Camera camera = client.gameRenderer.mainCamera();
                Quaternionf orientation = new Quaternionf((Quaternionfc)camera.rotation());
                Vec3 viewVec = client.player.getViewVector(0.0f);
                Vec3 targetVec = target.position().subtract(client.player.position());
                float rotationDirection = (float)Math.signum(viewVec.cross((Vec3)targetVec).y);
                if (rotationDirection == 0.0f) {
                    rotationDirection = 1.0f;
                }
                Vec3 playerEyePos = client.player.getEyePosition();
                Vec3 hitPos = hitResult2.getLocation();
                Vec3 direction = hitPos.subtract(playerEyePos).normalize();
                Vec3 offset = direction.scale(0.3);
                Vec3 spawnPos = hitPos.subtract(offset);
                if (AstolfoclientClient.hitEspManager != null) {
                    AstolfoclientClient.hitEspManager.addEffect(spawnPos, rotationDirection, orientation);
                }
            }
        }
    }
}

