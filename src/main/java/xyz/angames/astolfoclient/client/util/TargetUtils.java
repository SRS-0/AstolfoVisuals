package xyz.angames.astolfoclient.client.util;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

@Environment(value=EnvType.CLIENT)
public class TargetUtils {
    public static LivingEntity getLookedAtTarget(MinecraftClient client, double maxDistance) {
        Vec3d rot;
        Vec3d end;
        Vec3d start;
        BlockHitResult blockHit;
        if (client == null || client.player == null || client.world == null) {
            return null;
        }
        Entity cameraEntity = client.getCameraEntity();
        if (cameraEntity == null) {
            cameraEntity = client.player;
        }
        double effectiveDist = (blockHit = client.world.raycast(new RaycastContext(start = cameraEntity.getCameraPosVec(1.0f), end = start.add((rot = cameraEntity.getRotationVec(1.0f)).multiply(maxDistance)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, cameraEntity))) != null && blockHit.getType() != HitResult.Type.MISS ? start.distanceTo(blockHit.getPos()) : maxDistance;
        Box searchBox = cameraEntity.getBoundingBox().stretch(rot.multiply(effectiveDist)).expand(1.0, 1.0, 1.0);
        double closestDist = effectiveDist;
        LivingEntity closestEntity = null;
        for (Entity entity : client.world.getOtherEntities(cameraEntity, searchBox, e -> e instanceof LivingEntity && !(e instanceof ArmorStandEntity) && e.isAlive() && !e.isSpectator())) {
            double dist;
            if (TargetUtils.isInvisible(entity)) continue;
            float margin = entity.getTargetingMargin();
            Box entityBox = entity.getBoundingBox().expand(margin > 0.0f ? (double)margin : 0.1);
            Optional hit = entityBox.raycast(start, end);
            if (!hit.isPresent() || !((dist = start.distanceTo((Vec3d)hit.get())) < closestDist)) continue;
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
            return living.hasStatusEffect(StatusEffects.INVISIBILITY);
        }
        return false;
    }
}

