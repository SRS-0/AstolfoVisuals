package xyz.angames.astolfoclient.client.util;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.ClipContext.Block;

@Environment(EnvType.CLIENT)
public class TargetUtils {
   public static LivingEntity getLookedAtTarget(Minecraft client, double maxDistance) {
      if (client != null && client.player != null && client.level != null) {
         Entity cameraEntity = client.getCameraEntity();
         if (cameraEntity == null) {
            cameraEntity = client.player;
         }

         Vec3 start = cameraEntity.getEyePosition(1.0F);
         Vec3 rot = cameraEntity.getViewVector(1.0F);
         Vec3 end = start.add(rot.scale(maxDistance));
         BlockHitResult blockHit = client.level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, cameraEntity));
         double effectiveDist = blockHit != null && blockHit.getType() != Type.MISS ? start.distanceTo(blockHit.getLocation()) : maxDistance;
         AABB searchBox = cameraEntity.getBoundingBox().expandTowards(rot.scale(effectiveDist)).inflate(1.0, 1.0, 1.0);
         double closestDist = effectiveDist;
         LivingEntity closestEntity = null;

         for (Entity entity : client.level
            .getEntities(cameraEntity, searchBox, e -> e instanceof LivingEntity && !(e instanceof ArmorStand) && e.isAlive() && !e.isSpectator())) {
            if (!isInvisible(entity)) {
               float margin = entity.getPickRadius();
               AABB entityBox = entity.getBoundingBox().inflate(margin > 0.0F ? margin : 0.1);
               Optional<Vec3> hit = entityBox.clip(start, end);
               if (hit.isPresent()) {
                  double dist = start.distanceTo(hit.get());
                  if (dist < closestDist) {
                     closestDist = dist;
                     closestEntity = (LivingEntity)entity;
                  }
               }
            }
         }

         return closestEntity;
      } else {
         return null;
      }
   }

   public static boolean isInvisible(Entity entity) {
      if (entity == null) {
         return false;
      } else if (entity.isInvisible()) {
         return true;
      } else {
         return entity instanceof LivingEntity living ? living.hasEffect(MobEffects.INVISIBILITY) : false;
      }
   }
}
