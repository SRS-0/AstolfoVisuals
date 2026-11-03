package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Projectile.class})
public class ProjectileEntityMixin {
    @Inject(method={"onHitEntity"}, at={@At(value="HEAD")})
    private void onEntityHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        Projectile self = (Projectile)(Object)this;
        if (self.getOwner() == Minecraft.getInstance().player) {
            Module mod;
            Entity target = entityHitResult.getEntity();
            if (AstolfoclientClient.killEffectManager != null && (mod = AstolfoclientClient.moduleManager.getModuleByName("KillEffect")) != null && mod.isEnabled()) {
                AstolfoclientClient.killEffectManager.onAttack(target);
            }
        }
    }
}

