package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;

@Environment(value=EnvType.CLIENT)
public class KillEffectManager {
    public static final long LIFESPAN = 3000L;
    private final List<KillEffect> effects = new CopyOnWriteArrayList<KillEffect>();
    private final Map<Integer, TrackedTarget> recentAttacks = new ConcurrentHashMap<Integer, TrackedTarget>();
    private final MinecraftClient client = MinecraftClient.getInstance();

    public void onAttack(Entity target) {
        if (target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)target;
            this.recentAttacks.put(target.getId(), new TrackedTarget(living));
        }
    }

    public void tick() {
        if (this.client.world == null || this.client.player == null) {
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
            Entity currentEntity = this.client.world.getEntityById(id);
            boolean isKilled = false;
            Vec3d deathPos = tracked.lastPos;
            Box deathBox = tracked.lastBox;
            if (currentEntity == null) {
                isKilled = true;
            } else if (currentEntity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity)currentEntity;
                Vec3d currentPos = living.getEntityPos();
                if (living.isDead() || living.getHealth() <= 0.0f || living.deathTime > 0 || !living.isAlive()) {
                    isKilled = true;
                    deathPos = currentPos;
                    deathBox = living.getBoundingBox();
                } else if (currentPos.squaredDistanceTo(tracked.lastPos) > 400.0) {
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
        public Vec3d lastPos;
        public Box lastBox;
        public final long lastHitTime;

        public TrackedTarget(LivingEntity entity) {
            this.lastPos = entity.getEntityPos();
            this.lastBox = entity.getBoundingBox();
            this.lastHitTime = System.currentTimeMillis();
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class KillEffect {
        public final Vec3d pos;
        public final String mode;
        public final long startTime;
        public final List<Vec3d> zapPoints;
        public final List<ThanosParticle> thanosParticles;

        public KillEffect(Vec3d pos, Box box, String mode, long startTime) {
            block3: {
                block2: {
                    this.zapPoints = new ArrayList<Vec3d>();
                    this.thanosParticles = new ArrayList<ThanosParticle>();
                    this.pos = pos;
                    this.mode = mode;
                    this.startTime = startTime;
                    if (!mode.equals("Zap")) break block2;
                    float currentX = 0.0f;
                    float currentZ = 0.0f;
                    this.zapPoints.add(new Vec3d(0.0, 0.0, 0.0));
                    float y = 1.0f + (float)Math.random() * 1.5f;
                    while (y <= 20.0f) {
                        currentX = (float)((double)currentX + (Math.random() - 0.5) * 3.5);
                        currentZ = (float)((double)currentZ + (Math.random() - 0.5) * 3.5);
                        this.zapPoints.add(new Vec3d((double)currentX, (double)y, (double)currentZ));
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

