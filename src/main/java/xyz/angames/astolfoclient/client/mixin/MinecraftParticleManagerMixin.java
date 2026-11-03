package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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
@Mixin(value={ParticleEngine.class})
public class MinecraftParticleManagerMixin {
    @Unique
    private static long astolfoclient$lastTotemPopTime = 0L;

    @Inject(method={"createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;I)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddEmitter(Entity entity, ParticleOptions parameters, int maxAge, CallbackInfo ci) {
        if (parameters != null && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            ParticlesModule pm;
            Module mod;
            Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Particles") : null;
            if (mod instanceof ParticlesModule && (pm = (ParticlesModule)mod).isEnabled() && pm.totemPop.get()) {
                long now = System.currentTimeMillis();
                if (now - astolfoclient$lastTotemPopTime > 40L) {
                    astolfoclient$lastTotemPopTime = now;
                    if (AstolfoclientClient.particleManager != null && entity != null) {
                        Vec3 pos = entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
                        AstolfoclientClient.particleManager.addTotemPop(pos);
                    }
                }
                ci.cancel();
            }
        }
    }

    @Inject(method={"createTrackingEmitter(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/particles/ParticleOptions;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddEmitterShort(Entity entity, ParticleOptions parameters, CallbackInfo ci) {
        if (parameters != null && parameters.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            ParticlesModule pm;
            Module mod;
            Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Particles") : null;
            if (mod instanceof ParticlesModule && (pm = (ParticlesModule)mod).isEnabled() && pm.totemPop.get()) {
                long now = System.currentTimeMillis();
                if (now - astolfoclient$lastTotemPopTime > 40L) {
                    astolfoclient$lastTotemPopTime = now;
                    if (AstolfoclientClient.particleManager != null && entity != null) {
                        Vec3 pos = entity.position().add(0.0, (double)entity.getBbHeight() * 0.5, 0.0);
                        AstolfoclientClient.particleManager.addTotemPop(pos);
                    }
                }
                ci.cancel();
            }
        }
    }

    @Inject(method={"createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAddParticle(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
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

