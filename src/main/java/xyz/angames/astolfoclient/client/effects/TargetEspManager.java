package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class TargetEspManager {
   public static final long LIFESPAN = 450L;
   private final Map<Entity, TargetEspEffect> effects = new ConcurrentHashMap<>();

   public void addEffect(Entity target) {
      if (target != null && !TargetUtils.isInvisible(target)) {
         this.effects.computeIfAbsent(target, TargetEspEffect::new).registerHit();
      }
   }

   public void tick() {
      this.effects
         .values()
         .removeIf(
            effect -> System.currentTimeMillis() - effect.lastHitTime > 450L
               || effect.target.isRemoved()
               || !effect.target.isAlive()
               || TargetUtils.isInvisible(effect.target)
         );
   }

   public Map<Entity, TargetEspEffect> getEffects() {
      return this.effects;
   }
}
