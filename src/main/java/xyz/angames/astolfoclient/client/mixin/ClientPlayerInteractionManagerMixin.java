package xyz.angames.astolfoclient.client.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.util.FakePlayerEntity;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerInteractionManagerMixin {
   @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
   public void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
      if (AstolfoclientClient.killEffectManager != null && AstolfoclientClient.moduleManager != null) {
         Module mod = AstolfoclientClient.moduleManager.getModuleByName("KillEffect");
         if (mod != null && mod.isEnabled()) {
            AstolfoclientClient.killEffectManager.onAttack(target);
         }
      }

      if (AstolfoclientClient.moduleManager != null) {
         Module ragdollModule = AstolfoclientClient.moduleManager.getModuleByName("Ragdoll");
         if (ragdollModule != null
            && ragdollModule.isEnabled()
            && ((RagdollModule)ragdollModule).hit.get()
            && target instanceof LivingEntity livingTarget
            && AstolfoclientClient.ragdollRenderer != null) {
            AstolfoclientClient.ragdollRenderer.addRagdoll(livingTarget);
         }
      }

      if (target instanceof FakePlayerEntity fake) {
         fake.handleEntityEvent((byte)2);
         player.resetAttackStrengthTicker();
         float cooldownProgress = player.getAttackStrengthScale(0.5F);
         boolean fullyCharged = cooldownProgress > 0.9F;
         boolean isCrit = fullyCharged
            && !player.onGround()
            && !player.onClimbable()
            && !player.isInWater()
            && !player.hasEffect(MobEffects.BLINDNESS)
            && !player.isPassenger();
         double baseDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
         if (baseDamage <= 1.0 && player.getMainHandItem() != null && !player.getMainHandItem().isEmpty()) {
            String name = player.getMainHandItem().getItem().toString().toLowerCase();
            if (name.contains("sword")) {
               if (name.contains("netherite")) {
                  baseDamage = 8.0;
               } else if (name.contains("diamond")) {
                  baseDamage = 7.0;
               } else if (name.contains("iron")) {
                  baseDamage = 6.0;
               } else if (name.contains("stone")) {
                  baseDamage = 5.0;
               } else {
                  baseDamage = 4.0;
               }
            } else if (name.contains("axe")) {
               if (!name.contains("netherite") && !name.contains("diamond") && !name.contains("iron") && !name.contains("stone")) {
                  baseDamage = 7.0;
               } else {
                  baseDamage = 9.0;
               }
            }
         }

         float damage = (float)(baseDamage * (0.2F + cooldownProgress * cooldownProgress * 0.8F));
         if (damage < 1.0F) {
            damage = 1.0F;
         }

         if (isCrit) {
            damage *= 1.5F;
         }

         int fireAspectLevel = 0;
         if (player.getMainHandItem() != null && !player.getMainHandItem().isEmpty()) {
            ItemEnchantments enchants = EnchantmentHelper.getEnchantmentsForCrafting(player.getMainHandItem());

            for (Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
               String id = ((Holder<Enchantment>)entry.getKey()).unwrapKey().map(k -> k.identifier().toString()).orElse("");
               if (id.contains("fire_aspect")) {
                  fireAspectLevel = entry.getIntValue();
                  break;
               }
            }
         }

         if (fireAspectLevel > 0) {
            fake.igniteForSeconds(fireAspectLevel * 4);
         }

         boolean isSword = player.getMainHandItem() != null && player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS);
         boolean isSweep = fullyCharged && !isCrit && player.onGround() && !player.isSprinting() && isSword;
         boolean isKnockback = fullyCharged && player.isSprinting();
         player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
         if (isCrit) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
            player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
         } else if (isSweep) {
            double d = -Math.sin(player.getYRot() * (float) (Math.PI / 180.0));
            double e = Math.cos(player.getYRot() * (float) (Math.PI / 180.0));
            if (player.level() instanceof ClientLevel) {
               player.level()
                  .addParticle(ParticleTypes.SWEEP_ATTACK, target.getX() + d, target.getY(0.5), target.getZ() + e, d, 0.0, e);
            }

            player.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            if (player.level() != null) {
               float sweepDamage = 1.0F + 0.5F * (float)baseDamage;

               for (Entity entity : player.level()
                  .getEntitiesOfClass(FakePlayerEntity.class, target.getBoundingBox().inflate(1.0, 0.25, 1.0), ent -> ent != target && ent != player)) {
                  entity.handleEntityEvent((byte)2);
                  player.playSound(SoundEvents.PLAYER_HURT, 1.0F, 1.0F);
                  float otherHealth = ((FakePlayerEntity)entity).getHealth() - sweepDamage;
                  if (otherHealth <= 0.0F) {
                     entity.handleEntityEvent((byte)35);
                     Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                     player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
                     ((FakePlayerEntity)entity).setHealth(20.0F);
                  } else {
                     ((FakePlayerEntity)entity).setHealth(otherHealth);
                  }
               }
            }
         } else if (isKnockback) {
            player.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 1.0F);
         } else if (fullyCharged) {
            player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
         } else {
            player.playSound(SoundEvents.PLAYER_ATTACK_WEAK, 1.0F, 1.0F);
         }

         if (player.getMainHandItem() != null && !player.getMainHandItem().isEmpty() && player.getMainHandItem().isEnchanted()) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
         }

         float newHealth = fake.getHealth() - damage;
         if (newHealth <= 0.0F) {
            fake.handleEntityEvent((byte)35);
            Minecraft.getInstance().particleEngine.createTrackingEmitter(fake, ParticleTypes.TOTEM_OF_UNDYING, 30);
            Minecraft.getInstance().player.displayItemActivation(new ItemStack(Items.TOTEM_OF_UNDYING));
            player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
            fake.setHealth(20.0F);
         } else {
            fake.setHealth(newHealth);
         }

         ci.cancel();
      }
   }

   @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
   public void onInteractEntityAtLocation(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      if (entity instanceof FakePlayerEntity) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }
}
