package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.module.modules.render.SwingAnimationModule;

@Environment(value=EnvType.CLIENT)
@Mixin(value={LivingEntity.class})
public class LivingEntityMixin {
    @Inject(method={"jumpFromGround"}, at={@At(value="HEAD")})
    private void onJump(CallbackInfo ci) {
        Module jumpCircleModule;
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity.equals((Object)Minecraft.getInstance().player) && AstolfoclientClient.moduleManager != null && (jumpCircleModule = AstolfoclientClient.moduleManager.getModuleByName("JumpCircle")) != null && jumpCircleModule.isEnabled() && AstolfoclientClient.jumpCircleManager != null) {
            AstolfoclientClient.jumpCircleManager.addCircle(entity.getX(), entity.getY(), entity.getZ());
        }
    }

    @Inject(method={"hasEffect"}, at={@At(value="HEAD")}, cancellable=true)
    private void onHasStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        NoRenderModule noRender;
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity == Minecraft.getInstance().player && (noRender = NoRenderModule.getInstance()) != null && noRender.isEnabled() && noRender.blindness.get() && (effect.equals((Object)MobEffects.BLINDNESS) || effect.equals((Object)MobEffects.DARKNESS))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method={"getEffect"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetStatusEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        NoRenderModule noRender;
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity == Minecraft.getInstance().player && (noRender = NoRenderModule.getInstance()) != null && noRender.isEnabled() && noRender.blindness.get() && (effect.equals((Object)MobEffects.BLINDNESS) || effect.equals((Object)MobEffects.DARKNESS))) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method={"handleEntityEvent(B)V"}, at={@At(value="HEAD")})
    private void onHandleStatus(byte status, CallbackInfo ci) {
        Module ragdollModule;
        LivingEntity entity = (LivingEntity)(Object)this;
        if ((status == 35 || status == 3) && AstolfoclientClient.moduleManager != null && (ragdollModule = AstolfoclientClient.moduleManager.getModuleByName("Ragdoll")) != null && ragdollModule.isEnabled()) {
            boolean isDeath;
            boolean isTotem = status == 35 && ((RagdollModule)ragdollModule).totemPop.get();
            boolean bl = isDeath = status == 3 && ((RagdollModule)ragdollModule).death.get();
            if ((isTotem || isDeath) && AstolfoclientClient.ragdollRenderer != null) {
                AstolfoclientClient.ragdollRenderer.addRagdoll(entity);
            }
        }
    }

    @Inject(method={"getArrowCount"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetStuckArrowCount(CallbackInfoReturnable<Integer> cir) {
        if ((Integer)cir.getReturnValue() > 50) {
            cir.setReturnValue(50);
        }
    }

    @Inject(method={"getModifiedSwingDuration"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        SwingAnimationModule swing;
        LivingEntity entity = (LivingEntity)(Object)this;
        if (entity == Minecraft.getInstance().player && AstolfoclientClient.moduleManager != null && (swing = (SwingAnimationModule)AstolfoclientClient.moduleManager.getModuleByName("SwingAnimation")) != null && swing.isEnabled() && swing.slow.get()) {
            cir.setReturnValue(((Number)swing.speed.getValue()).intValue());
        }
    }
}

