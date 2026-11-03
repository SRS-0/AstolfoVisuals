package xyz.angames.astolfoclient.client.effects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.effects.HitEspEffect;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;

@Environment(value=EnvType.CLIENT)
public class HitEspManager {
    private final List<HitEspEffect> effects = new CopyOnWriteArrayList<HitEspEffect>();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public void addEffect(Vec3d position, float rotationDirection, Quaternionf orientation) {
        this.effects.add(new HitEspEffect(position, rotationDirection, orientation));
    }

    public void tick() {
        long currentTime = System.currentTimeMillis();
        HitEspModule module = (HitEspModule)AstolfoclientClient.moduleManager.getModuleByName("HitESP");
        if (module == null) {
            return;
        }
        double leaveTimeVal = module.leaveTime.get();
        boolean doExplosionVal = module.doExplosion.get();
        double fadeTimeVal = module.fadeTime.get();
        double gravityVal = module.gravity.get();
        double frictionVal = module.friction.get();
        double groundLifespanVal = module.groundLifespan.get();
        double spinSpeedVal = module.spinSpeed.get();
        boolean bounceVal = module.bounce.get();
        double bounceFactorVal = module.bounceFactor.get();
        this.effects.removeIf(effect -> {
            long age = currentTime - effect.creationTime;
            if (!doExplosionVal) {
                return (double)age >= leaveTimeVal + fadeTimeVal;
            }
            if ((double)age >= leaveTimeVal && !effect.isShattered) {
                this.shatterEffect((HitEspEffect)effect, module);
            }
            if (effect.isShattered) {
                boolean allDead = true;
                for (HitEspEffect.Shard shard : effect.shards) {
                    shard.prevPos = shard.pos;
                    shard.prevRotX = shard.rotX;
                    shard.prevRotY = shard.rotY;
                    shard.prevRotZ = shard.rotZ;
                    if (!shard.onGround) {
                        shard.vy -= gravityVal;
                        shard.vx *= frictionVal;
                        shard.vz *= frictionVal;
                        if (this.mc.world != null) {
                            boolean collideZ;
                            double nextX = shard.pos.x + shard.vx;
                            double nextY = shard.pos.y + shard.vy;
                            double nextZ = shard.pos.z + shard.vz;
                            boolean collideX = !this.mc.world.getBlockState(BlockPos.ofFloored((Position)new Vec3d(nextX, shard.pos.y, shard.pos.z))).getCollisionShape((BlockView)this.mc.world, BlockPos.ofFloored((Position)new Vec3d(nextX, shard.pos.y, shard.pos.z))).isEmpty();
                            boolean collideY = !this.mc.world.getBlockState(BlockPos.ofFloored((Position)new Vec3d(shard.pos.x, nextY, shard.pos.z))).getCollisionShape((BlockView)this.mc.world, BlockPos.ofFloored((Position)new Vec3d(shard.pos.x, nextY, shard.pos.z))).isEmpty();
                            boolean bl = collideZ = !this.mc.world.getBlockState(BlockPos.ofFloored((Position)new Vec3d(shard.pos.x, shard.pos.y, nextZ))).getCollisionShape((BlockView)this.mc.world, BlockPos.ofFloored((Position)new Vec3d(shard.pos.x, shard.pos.y, nextZ))).isEmpty();
                            if (bounceVal) {
                                if (collideX) {
                                    shard.vx = -shard.vx * bounceFactorVal;
                                }
                                if (collideY) {
                                    if (Math.abs(shard.vy) < 0.05 && shard.vy < 0.0) {
                                        shard.onGround = true;
                                        shard.groundHitTime = currentTime;
                                    } else {
                                        shard.vy = -shard.vy * bounceFactorVal;
                                    }
                                }
                                if (collideZ) {
                                    shard.vz = -shard.vz * bounceFactorVal;
                                }
                                if (!shard.onGround) {
                                    shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                                    shard.rotX = (float)((double)shard.rotX + (double)shard.rotSpeedX * spinSpeedVal);
                                    shard.rotY = (float)((double)shard.rotY + (double)shard.rotSpeedY * spinSpeedVal);
                                    shard.rotZ = (float)((double)shard.rotZ + (double)shard.rotSpeedZ * spinSpeedVal);
                                }
                            } else if (collideX || collideY || collideZ) {
                                shard.onGround = true;
                                shard.groundHitTime = currentTime;
                            } else {
                                shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                                shard.rotX = (float)((double)shard.rotX + (double)shard.rotSpeedX * spinSpeedVal);
                                shard.rotY = (float)((double)shard.rotY + (double)shard.rotSpeedY * spinSpeedVal);
                                shard.rotZ = (float)((double)shard.rotZ + (double)shard.rotSpeedZ * spinSpeedVal);
                            }
                        } else {
                            shard.pos = shard.pos.add(shard.vx, shard.vy, shard.vz);
                            shard.rotX = (float)((double)shard.rotX + (double)shard.rotSpeedX * spinSpeedVal);
                            shard.rotY = (float)((double)shard.rotY + (double)shard.rotSpeedY * spinSpeedVal);
                            shard.rotZ = (float)((double)shard.rotZ + (double)shard.rotSpeedZ * spinSpeedVal);
                        }
                    }
                    if (shard.onGround && !((double)(currentTime - shard.groundHitTime) < groundLifespanVal)) continue;
                    allDead = false;
                }
                return allDead;
            }
            return false;
        });
    }

    private void shatterEffect(HitEspEffect effect, HitEspModule module) {
        effect.isShattered = true;
        int gridSize = (int)module.gridSize.get();
        if (gridSize < 1) {
            gridSize = 1;
        }
        float step = 1.0f / (float)gridSize;
        double explosionStrength = module.explosionStrength.get();
        double explosionRadius = module.explosionRadius.get();
        for (int x = 0; x < gridSize; ++x) {
            for (int y = 0; y < gridSize; ++y) {
                float u1 = (float)x * step;
                float v1 = (float)y * step;
                float u2 = u1 + step;
                float v2 = v1 + step;
                float localX = ((float)x + 0.5f) * step - 0.5f;
                float localY = ((float)y + 0.5f) * step - 0.5f;
                Vector3f localOffset = new Vector3f(localX, localY, 0.0f);
                effect.orientation.transform(localOffset);
                Vec3d startPos = effect.position.add((double)localOffset.x * explosionRadius, (double)localOffset.y * explosionRadius, (double)localOffset.z * explosionRadius);
                double vx = ((double)localOffset.x * 0.5 + (Math.random() - 0.5) * 0.3) * explosionStrength;
                double vy = ((double)localOffset.y * 0.5 + Math.random() * 0.4 + 0.2) * explosionStrength;
                double vz = ((double)localOffset.z * 0.5 + (Math.random() - 0.5) * 0.3) * explosionStrength;
                effect.shards.add(new HitEspEffect.Shard(startPos, vx, vy, vz, u1, v1, u2, v2));
            }
        }
    }

    public List<HitEspEffect> getEffects() {
        return this.effects;
    }
}

