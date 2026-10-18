package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class DiamondEspManager {
   public static final long LIFESPAN = 450L;
   private final Map<Entity, DiamondEspManager.DiamondEffect> effects = new ConcurrentHashMap<>();

   public void addEffect(Entity target) {
      if (target != null && !TargetUtils.isInvisible(target)) {
         this.effects.compute(target, (k, existing) -> {
            if (existing == null) {
               return new DiamondEspManager.DiamondEffect(target);
            }

            existing.updateHit();
            return (DiamondEspManager.DiamondEffect)existing;
         });
      }
   }

   public void addAttack(Entity target) {
      if (target != null && !TargetUtils.isInvisible(target)) {
         DiamondEspManager.DiamondEffect effect = this.effects.get(target);
         if (effect != null) {
            effect.lastAttackTime = System.currentTimeMillis();
         }
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();
      this.effects.values().removeIf(effect -> now - effect.lastHitTime > 450L || !effect.target.isAlive() || TargetUtils.isInvisible(effect.target));
   }

   public Map<Entity, DiamondEspManager.DiamondEffect> getEffects() {
      return this.effects;
   }

   @Environment(EnvType.CLIENT)
   public static class DiamondEffect {
      public final long startTime;
      public long lastHitTime;
      public long lastAttackTime;
      public final Entity target;

      public DiamondEffect(Entity target) {
         this.target = target;
         this.startTime = System.currentTimeMillis();
         this.lastHitTime = this.startTime;
         this.lastAttackTime = 0L;
      }

      public void updateHit() {
         this.lastHitTime = System.currentTimeMillis();
      }
   }
}
