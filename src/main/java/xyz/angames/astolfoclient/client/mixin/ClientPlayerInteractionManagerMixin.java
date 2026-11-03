package xyz.angames.astolfoclient.client.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
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
@Mixin(value={ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method={"attackEntity"}, at={@At(value="HEAD")}, cancellable=true)
    public void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
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
            fake.handleStatus((byte)2);
            player.resetTicksSinceLastAttack();
            float cooldownProgress = player.getAttackCooldownProgress(0.5f);
            boolean fullyCharged = cooldownProgress > 0.9f;
            boolean isCrit = fullyCharged && !player.isOnGround() && !player.isClimbing() && !player.isTouchingWater() && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasVehicle();
            double baseDamage = player.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
            if (baseDamage <= 1.0 && player.getMainHandStack() != null && !player.getMainHandStack().isEmpty()) {
                String name = player.getMainHandStack().getItem().toString().toLowerCase();
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
            if (player.getMainHandStack() != null && !player.getMainHandStack().isEmpty()) {
                ItemEnchantmentsComponent enchants = EnchantmentHelper.getEnchantments((ItemStack)player.getMainHandStack());
                for (Object2IntMap.Entry entry : enchants.getEnchantmentEntries()) {
                    String id = ((RegistryEntry<?>)entry.getKey()).getKey().map(k -> k.getValue().toString()).orElse("");
                    if (!id.contains("fire_aspect")) continue;
                    fireAspectLevel = entry.getIntValue();
                    break;
                }
            }
            if (fireAspectLevel > 0) {
                fake.setOnFireFor(fireAspectLevel * 4);
            }
            boolean isSword = player.getMainHandStack() != null && player.getMainHandStack().isIn(net.minecraft.registry.tag.ItemTags.SWORDS);
            boolean isSweep = fullyCharged && !isCrit && player.isOnGround() && !player.isSprinting() && isSword;
            boolean isKnockback = fullyCharged && player.isSprinting();
            player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            if (isCrit) {
                MinecraftClient.getInstance().particleManager.addEmitter(target, (ParticleEffect)ParticleTypes.CRIT);
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            } else if (isSweep) {
                double d = -Math.sin(player.getYaw() * ((float)Math.PI / 180));
                double e = Math.cos(player.getYaw() * ((float)Math.PI / 180));
                if (player.getEntityWorld() instanceof ClientWorld) {
                    ((ClientWorld)player.getEntityWorld()).addParticleClient((ParticleEffect)ParticleTypes.SWEEP_ATTACK, target.getX() + d, target.getBodyY(0.5), target.getZ() + e, d, 0.0, e);
                }
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.0f);
                if (player.getEntityWorld() != null) {
                    float sweepDamage = 1.0f + 0.5f * (float)baseDamage;
                    for (Entity entity : player.getEntityWorld().getEntitiesByClass(FakePlayerEntity.class, target.getBoundingBox().expand(1.0, 0.25, 1.0), ent -> ent != target && ent != player)) {
                        entity.handleStatus((byte)2);
                        player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                        float otherHealth = ((FakePlayerEntity)entity).getHealth() - sweepDamage;
                        if (otherHealth <= 0.0f) {
                            entity.handleStatus((byte)35);
                            MinecraftClient.getInstance().particleManager.addEmitter(entity, (ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, 30);
                            player.playSound(SoundEvents.ITEM_TOTEM_USE, 1.0f, 1.0f);
                            ((FakePlayerEntity)entity).setHealth(20.0f);
                            continue;
                        }
                        ((FakePlayerEntity)entity).setHealth(otherHealth);
                    }
                }
            } else if (isKnockback) {
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 1.0f);
            } else if (fullyCharged) {
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 1.0f);
            } else {
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 1.0f, 1.0f);
            }
            if (player.getMainHandStack() != null && !player.getMainHandStack().isEmpty() && player.getMainHandStack().hasEnchantments()) {
                MinecraftClient.getInstance().particleManager.addEmitter(target, (ParticleEffect)ParticleTypes.ENCHANTED_HIT);
            }
            if ((newHealth = fake.getHealth() - damage) <= 0.0f) {
                fake.handleStatus((byte)35);
                MinecraftClient.getInstance().particleManager.addEmitter((Entity)fake, (ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, 30);
                MinecraftClient.getInstance().gameRenderer.showFloatingItem(new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING));
                player.playSound(SoundEvents.ITEM_TOTEM_USE, 1.0f, 1.0f);
                fake.setHealth(20.0f);
            } else {
                fake.setHealth(newHealth);
            }
            ci.cancel();
        }
    }

    @Inject(method={"interactEntity"}, at={@At(value="HEAD")}, cancellable=true)
    public void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (entity instanceof FakePlayerEntity) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }

    @Inject(method={"interactEntityAtLocation"}, at={@At(value="HEAD")}, cancellable=true)
    public void onInteractEntityAtLocation(PlayerEntity player, Entity entity, EntityHitResult hitResult, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (entity instanceof FakePlayerEntity) {
            cir.setReturnValue(ActionResult.PASS);
        }
    }
}

