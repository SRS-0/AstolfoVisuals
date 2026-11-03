package xyz.angames.astolfoclient.client.effects;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;

@Environment(value=EnvType.CLIENT)
public class DamageIndicatorManager {
    private final List<DamageParticle> particles = new ArrayList<DamageParticle>();
    private final Map<Integer, Float> healthCache = new HashMap<Integer, Float>();
    private final DecimalFormat format = new DecimalFormat("#.#");

    public void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            this.particles.clear();
            this.healthCache.clear();
            return;
        }
        DamageIndicatorModule module = (DamageIndicatorModule)AstolfoclientClient.moduleManager.getModuleByName("DamageIndicators");
        if (module == null || !module.isEnabled()) {
            this.healthCache.clear();
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            float previousHealth;
            float damageAmount;
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity)entity;
            int id = living.getId();
            float currentHealth = living.getHealth() + living.getAbsorptionAmount();
            if (this.healthCache.containsKey(id) && (damageAmount = (previousHealth = this.healthCache.get(id).floatValue()) - currentHealth) > 0.1f) {
                this.spawnParticle(living, damageAmount);
            }
            this.healthCache.put(id, Float.valueOf(currentHealth));
        }
        for (int i = this.particles.size() - 1; i >= 0; --i) {
            DamageParticle p = this.particles.get(i);
            ++p.age;
            p.x += (double)p.vx;
            p.y += (double)p.vy;
            p.z += (double)p.vz;
            p.vx *= 0.8f;
            p.vy *= 0.8f;
            p.vz *= 0.8f;
            p.y += (double)0.015f;
            if (p.age < p.maxAge) continue;
            this.particles.remove(i);
        }
    }

    private void spawnParticle(LivingEntity target, float damage) {
        DamageParticle p = new DamageParticle();
        p.x = target.getX() + (Math.random() - 0.5) * 0.8;
        p.y = target.getY() + (double)target.getBbHeight() * 0.5 + Math.random() * 0.5;
        p.z = target.getZ() + (Math.random() - 0.5) * 0.8;
        double angle = Math.random() * Math.PI * 2.0;
        double speed = 0.15 + Math.random() * 0.1;
        p.vx = (float)(Math.cos(angle) * speed);
        p.vz = (float)(Math.sin(angle) * speed);
        p.vy = 0.15f + (float)(Math.random() * 0.1);
        p.text = "-" + this.format.format(damage);
        p.isCrit = damage > 6.0f;
        p.maxAge = p.isCrit ? 50 : 35;
        this.particles.add(p);
    }

    public List<DamageParticle> getParticles() {
        return this.particles;
    }

    @Environment(value=EnvType.CLIENT)
    public static class DamageParticle {
        public String text;
        public double x;
        public double y;
        public double z;
        public float vx;
        public float vy;
        public float vz;
        public int age = 0;
        public int maxAge;
        public boolean isCrit;
    }
}

