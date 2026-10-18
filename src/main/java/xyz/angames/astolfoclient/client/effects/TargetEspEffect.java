package xyz.angames.astolfoclient.client.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;

@Environment(EnvType.CLIENT)
public class TargetEspEffect {
   public long lastHitTime;
   public long startTime;
   public final Entity target;
   public float currentAngle = 0.0F;
   public long lastRenderTime = 0L;

   public TargetEspEffect(Entity target) {
      this.target = target;
      this.startTime = System.currentTimeMillis();
      this.registerHit();
   }

   public void registerHit() {
      this.lastHitTime = System.currentTimeMillis();
   }
}
