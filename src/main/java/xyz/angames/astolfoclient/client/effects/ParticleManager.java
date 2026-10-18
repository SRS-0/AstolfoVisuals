package xyz.angames.astolfoclient.client.effects;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;

@Environment(EnvType.CLIENT)
public class ParticleManager {
   private final List<Particle> particles = new CopyOnWriteArrayList<>();
   private final Random random = new Random();
   private static final double GRAVITY = 0.015;
   private static final double EXPLOSION_STRENGTH = 0.3;
   private static final double BOUNCE_FACTOR = 0.6;
   private static final double FRICTION = 0.98;
   private static final Color[] VANILLA_TOTEM_COLORS = new Color[]{
      new Color(130, 255, 60), new Color(60, 230, 80), new Color(40, 180, 70), new Color(255, 255, 140), new Color(255, 180, 30)
   };

   public void addEffects(Vec3 origin) {
      ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
      if (mod != null && mod.isEnabled() && mod.hits.get()) {
         int amount = mod.amount.getInt();
         long lifespan = mod.lifespan.getInt();

         for (int i = 0; i < amount; i++) {
            double velX = (this.random.nextDouble() - 0.5) * 0.3;
            double velY = this.random.nextDouble() * 0.5 * 0.3;
            double velZ = (this.random.nextDouble() - 0.5) * 0.3;
            this.particles.add(new Particle(origin, new Vec3(velX, velY, velZ), mod.type.getValue(), true, lifespan));
         }
      }
   }

   public void addTotemPop(Vec3 origin) {
      ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
      if (mod != null && mod.isEnabled() && mod.totemPop.get()) {
         int amount = mod.totemAmount.getInt();
         long lifespan = mod.totemLifespan.getInt();
         ParticlesModule.ParticleType pType = mod.totemType.getValue();
         String animMode = mod.totemAnimation.get();
         String colorMode = mod.totemColor.get();
         boolean physics = mod.totemPhysics.get();

         for (int i = 0; i < amount; i++) {
            Color color = this.getTotemParticleColor(colorMode);
            double extra1 = 0.0;
            double extra2 = 0.0;
            Particle.ParticleAnimation anim;
            Vec3 vel;
            switch (animMode) {
               case "Sphere": {
                  anim = Particle.ParticleAnimation.SPHERE;
                  double phi = Math.acos(1.0 - 2.0 * (i + 0.5) / amount);
                  double theta = Math.PI * (1.0 + Math.sqrt(5.0)) * i;
                  double speed = 0.22 + this.random.nextDouble() * 0.12;
                  double vx = Math.sin(phi) * Math.cos(theta) * speed;
                  double vy = Math.cos(phi) * speed;
                  double vz = Math.sin(phi) * Math.sin(theta) * speed;
                  vel = new Vec3(vx, vy, vz);
                  break;
               }
               case "Spiral": {
                  anim = Particle.ParticleAnimation.SPIRAL;
                  double angle = (Math.PI * 2) / Math.max(1, amount) * i * 2.5 + this.random.nextDouble() * 0.4;
                  double radius = 0.15 + this.random.nextDouble() * 0.25;
                  double upward = 0.08 + this.random.nextDouble() * 0.18;
                  vel = new Vec3(Math.cos(angle) * 0.1, upward, Math.sin(angle) * 0.1);
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
                  vel = new Vec3(vx, vy, vz);
                  break;
               }
               case "Shockwave": {
                  anim = Particle.ParticleAnimation.SHOCKWAVE;
                  double angle = (Math.PI * 2) / Math.max(1, amount) * i + (this.random.nextDouble() - 0.5) * 0.1;
                  double speed = 0.28 + this.random.nextDouble() * 0.12;
                  double vx = Math.cos(angle) * speed;
                  double vy = (this.random.nextDouble() - 0.5) * 0.05;
                  double vz = Math.sin(angle) * speed;
                  vel = new Vec3(vx, vy, vz);
                  extra1 = angle;
                  break;
               }
               case "Explosion": {
                  anim = Particle.ParticleAnimation.EXPLOSION;
                  double velX = (this.random.nextDouble() - 0.5) * 0.45;
                  double velY = this.random.nextDouble() * 0.45 + 0.05;
                  double velZ = (this.random.nextDouble() - 0.5) * 0.45;
                  vel = new Vec3(velX, velY, velZ);
                  break;
               }
               default: {
                  anim = Particle.ParticleAnimation.EXPLOSION;
                  double velX = (this.random.nextDouble() - 0.5) * 0.45;
                  double velY = this.random.nextDouble() * 0.45 + 0.05;
                  double velZ = (this.random.nextDouble() - 0.5) * 0.45;
                  vel = new Vec3(velX, velY, velZ);
               }
            }

            this.particles.add(new Particle(origin, vel, pType, physics, lifespan, color, anim, origin, extra1, extra2));
         }
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

   public void addTrail(Vec3 origin, ParticlesModule.ParticleType type, int amount, long lifespan) {
      for (int i = 0; i < amount; i++) {
         double velX = (this.random.nextDouble() - 0.5) * 0.02;
         double velY = (this.random.nextDouble() - 0.5) * 0.02;
         double velZ = (this.random.nextDouble() - 0.5) * 0.02;
         this.particles.add(new Particle(origin, new Vec3(velX, velY, velZ), type, false, lifespan));
      }
   }

   public void tick() {
      ParticlesModule mod = (ParticlesModule)AstolfoclientClient.moduleManager.getModuleByName("Particles");
      if (mod != null) {
         Minecraft client = Minecraft.getInstance();
         if (client.level != null && client.player != null) {
            if (mod.isEnabled()) {
               if (mod.walk.get() && client.player.onGround() && client.player.getDeltaMovement().lengthSqr() > 0.01 && this.random.nextInt(3) == 0) {
                  Vec3 footPos = client.player
                     .position()
                     .add((this.random.nextDouble() - 0.5) * 0.5, this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.5);
                  this.addTrail(footPos, mod.walkType.getValue(), mod.walkAmount.getInt(), mod.walkLifespan.getInt());
               }

               for (Entity entity : client.level.entitiesForRendering()) {
                  if (entity.getDeltaMovement().lengthSqr() > 0.01) {
                     if (mod.arrows.get() && entity instanceof AbstractArrow && !(entity instanceof ThrownTrident)) {
                        this.addTrail(
                           entity.position().add(0.0, entity.getBbHeight() / 2.0F, 0.0),
                           mod.arrowType.getValue(),
                           mod.arrowAmount.getInt(),
                           mod.arrowLifespan.getInt()
                        );
                     } else if (mod.pearls.get() && entity instanceof ThrownEnderpearl) {
                        this.addTrail(
                           entity.position().add(0.0, entity.getBbHeight() / 2.0F, 0.0),
                           mod.pearlType.getValue(),
                           mod.pearlAmount.getInt(),
                           mod.pearlLifespan.getInt()
                        );
                     } else if (mod.tridents.get() && entity instanceof ThrownTrident) {
                        this.addTrail(
                           entity.position().add(0.0, entity.getBbHeight() / 2.0F, 0.0),
                           mod.tridentType.getValue(),
                           mod.tridentAmount.getInt(),
                           mod.tridentLifespan.getInt()
                        );
                     } else if (mod.items.get() && entity instanceof ItemEntity && this.random.nextInt(2) == 0) {
                        this.addTrail(
                           entity.position().add(0.0, entity.getBbHeight() / 2.0F, 0.0),
                           mod.itemType.getValue(),
                           mod.itemAmount.getInt(),
                           mod.itemLifespan.getInt()
                        );
                     }
                  }
               }
            }

            long currentTime = System.currentTimeMillis();
            this.particles.removeIf(p -> {
               if (currentTime - p.creationTime > p.lifespan) {
                  return true;
               }

               p.prevPosition = p.position;
               switch (p.animation) {
                  case SPHERE:
                     p.velocity = p.velocity.scale(0.95);
                     if (p.hasPhysics) {
                        this.handlePhysics(client, p);
                     } else {
                        p.position = p.position.add(p.velocity);
                     }
                     break;
                  case SPIRAL:
                     p.extraData1 += 0.18;
                     p.extraData2 += 0.015;
                     double sx = p.origin.x + Math.cos(p.extraData1) * p.extraData2;
                     double sz = p.origin.z + Math.sin(p.extraData1) * p.extraData2;
                     p.velocity = p.velocity.scale(0.97);
                     p.position = new Vec3(sx, p.position.y + p.velocity.y, sz);
                     break;
                  case SHOCKWAVE:
                     p.velocity = p.velocity.scale(0.95);
                     if (p.hasPhysics) {
                        this.handlePhysics(client, p);
                     } else {
                        p.position = p.position.add(p.velocity);
                     }
                     break;
                  case FOUNTAIN:
                     p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                     p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                     if (p.hasPhysics) {
                        this.handlePhysics(client, p);
                     } else {
                        p.position = p.position.add(p.velocity);
                     }
                     break;
                  case EXPLOSION:
                  case NONE:
                     if (p.hasPhysics) {
                        p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                        p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                        this.handlePhysics(client, p);
                     } else {
                        p.position = p.position.add(p.velocity);
                     }
                     break;
                  default:
                     if (p.hasPhysics) {
                        p.velocity = p.velocity.add(0.0, -0.015, 0.0);
                        p.velocity = p.velocity.multiply(0.98, 1.0, 0.98);
                        this.handlePhysics(client, p);
                     } else {
                        p.position = p.position.add(p.velocity);
                     }
               }

               return false;
            });
         } else {
            this.particles.clear();
         }
      }
   }

   private boolean shouldCollide(Minecraft client, BlockPos pos) {
      if (client.level == null) {
         return false;
      } else {
         BlockState state = client.level.getBlockState(pos);
         if (state.isAir()) {
            return false;
         } else if (state.liquid()) {
            return false;
         } else if (!state.is(Blocks.LIGHT) && !state.is(Blocks.STRUCTURE_VOID)) {
            VoxelShape collision = state.getCollisionShape(client.level, pos);
            return !collision.isEmpty();
         } else {
            return false;
         }
      }
   }

   private void handlePhysics(Minecraft client, Particle p) {
      if (client.level == null) {
         p.position = p.position.add(p.velocity);
      } else {
         double dx = p.velocity.x;
         double dy = p.velocity.y;
         double dz = p.velocity.z;
         double currX = p.position.x;
         double currY = p.position.y;
         double currZ = p.position.z;
         if (this.shouldCollide(client, BlockPos.containing(currX, currY + dy, currZ))) {
            p.velocity = new Vec3(p.velocity.x, -dy * 0.6, p.velocity.z);
         }

         if (this.shouldCollide(client, BlockPos.containing(currX + dx, currY, currZ))) {
            p.velocity = new Vec3(-dx * 0.6, p.velocity.y, p.velocity.z);
         }

         if (this.shouldCollide(client, BlockPos.containing(currX, currY, currZ + dz))) {
            p.velocity = new Vec3(p.velocity.x, p.velocity.y, -dz * 0.6);
         }

         p.position = p.position.add(p.velocity);
      }
   }

   public List<Particle> getParticles() {
      return this.particles;
   }
}
