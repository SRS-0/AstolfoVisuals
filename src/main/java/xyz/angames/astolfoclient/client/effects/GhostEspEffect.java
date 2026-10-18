package xyz.angames.astolfoclient.client.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class GhostEspEffect {
   public final Entity target;
   public long lastHitTime;
   public long lastAttackTime = 0L;
   public float currentAngle = 0.0F;
   public long lastRenderTime = 0L;

   public GhostEspEffect(Entity target) {
      this.target = target;
      this.lastHitTime = System.currentTimeMillis();
   }
}
