package xyz.angames.astolfoclient.client.effects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;

@Environment(EnvType.CLIENT)
public class HitEspManager {
   private final List<HitEspEffect> effects = new CopyOnWriteArrayList<>();
   private final Minecraft mc = Minecraft.getInstance();

   public void addEffect(Vec3 position, float rotationDirection, Quaternionf orientation) {
      this.effects.add(new HitEspEffect(position, rotationDirection, orientation));
   }

   public void tick() {
      long currentTime = System.currentTimeMillis();
      HitEspModule module = (HitEspModule)AstolfoclientClient.moduleManager.getModuleByName("HitESP");
      if (module != null) {
         double leaveTimeVal = module.leaveTime.get();
         boolean doExplosionVal = module.doExplosion.get();
         double fadeTimeVal = module.fadeTime.get();
         double gravityVal = module.gravity.get();
         double frictionVal = module.friction.get();
         double groundLifespanVal = module.groundLifespan.get();
         double spinSpeedVal = module.spinSpeed.get();
         boolean bounceVal = module.bounce.get();
         double bounceFactorVal = module.bounceFactor.get();
         this.effects
            .removeIf(
               effect -> {
                  long age = currentTime - effect.creationTime;
                  if (!doExplosionVal) {
                     return age >= leaveTimeVal + fadeTimeVal;
                  }

                  if (age >= leaveTimeVal && !effect.isShattered) {
                     this.shatterEffect(effect, module);
                  }

                  if (!effect.isShattered) {
                     return false;
                  }

                  boolean allDead = true;

                  for (HitEspEffect.Shard shard : effect.shards) {
                     shard.prevPos = shard.pos;
                     shard.prevRotX = shard.rotX;
                     shard.prevRotY = shard.rotY;
                     shard.prevRotZ = shard.rotZ;
                     if (!shard.onGround) {
                        shard.vy -= gravityVal;
                        shard.vx *= frictionVal;
                        shard.vz *= frictionVal;
                        if (this.mc.level != null) {
                           double nextX = shard.pos.x + shard.vx;
                           double nextY = shard.pos.y + shard.vy;
                           double nextZ = shard.pos.z + shard.vz;
                           boolean collideX = !this.mc
                              .level
                              .getBlockState(BlockPos.containing(new Vec3(nextX, shard.pos.y, shard.pos.z)))
                              .getCollisionShape(this.mc.level, BlockPos.containing(new Vec3(nextX, shard.pos.y, shard.pos.z)))
                              .isEmpty();
                           boolean collideY = !this.mc
                              .level
                              .getBlockState(BlockPos.containing(new Vec3(shard.pos.x, nextY, shard.pos.z)))
                              .getCollisionShape(this.mc.level, BlockPos.containing(new Vec3(shard.pos.x, nextY, shard.pos.z)))
                              .isEmpty();
                           boolean collideZ = !this.mc
                              .level
                              .getBlockState(BlockPos.containing(new Vec3(shard.pos.x, shard.pos.y, nextZ)))
                              .getCollisionShape(this.mc.level, BlockPos.containing(new Vec3(shard.pos.x, shard.pos.y, nextZ)))
                              .isEmpty();
                           if (bounceVal) {
                              if (collideX) {
                                 shard.vx = -shard.vx * bounceFactorVal;
                              }

                              if (collideY) {
                                 if (Math.abs(shard.vy) < 0.05 && shard.vy < 0.0) {
                                    shard.onGround = true;
                                    shard.groundHitTime = currentTime;
                                 } else {
                                    shard.vy = -shard.vy * bounceFactorVal;
                                 }
                              }

                              if (collideZ) {
                                 shard.vz = -shard.vz * bounceFactorVal;
                              }

                              if (!shard.onGround) {
                                 shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                                 shard.rotX = (float)(shard.rotX + shard.rotSpeedX * spinSpeedVal);
                                 shard.rotY = (float)(shard.rotY + shard.rotSpeedY * spinSpeedVal);
                                 shard.rotZ = (float)(shard.rotZ + shard.rotSpeedZ * spinSpeedVal);
                              }
                           } else if (!collideX && !collideY && !collideZ) {
                              shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                              shard.rotX = (float)(shard.rotX + shard.rotSpeedX * spinSpeedVal);
                              shard.rotY = (float)(shard.rotY + shard.rotSpeedY * spinSpeedVal);
                              shard.rotZ = (float)(shard.rotZ + shard.rotSpeedZ * spinSpeedVal);
                           } else {
                              shard.onGround = true;
                              shard.groundHitTime = currentTime;
                           }
                        } else {
                           shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                           shard.rotX = (float)(shard.rotX + shard.rotSpeedX * spinSpeedVal);
                           shard.rotY = (float)(shard.rotY + shard.rotSpeedY * spinSpeedVal);
                           shard.rotZ = (float)(shard.rotZ + shard.rotSpeedZ * spinSpeedVal);
                        }
                     }

                     if (!shard.onGround || currentTime - shard.groundHitTime < groundLifespanVal) {
                        allDead = false;
                     }
                  }

                  return allDead;
               }
            );
      }
   }

   private void shatterEffect(HitEspEffect effect, HitEspModule module) {
      effect.isShattered = true;
      int gridSize = (int)module.gridSize.get();
      if (gridSize < 1) {
         gridSize = 1;
      }

      float step = 1.0F / gridSize;
      double explosionStrength = module.explosionStrength.get();
      double explosionRadius = module.explosionRadius.get();

      for (int x = 0; x < gridSize; x++) {
         for (int y = 0; y < gridSize; y++) {
            float u1 = x * step;
            float v1 = y * step;
            float u2 = u1 + step;
            float v2 = v1 + step;
            float localX = (x + 0.5F) * step - 0.5F;
            float localY = (y + 0.5F) * step - 0.5F;
            Vector3f localOffset = new Vector3f(localX, localY, 0.0F);
            effect.orientation.transform(localOffset);
            Vec3 startPos = effect.position.add(localOffset.x * explosionRadius, localOffset.y * explosionRadius, localOffset.z * explosionRadius);
            double vx = (localOffset.x * 0.5 + (Math.random() - 0.5) * 0.3) * explosionStrength;
            double vy = (localOffset.y * 0.5 + Math.random() * 0.4 + 0.2) * explosionStrength;
            double vz = (localOffset.z * 0.5 + (Math.random() - 0.5) * 0.3) * explosionStrength;
            effect.shards.add(new HitEspEffect.Shard(startPos, vx, vy, vz, u1, v1, u2, v2));
         }
      }
   }

   public List<HitEspEffect> getEffects() {
      return this.effects;
   }
}
