package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ProjectileEntity.class})
public class ProjectileEntityMixin {
    @Inject(method={"onEntityHit"}, at={@At(value="HEAD")})
    private void onEntityHit(EntityHitResult entityHitResult, CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity)(Object)this;
        if (self.getOwner() == MinecraftClient.getInstance().player) {
            Module mod;
            Entity target = entityHitResult.getEntity();
            if (AstolfoclientClient.killEffectManager != null && (mod = AstolfoclientClient.moduleManager.getModuleByName("KillEffect")) != null && mod.isEnabled()) {
                AstolfoclientClient.killEffectManager.onAttack(target);
            }
        }
    }
}

