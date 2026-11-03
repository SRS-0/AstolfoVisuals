package xyz.angames.astolfoclient.client.util;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Environment(value=EnvType.CLIENT)
public class TargetUtils {
    public static LivingEntity getLookedAtTarget(Minecraft client, double maxDistance) {
        Vec3 rot;
        Vec3 end;
        Vec3 start;
        BlockHitResult blockHit;
        if (client == null || client.player == null || client.level == null) {
            return null;
        }
        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) {
            cameraEntity = client.player;
        }
        double effectiveDist = (blockHit = client.level.clip(new ClipContext(start = cameraEntity.getEyePosition(1.0f), end = start.add((rot = cameraEntity.getViewVector(1.0f)).scale(maxDistance)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cameraEntity))) != null && blockHit.getType() != HitResult.Type.MISS ? start.distanceTo(blockHit.getLocation()) : maxDistance;
        AABB searchBox = cameraEntity.getBoundingBox().expandTowards(rot.scale(effectiveDist)).inflate(1.0, 1.0, 1.0);
        double closestDist = effectiveDist;
        LivingEntity closestEntity = null;
        for (Entity entity : client.level.getEntities(cameraEntity, searchBox, e -> e instanceof LivingEntity && !(e instanceof ArmorStand) && e.isAlive() && !e.isSpectator())) {
            double dist;
            if (TargetUtils.isInvisible(entity)) continue;
            float margin = entity.getPickRadius();
            AABB entityBox = entity.getBoundingBox().inflate(margin > 0.0f ? (double)margin : 0.1);
            Optional hit = entityBox.clip(start, end);
            if (!hit.isPresent() || !((dist = start.distanceTo((Vec3)hit.get())) < closestDist)) continue;
            closestDist = dist;
            closestEntity = (LivingEntity)entity;
        }
        return closestEntity;
    }

    public static boolean isInvisible(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.isInvisible()) {
            return true;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            return living.hasEffect(MobEffects.INVISIBILITY);
        }
        return false;
    }
}

