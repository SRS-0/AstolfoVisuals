package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;

@Environment(EnvType.CLIENT)
public class KillEffectManager {
   public static final long LIFESPAN = 3000L;
   private final List<KillEffectManager.KillEffect> effects = new CopyOnWriteArrayList<>();
   private final Map<Integer, KillEffectManager.TrackedTarget> recentAttacks = new ConcurrentHashMap<>();
   private final Minecraft client = Minecraft.getInstance();

   public void onAttack(Entity target) {
      if (target instanceof LivingEntity living) {
         this.recentAttacks.put(target.getId(), new KillEffectManager.TrackedTarget(living));
      }
   }

   public void tick() {
      if (this.client.level != null && this.client.player != null) {
         long now = System.currentTimeMillis();

         for (Entry<Integer, KillEffectManager.TrackedTarget> entry : this.recentAttacks.entrySet()) {
            int id = entry.getKey();
            KillEffectManager.TrackedTarget tracked = entry.getValue();
            if (now - tracked.lastHitTime > 10000L) {
               this.recentAttacks.remove(id);
            } else {
               Entity currentEntity = this.client.level.getEntity(id);
               boolean isKilled = false;
               Vec3 deathPos = tracked.lastPos;
               AABB deathBox = tracked.lastBox;
               if (currentEntity == null) {
                  isKilled = true;
               } else if (currentEntity instanceof LivingEntity living) {
                  Vec3 currentPos = living.position();
                  if (living.isDeadOrDying() || living.getHealth() <= 0.0F || living.deathTime > 0 || !living.isAlive()) {
                     isKilled = true;
                     deathPos = currentPos;
                     deathBox = living.getBoundingBox();
                  } else if (currentPos.distanceToSqr(tracked.lastPos) > 400.0) {
                     isKilled = true;
                  }

                  if (!isKilled) {
                     tracked.lastPos = currentPos;
                     tracked.lastBox = living.getBoundingBox();
                  }
               }

               if (isKilled) {
                  KillEffectModule mod = (KillEffectModule)AstolfoclientClient.moduleManager.getModuleByName("KillEffect");
                  String mode = mod != null ? mod.mode.get() : "Zap";
                  this.effects.add(new KillEffectManager.KillEffect(deathPos, deathBox, mode, now));
                  this.recentAttacks.remove(id);
               }
            }
         }

         this.effects.removeIf(e -> now - e.startTime > 3000L);
      } else {
         this.effects.clear();
         this.recentAttacks.clear();
      }
   }

   public List<KillEffectManager.KillEffect> getEffects() {
      return this.effects;
   }

   @Environment(EnvType.CLIENT)
   public static class KillEffect {
      public final Vec3 pos;
      public final String mode;
      public final long startTime;
      public final List<Vec3> zapPoints = new ArrayList<>();
      public final List<KillEffectManager.ThanosParticle> thanosParticles = new ArrayList<>();

      public KillEffect(Vec3 pos, AABB box, String mode, long startTime) {
         this.pos = pos;
         this.mode = mode;
         this.startTime = startTime;
         if (mode.equals("Zap")) {
            float currentX = 0.0F;
            float currentZ = 0.0F;
            this.zapPoints.add(new Vec3(0.0, 0.0, 0.0));

            for (float y = 1.0F + (float)Math.random() * 1.5F; y <= 20.0F; y = (float)(y + (1.0 + Math.random() * 1.5))) {
               currentX = (float)(currentX + (Math.random() - 0.5) * 3.5);
               currentZ = (float)(currentZ + (Math.random() - 0.5) * 3.5);
               this.zapPoints.add(new Vec3(currentX, y, currentZ));
            }
         } else if (mode.equals("Thanos")) {
            float width = box != null ? (float)(box.maxX - box.minX) : 0.6F;
            float height = box != null ? (float)(box.maxY - box.minY) : 1.8F;

            for (int i = 0; i < 500; i++) {
               KillEffectManager.ThanosParticle p = new KillEffectManager.ThanosParticle();
               p.startX = (float)((Math.random() - 0.5) * width);
               p.startY = (float)(Math.random() * height);
               p.startZ = (float)((Math.random() - 0.5) * width);
               p.fallSpeed = (float)(0.001 + Math.random() * 0.003);
               p.delay = (float)(Math.random() * 1200.0);
               this.thanosParticles.add(p);
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static class ThanosParticle {
      public float startX;
      public float startY;
      public float startZ;
      public float fallSpeed;
      public float delay;
   }

   @Environment(EnvType.CLIENT)
   private static class TrackedTarget {
      public Vec3 lastPos;
      public AABB lastBox;
      public final long lastHitTime;

      public TrackedTarget(LivingEntity entity) {
         this.lastPos = entity.position();
         this.lastBox = entity.getBoundingBox();
         this.lastHitTime = System.currentTimeMillis();
      }
   }
}
