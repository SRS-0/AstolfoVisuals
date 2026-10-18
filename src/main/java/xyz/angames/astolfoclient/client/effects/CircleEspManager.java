package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class CircleEspManager {
   public static final long LIFESPAN = 450L;
   private final Map<Entity, CircleEspManager.CircleEspEffect> effects = new ConcurrentHashMap<>();

   public void addEffect(Entity target) {
      if (target != null && !TargetUtils.isInvisible(target)) {
         this.effects.compute(target, (k, existing) -> {
            if (existing == null) {
               return new CircleEspManager.CircleEspEffect(target);
            }

            existing.updateHit();
            return (CircleEspManager.CircleEspEffect)existing;
         });
      }
   }

   public void tick() {
      long now = System.currentTimeMillis();
      this.effects.values().removeIf(effect -> now - effect.lastHitTime > 450L || !effect.target.isAlive() || TargetUtils.isInvisible(effect.target));
   }

   public Map<Entity, CircleEspManager.CircleEspEffect> getEffects() {
      return this.effects;
   }

   @Environment(EnvType.CLIENT)
   public static class CircleEspEffect {
      public final long startTime;
      public long lastHitTime;
      public final Entity target;

      public CircleEspEffect(Entity target) {
         this.target = target;
         this.startTime = System.currentTimeMillis();
         this.lastHitTime = this.startTime;
      }

      public void updateHit() {
         this.lastHitTime = System.currentTimeMillis();
      }
   }
}
