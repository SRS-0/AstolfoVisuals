package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class DiamondEspManager {
    public static final long LIFESPAN = 450L;
    private final Map<Entity, DiamondEffect> effects = new ConcurrentHashMap<Entity, DiamondEffect>();

    public void addEffect(Entity target) {
        if (target == null || TargetUtils.isInvisible(target)) {
            return;
        }
        this.effects.compute(target, (k, existing) -> {
            if (existing == null) {
                return new DiamondEffect(target);
            }
            existing.updateHit();
            return existing;
        });
    }

    public void addAttack(Entity target) {
        if (target == null || TargetUtils.isInvisible(target)) {
            return;
        }
        DiamondEffect effect = this.effects.get(target);
        if (effect != null) {
            effect.lastAttackTime = System.currentTimeMillis();
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        this.effects.values().removeIf(effect -> now - effect.lastHitTime > 450L || !effect.target.isAlive() || TargetUtils.isInvisible(effect.target));
    }

    public Map<Entity, DiamondEffect> getEffects() {
        return this.effects;
    }

    @Environment(value=EnvType.CLIENT)
    public static class DiamondEffect {
        public final long startTime;
        public long lastHitTime;
        public long lastAttackTime;
        public final Entity target;

        public DiamondEffect(Entity target) {
            this.target = target;
            this.lastHitTime = this.startTime = System.currentTimeMillis();
            this.lastAttackTime = 0L;
        }

        public void updateHit() {
            this.lastHitTime = System.currentTimeMillis();
        }
    }
}

