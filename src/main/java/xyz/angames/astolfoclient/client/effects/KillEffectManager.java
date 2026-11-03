package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;

@Environment(value=EnvType.CLIENT)
public class KillEffectManager {
    public static final long LIFESPAN = 3000L;
    private final List<KillEffect> effects = new CopyOnWriteArrayList<KillEffect>();
    private final Map<Integer, TrackedTarget> recentAttacks = new ConcurrentHashMap<Integer, TrackedTarget>();
    private final Minecraft client = Minecraft.getInstance();

    public void onAttack(Entity target) {
        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)target;
            this.recentAttacks.put(target.getId(), new TrackedTarget(living));
        }
    }

    public void tick() {
        if (this.client.level == null || this.client.player == null) {
            this.effects.clear();
            this.recentAttacks.clear();
            return;
        }
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, TrackedTarget> entry : this.recentAttacks.entrySet()) {
            int id = entry.getKey();
            TrackedTarget tracked = entry.getValue();
            if (now - tracked.lastHitTime > 10000L) {
                this.recentAttacks.remove(id);
                continue;
            }
            Entity currentEntity = this.client.level.getEntity(id);
            boolean isKilled = false;
            Vec3 deathPos = tracked.lastPos;
            AABB deathBox = tracked.lastBox;
            if (currentEntity == null) {
                isKilled = true;
            } else if (currentEntity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)currentEntity;
                Vec3 currentPos = living.position();
                if (living.isDeadOrDying() || living.getHealth() <= 0.0f || living.deathTime > 0 || !living.isAlive()) {
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
            if (!isKilled) continue;
            KillEffectModule mod = (KillEffectModule)AstolfoclientClient.moduleManager.getModuleByName("KillEffect");
            String mode = mod != null ? mod.mode.get() : "Zap";
            this.effects.add(new KillEffect(deathPos, deathBox, mode, now));
            this.recentAttacks.remove(id);
        }
        this.effects.removeIf(e -> now - e.startTime > 3000L);
    }

    public List<KillEffect> getEffects() {
        return this.effects;
    }

    @Environment(value=EnvType.CLIENT)
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

    @Environment(value=EnvType.CLIENT)
    public static class KillEffect {
        public final Vec3 pos;
        public final String mode;
        public final long startTime;
        public final List<Vec3> zapPoints;
        public final List<ThanosParticle> thanosParticles;

        public KillEffect(Vec3 pos, AABB box, String mode, long startTime) {
            block3: {
                block2: {
                    this.zapPoints = new ArrayList<Vec3>();
                    this.thanosParticles = new ArrayList<ThanosParticle>();
                    this.pos = pos;
                    this.mode = mode;
                    this.startTime = startTime;
                    if (!mode.equals("Zap")) break block2;
                    float currentX = 0.0f;
                    float currentZ = 0.0f;
                    this.zapPoints.add(new Vec3(0.0, 0.0, 0.0));
                    float y = 1.0f + (float)Math.random() * 1.5f;
                    while (y <= 20.0f) {
                        currentX = (float)((double)currentX + (Math.random() - 0.5) * 3.5);
                        currentZ = (float)((double)currentZ + (Math.random() - 0.5) * 3.5);
                        this.zapPoints.add(new Vec3((double)currentX, (double)y, (double)currentZ));
                        y = (float)((double)y + (1.0 + Math.random() * 1.5));
                    }
                    break block3;
                }
                if (!mode.equals("Thanos")) break block3;
                float width = box != null ? (float)(box.maxX - box.minX) : 0.6f;
                float height = box != null ? (float)(box.maxY - box.minY) : 1.8f;
                for (int i = 0; i < 500; ++i) {
                    ThanosParticle p = new ThanosParticle();
                    p.startX = (float)((Math.random() - 0.5) * (double)width);
                    p.startY = (float)(Math.random() * (double)height);
                    p.startZ = (float)((Math.random() - 0.5) * (double)width);
                    p.fallSpeed = (float)(0.001 + Math.random() * 0.003);
                    p.delay = (float)(Math.random() * 1200.0);
                    this.thanosParticles.add(p);
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ThanosParticle {
        public float startX;
        public float startY;
        public float startZ;
        public float fallSpeed;
        public float delay;
    }
}

