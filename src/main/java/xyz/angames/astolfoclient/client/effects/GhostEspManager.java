package xyz.angames.astolfoclient.client.effects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.effects.GhostEspEffect;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class GhostEspManager {
    public static final long LIFESPAN = 450L;
    private final Map<Entity, GhostEspEffect> effects = new ConcurrentHashMap<Entity, GhostEspEffect>();

    public void addEffect(Entity target) {
        if (target == null || TargetUtils.isInvisible(target)) {
            return;
        }
        this.effects.compute(target, (entity, effect) -> {
            if (effect == null) {
                return new GhostEspEffect(target);
            }
            effect.lastHitTime = System.currentTimeMillis();
            return effect;
        });
    }

    public void addAttack(Entity target) {
        if (target == null || TargetUtils.isInvisible(target)) {
            return;
        }
        this.effects.computeIfPresent(target, (entity, effect) -> {
            effect.lastAttackTime = System.currentTimeMillis();
            return effect;
        });
    }

    public void tick() {
        this.effects.entrySet().removeIf(entry -> !((Entity)entry.getKey()).isAlive() || System.currentTimeMillis() - ((GhostEspEffect)entry.getValue()).lastHitTime > 450L || TargetUtils.isInvisible((Entity)entry.getKey()));
    }

    public Map<Entity, GhostEspEffect> getEffects() {
        return this.effects;
    }
}

