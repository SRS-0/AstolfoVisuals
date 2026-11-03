package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ParticleManager.class})
public class MinecraftParticleManagerMixin {
    @Unique
    private static long astolfoclient$lastTotemPopTime = 0L;

    @Inject(method={"addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddEmitter(Entity entity, ParticleEffect parameters, int maxAge, CallbackInfo ci) {
        if (parameters != null && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            ParticlesModule pm;
            Module mod;
            Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Particles") : null;
            if (mod instanceof ParticlesModule && (pm = (ParticlesModule)mod).isEnabled() && pm.totemPop.get()) {
                long now = System.currentTimeMillis();
                if (now - astolfoclient$lastTotemPopTime > 40L) {
                    astolfoclient$lastTotemPopTime = now;
                    if (AstolfoclientClient.particleManager != null && entity != null) {
                        Vec3d pos = entity.getEntityPos().add(0.0, (double)entity.getHeight() * 0.5, 0.0);
                        AstolfoclientClient.particleManager.addTotemPop(pos);
                    }
                }
                ci.cancel();
            }
        }
    }

    @Inject(method={"addEmitter(Lnet/minecraft/entity/Entity;Lnet/minecraft/particle/ParticleEffect;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddEmitterShort(Entity entity, ParticleEffect parameters, CallbackInfo ci) {
        if (parameters != null && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            ParticlesModule pm;
            Module mod;
            Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Particles") : null;
            if (mod instanceof ParticlesModule && (pm = (ParticlesModule)mod).isEnabled() && pm.totemPop.get()) {
                long now = System.currentTimeMillis();
                if (now - astolfoclient$lastTotemPopTime > 40L) {
                    astolfoclient$lastTotemPopTime = now;
                    if (AstolfoclientClient.particleManager != null && entity != null) {
                        Vec3d pos = entity.getEntityPos().add(0.0, (double)entity.getHeight() * 0.5, 0.0);
                        AstolfoclientClient.particleManager.addTotemPop(pos);
                    }
                }
                ci.cancel();
            }
        }
    }

    @Inject(method={"addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddParticle(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z) || Double.isNaN(velocityX) || Double.isNaN(velocityY) || Double.isNaN(velocityZ) || Double.isInfinite(velocityX) || Double.isInfinite(velocityY) || Double.isInfinite(velocityZ) || Math.abs(x) > 3.0E7 || Math.abs(y) > 3.0E7 || Math.abs(z) > 3.0E7) {
            cir.setReturnValue(null);
            return;
        }
        if (parameters != null && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            ParticlesModule pm;
            Module mod;
            Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Particles") : null;
            if (mod instanceof ParticlesModule && (pm = (ParticlesModule)mod).isEnabled() && pm.totemPop.get()) {
                cir.setReturnValue(null);
            }
        }
    }
}

