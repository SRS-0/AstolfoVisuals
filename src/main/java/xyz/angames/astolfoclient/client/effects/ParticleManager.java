package xyz.angames.astolfoclient.client.effects;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.effects.Particle;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(value=EnvType.CLIENT)
public class ParticleManager {
    private final List<Particle> particles = new CopyOnWriteArrayList<Particle>();
    private final Random random = new Random();
    private static final double GRAVITY = 0.015;
    private static final double EXPLOSION_STRENGTH = 0.3;
    private static final double BOUNCE_FACTOR = 0.6;
    private static final double FRICTION = 0.98;
    private static final Color[] VANILLA_TOTEM_COLORS = new Color[]{new Color(130, 255, 60), new Color(60, 230, 80), new Color(40, 180, 70), new Color(255, 255, 140), new Color(255, 180, 30)};

    public void addEffects(Vec3d origin) {
        ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
        if (mod == null || !mod.isEnabled() || !mod.hits.get()) {
            return;
        }
        int amount = mod.amount.getInt();
        long lifespan = mod.lifespan.getInt();
        for (int i = 0; i < amount; ++i) {
            double velX = (this.random.nextDouble() - 0.5) * 0.3;
            double velY = this.random.nextDouble() * 0.5 * 0.3;
            double velZ = (this.random.nextDouble() - 0.5) * 0.3;
            this.particles.add(new Particle(origin, new Vec3d(velX, velY, velZ), mod.type.getValue(), true, lifespan));
        }
    }

    public void addTotemPop(Vec3d origin) {
        ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
        if (mod == null || !mod.isEnabled() || !mod.totemPop.get()) {
            return;
        }
        int amount = mod.totemAmount.getInt();
        long lifespan = mod.totemLifespan.getInt();
        ParticlesModule.ParticleType pType = mod.totemType.getValue();
        String animMode = mod.totemAnimation.get();
        String colorMode = mod.totemColor.get();
        boolean physics = mod.totemPhysics.get();
        for (int i = 0; i < amount; ++i) {
            Vec3d vel;
            Particle.ParticleAnimation anim;
            Color color = this.getTotemParticleColor(colorMode);
            double extra1 = 0.0;
            double extra2 = 0.0;
            switch (animMode) {
                case "Sphere": {
                    anim = Particle.ParticleAnimation.SPHERE;
                    double phi = Math.acos(1.0 - 2.0 * ((double)i + 0.5) / (double)amount);
                    double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * (double)i;
                    double speed = 0.22 + this.random.nextDouble() * 0.12;
                    double vx = Math.sin(phi) * Math.cos(theta) * speed;
                    double vy = Math.cos(phi) * speed;
                    double vz = Math.sin(phi) * Math.sin(theta) * speed;
                    vel = new Vec3d(vx, vy, vz);
                    break;
                }
                case "Spiral": {
                    anim = Particle.ParticleAnimation.SPIRAL;
                    double angle = Math.PI * 2 / (double)Math.max(1, amount) * (double)i * 2.5 + this.random.nextDouble() * 0.4;
                    double radius = 0.15 + this.random.nextDouble() * 0.25;
                    double upward = 0.08 + this.random.nextDouble() * 0.18;
                    vel = new Vec3d(Math.cos(angle) * 0.1, upward, Math.sin(angle) * 0.1);
                    extra1 = angle;
                    extra2 = radius;
                    break;
                }
                case "Fountain": {
                    anim = Particle.ParticleAnimation.FOUNTAIN;
                    double angle = this.random.nextDouble() * Math.PI * 2.0;
                    double spread = 0.08 + this.random.nextDouble() * 0.16;
                    double vx = Math.cos(angle) * spread;
                    double vy = 0.32 + this.random.nextDouble() * 0.26;
                    double vz = Math.sin(angle) * spread;
                    vel = new Vec3d(vx, vy, vz);
                    break;
                }
                case "Shockwave": {
                    anim = Particle.ParticleAnimation.SHOCKWAVE;
                    double angle = Math.PI * 2 / (double)Math.max(1, amount) * (double)i + (this.random.nextDouble() - 0.5) * 0.1;
                    double speed = 0.28 + this.random.nextDouble() * 0.12;
                    double vx = Math.cos(angle) * speed;
                    double vy = (this.random.nextDouble() - 0.5) * 0.05;
                    double vz = Math.sin(angle) * speed;
                    vel = new Vec3d(vx, vy, vz);
                    extra1 = angle;
                    break;
                }
                case "Explosion": {
                    anim = Particle.ParticleAnimation.EXPLOSION;
                    double velX = (this.random.nextDouble() - 0.5) * 0.45;
                    double velY = this.random.nextDouble() * 0.45 + 0.05;
                    double velZ = (this.random.nextDouble() - 0.5) * 0.45;
                    vel = new Vec3d(velX, velY, velZ);
                    break;
                }
                default: {
                    anim = Particle.ParticleAnimation.EXPLOSION;
                    double velX = (this.random.nextDouble() - 0.5) * 0.45;
                    double velY = this.random.nextDouble() * 0.45 + 0.05;
                    double velZ = (this.random.nextDouble() - 0.5) * 0.45;
                    vel = new Vec3d(velX, velY, velZ);
                }
            }
            this.particles.add(new Particle(origin, vel, pType, physics, lifespan, color, anim, origin, extra1, extra2));
        }
    }

    private Color getTotemParticleColor(String colorMode) {
        return switch (colorMode) {
            case "Vanilla" -> VANILLA_TOTEM_COLORS[this.random.nextInt(VANILLA_TOTEM_COLORS.length)];
            case "Lime" -> new Color(130, 255, 60);
            case "Green" -> new Color(60, 230, 80);
            case "Dark Green" -> new Color(40, 180, 70);
            case "Light Yellow" -> new Color(255, 255, 140);
            case "Dark Yellow" -> new Color(255, 180, 30);
            case "Theme" -> null;
            default -> VANILLA_TOTEM_COLORS[this.random.nextInt(VANILLA_TOTEM_COLORS.length)];
        };
    }

    public void addTrail(Vec3d origin, ParticlesModule.ParticleType type, int amount, long lifespan) {
        for (int i = 0; i < amount; ++i) {
            double velX = (this.random.nextDouble() - 0.5) * 0.02;
            double velY = (this.random.nextDouble() - 0.5) * 0.02;
            double velZ = (this.random.nextDouble() - 0.5) * 0.02;
            this.particles.add(new Particle(origin, new Vec3d(velX, velY, velZ), type, false, lifespan));
        }
    }

    public void tick() {
        ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
        if (mod == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            this.particles.clear();
            return;
        }
        if (mod.isEnabled()) {
            if (mod.walk.get() && client.player.isOnGround() && client.player.getVelocity().lengthSquared() > 0.01 && this.random.nextInt(3) == 0) {
                Vec3d footPos = client.player.getEntityPos().add((this.random.nextDouble() - 0.5) * 0.5, this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.5);
                this.addTrail(footPos, mod.walkType.getValue(), mod.walkAmount.getInt(), mod.walkLifespan.getInt());
            }
            for (Entity entity : client.world.getEntities()) {
                if (!(entity.getVelocity().lengthSquared() > 0.01)) continue;
                if (mod.arrows.get() && entity instanceof PersistentProjectileEntity && !(entity instanceof TridentEntity)) {
                    this.addTrail(entity.getEntityPos().add(0.0, (double)(entity.getHeight() / 2.0f), 0.0), mod.arrowType.getValue(), mod.arrowAmount.getInt(), mod.arrowLifespan.getInt());
                    continue;
                }
                if (mod.pearls.get() && entity instanceof EnderPearlEntity) {
                    this.addTrail(entity.getEntityPos().add(0.0, (double)(entity.getHeight() / 2.0f), 0.0), mod.pearlType.getValue(), mod.pearlAmount.getInt(), mod.pearlLifespan.getInt());
                    continue;
                }
                if (mod.tridents.get() && entity instanceof TridentEntity) {
                    this.addTrail(entity.getEntityPos().add(0.0, (double)(entity.getHeight() / 2.0f), 0.0), mod.tridentType.getValue(), mod.tridentAmount.getInt(), mod.tridentLifespan.getInt());
                    continue;
                }
                if (!mod.items.get() || !(entity instanceof ItemEntity) || this.random.nextInt(2) != 0) continue;
                this.addTrail(entity.getEntityPos().add(0.0, (double)(entity.getHeight() / 2.0f), 0.0), mod.itemType.getValue(), mod.itemAmount.getInt(), mod.itemLifespan.getInt());
            }
        }
        long currentTime = System.currentTimeMillis();
        this.particles.removeIf(p -> {
            if (currentTime - p.creationTime > p.lifespan) {
                return true;
            }
            p.prevPosition = p.position;
            switch (p.animation) {
                case SPHERE: {
                    p.velocity = p.velocity.multiply(0.95);
                    if (p.hasPhysics) {
                        this.handlePhysics(client, (Particle)p);
                        break;
                    }
                    p.position = p.position.add(p.velocity);
                    break;
                }
                case SPIRAL: {
                    p.extraData1 += 0.18;
                    p.extraData2 += 0.015;
                    double sx = p.origin.x + Math.cos(p.extraData1) * p.extraData2;
                    double sz = p.origin.z + Math.sin(p.extraData1) * p.extraData2;
                    p.velocity = p.velocity.multiply(0.97);
                    p.position = new Vec3d(sx, p.position.y + p.velocity.y, sz);
                    break;
                }
                case SHOCKWAVE: {
                    p.velocity = p.velocity.multiply(0.95);
                    if (p.hasPhysics) {
                        this.handlePhysics(client, (Particle)p);
                        break;
                    }
                    p.position = p.position.add(p.velocity);
                    break;
                }
                case FOUNTAIN: {
                    p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                    p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                    if (p.hasPhysics) {
                        this.handlePhysics(client, (Particle)p);
                        break;
                    }
                    p.position = p.position.add(p.velocity);
                    break;
                }
                case EXPLOSION: 
                case NONE: {
                    if (p.hasPhysics) {
                        p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                        p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                        this.handlePhysics(client, (Particle)p);
                        break;
                    }
                    p.position = p.position.add(p.velocity);
                    break;
                }
                default: {
                    if (p.hasPhysics) {
                        p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                        p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                        this.handlePhysics(client, (Particle)p);
                        break;
                    }
                    p.position = p.position.add(p.velocity);
                }
            }
            return false;
        });
    }

    private boolean shouldCollide(MinecraftClient client, BlockPos pos) {
        if (client.world == null) {
            return false;
        }
        BlockState state = client.world.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.isLiquid()) {
            return false;
        }
        if (state.isOf(Blocks.LIGHT) || state.isOf(Blocks.STRUCTURE_VOID)) {
            return false;
        }
        VoxelShape collision = state.getCollisionShape((BlockView)client.world, pos);
        return !collision.isEmpty();
    }

    private void handlePhysics(MinecraftClient client, Particle p) {
        if (client.world == null) {
            p.position = p.position.add(p.velocity);
            return;
        }
        double dx = p.velocity.x;
        double dy = p.velocity.y;
        double dz = p.velocity.z;
        double currX = p.position.x;
        double currY = p.position.y;
        double currZ = p.position.z;
        if (this.shouldCollide(client, BlockPos.ofFloored((double)currX, (double)(currY + dy), (double)currZ))) {
            p.velocity = new Vec3d(p.velocity.x, -dy * 0.6, p.velocity.z);
        }
        if (this.shouldCollide(client, BlockPos.ofFloored((double)(currX + dx), (double)currY, (double)currZ))) {
            p.velocity = new Vec3d(-dx * 0.6, p.velocity.y, p.velocity.z);
        }
        if (this.shouldCollide(client, BlockPos.ofFloored((double)currX, (double)currY, (double)(currZ + dz)))) {
            p.velocity = new Vec3d(p.velocity.x, p.velocity.y, -dz * 0.6);
        }
        p.position = p.position.add(p.velocity);
    }

    public List<Particle> getParticles() {
        return this.particles;
    }
}

