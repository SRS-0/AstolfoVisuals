package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class CircleEspManager {
    public static final long LIFESPAN = 450L;
    private final Map<Entity, CircleEspEffect> effects = new ConcurrentHashMap<Entity, CircleEspEffect>();

    public void addEffect(Entity target) {
        if (target == null || TargetUtils.isInvisible(target)) {
            return;
        }
        this.effects.compute(target, (k, existing) -> {
            if (existing == null) {
                return new CircleEspEffect(target);
            }
            existing.updateHit();
            return existing;
        });
    }

    public void tick() {
        long now = System.currentTimeMillis();
        this.effects.values().removeIf(effect -> now - effect.lastHitTime > 450L || !effect.target.isAlive() || TargetUtils.isInvisible(effect.target));
    }

    public Map<Entity, CircleEspEffect> getEffects() {
        return this.effects;
    }

    @Environment(value=EnvType.CLIENT)
    public static class CircleEspEffect {
        public final long startTime;
        public long lastHitTime;
        public final Entity target;

        public CircleEspEffect(Entity target) {
            this.target = target;
            this.lastHitTime = this.startTime = System.currentTimeMillis();
        }

        public void updateHit() {
            this.lastHitTime = System.currentTimeMillis();
        }
    }
}

