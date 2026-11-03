package xyz.angames.astolfoclient.client.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.util.FakePlayerEntity;

@Environment(value=EnvType.CLIENT)
@Mixin(value={MultiPlayerGameMode.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method={"attack"}, at={@At(value="HEAD")}, cancellable=true)
    public void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        Module ragdollModule;
        Module mod;
        if (AstolfoclientClient.killEffectManager != null && AstolfoclientClient.moduleManager != null && (mod = AstolfoclientClient.moduleManager.getModuleByName("KillEffect")) != null && mod.isEnabled()) {
            AstolfoclientClient.killEffectManager.onAttack(target);
        }
        if (AstolfoclientClient.moduleManager != null && (ragdollModule = AstolfoclientClient.moduleManager.getModuleByName("Ragdoll")) != null && ragdollModule.isEnabled() && ((RagdollModule)ragdollModule).hit.get() && target instanceof LivingEntity) {
            LivingEntity livingTarget = (LivingEntity)target;
            if (AstolfoclientClient.ragdollRenderer != null) {
                AstolfoclientClient.ragdollRenderer.addRagdoll(livingTarget);
            }
        }
        if (target instanceof FakePlayerEntity) {
            float newHealth;
            float damage;
            FakePlayerEntity fake = (FakePlayerEntity)target;
            fake.handleEntityEvent((byte)2);
            player.resetOnlyAttackStrengthTicker();
            float cooldownProgress = player.getAttackStrengthScale(0.5f);
            boolean fullyCharged = cooldownProgress > 0.9f;
            boolean isCrit = fullyCharged && !player.onGround() && !player.onClimbable() && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS) && !player.isPassenger();
            double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            if (baseDamage <= 1.0 && player.getMainHandItem() != null && !player.getMainHandItem().isEmpty()) {
                String name = player.getMainHandItem().getItem().toString().toLowerCase();
                if (name.contains("sword")) {
                    baseDamage = name.contains("netherite") ? 8.0 : (name.contains("diamond") ? 7.0 : (name.contains("iron") ? 6.0 : (name.contains("stone") ? 5.0 : 4.0)));
                } else if (name.contains("axe")) {
                    baseDamage = name.contains("netherite") || name.contains("diamond") || name.contains("iron") || name.contains("stone") ? 9.0 : 7.0;
                }
            }
            if ((damage = (float)(baseDamage * (double)(0.2f + cooldownProgress * cooldownProgress * 0.8f))) < 1.0f) {
                damage = 1.0f;
            }
            if (isCrit) {
                damage *= 1.5f;
            }
            int fireAspectLevel = 0;
            if (player.getMainHandItem() != null && !player.getMainHandItem().isEmpty()) {
                ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting((ItemStack)player.getMainHandItem());
                for (Object2IntMap.Entry entry : enchants.entrySet()) {
                    String id = ((Holder<?>)entry.getKey()).unwrapKey().map(k -> k.identifier().toString()).orElse("");
                    if (!id.contains("fire_aspect")) continue;
                    fireAspectLevel = entry.getIntValue();
                    break;
                }
            }
            if (fireAspectLevel > 0) {
                fake.igniteForSeconds(fireAspectLevel * 4);
            }
            boolean isSword = player.getMainHandItem() != null && player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS);
            boolean isSweep = fullyCharged && !isCrit && player.onGround() && !player.isSprinting() && isSword;
            boolean isKnockback = fullyCharged && player.isSprinting();
            player.playSound(SoundEvents.PLAYER_HURT, 1.0f, 1.0f);
            if (isCrit) {
                Minecraft.getInstance().particleEngine.createTrackingEmitter(target, (ParticleOptions)ParticleTypes.CRIT);
                player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            } else if (isSweep) {
                double d = -Math.sin(player.getYRot() * ((float)Math.PI / 180));
                double e = Math.cos(player.getYRot() * ((float)Math.PI / 180));
                if (player.level() instanceof ClientLevel) {
                    ((ClientLevel)player.level()).addParticle((ParticleOptions)ParticleTypes.SWEEP_ATTACK, target.getX() + d, target.getY(0.5), target.getZ() + e, d, 0.0, e);
                }
                player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                if (player.level() != null) {
                    float sweepDamage = 1.0f + 0.5f * (float)baseDamage;
                    for (Entity entity : player.level().getEntitiesOfClass(FakePlayerEntity.class, target.getBoundingBox().inflate(1.0, 0.25, 1.0), ent -> ent != target && ent != player)) {
                        entity.handleEntityEvent((byte)2);
                        player.playSound(SoundEvents.PLAYER_HURT, 1.0f, 1.0f);
                        float otherHealth = ((FakePlayerEntity)entity).getHealth() - sweepDamage;
                        if (otherHealth <= 0.0f) {
                            entity.handleEntityEvent((byte)35);
                            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, (ParticleOptions)ParticleTypes.TOTEM_OF_UNDYING, 30);
                            player.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
                            ((FakePlayerEntity)entity).setHealth(20.0f);
                            continue;
                        }
                        ((FakePlayerEntity)entity).setHealth(otherHealth);
                    }
                }
            } else if (isKnockback) {
                player.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
            } else if (fullyCharged) {
                player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
            } else {
                player.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
            }
            if (player.getMainHandItem() != null && !player.getMainHandItem().isEmpty() && player.getMainHandItem().isEnchanted()) {
                Minecraft.getInstance().particleEngine.createTrackingEmitter(target, (ParticleOptions)ParticleTypes.ENCHANTED_HIT);
            }
            if ((newHealth = fake.getHealth() - damage) <= 0.0f) {
                fake.handleEntityEvent((byte)35);
                Minecraft.getInstance().particleEngine.createTrackingEmitter((Entity)fake, (ParticleOptions)ParticleTypes.TOTEM_OF_UNDYING, 30);
                Minecraft.getInstance().player.displayItemActivation(new ItemStack((ItemLike)Items.TOTEM_OF_UNDYING));
                player.playSound(SoundEvents.TOTEM_USE, 1.0f, 1.0f);
                fake.setHealth(20.0f);
            } else {
                fake.setHealth(newHealth);
            }
            ci.cancel();
        }
    }

    @Inject(method={"interact"}, at={@At(value="HEAD")}, cancellable=true)
    public void onInteractEntityAtLocation(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (entity instanceof FakePlayerEntity) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}

