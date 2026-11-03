package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

@Environment(value=EnvType.CLIENT)
public class HitEspEffect {
    public final long creationTime;
    public final Vec3d position;
    public final float rotationDirection;
    public final Quaternionf orientation;
    public boolean isShattered = false;
    public final List<Shard> shards = new ArrayList<Shard>();

    public HitEspEffect(Vec3d position, float rotationDirection, Quaternionf orientation) {
        this.creationTime = System.currentTimeMillis();
        this.position = position;
        this.rotationDirection = rotationDirection;
        this.orientation = orientation;
    }

    @Environment(value=EnvType.CLIENT)
    public static class Shard {
        public Vec3d pos;
        public Vec3d prevPos;
        public double vx;
        public double vy;
        public double vz;
        public float rotX;
        public float rotY;
        public float rotZ;
        public float prevRotX;
        public float prevRotY;
        public float prevRotZ;
        public float rotSpeedX;
        public float rotSpeedY;
        public float rotSpeedZ;
        public float u1;
        public float v1;
        public float u2;
        public float v2;
        public boolean onGround = false;
        public long groundHitTime = 0L;

        public Shard(Vec3d startPos, double vx, double vy, double vz, float u1, float v1, float u2, float v2) {
            this.pos = startPos;
            this.prevPos = startPos;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.rotX = 0.0f;
            this.rotY = 0.0f;
            this.rotZ = 0.0f;
            this.prevRotX = 0.0f;
            this.prevRotY = 0.0f;
            this.prevRotZ = 0.0f;
            this.rotSpeedX = (float)(Math.random() - 0.5) * 30.0f;
            this.rotSpeedY = (float)(Math.random() - 0.5) * 30.0f;
            this.rotSpeedZ = (float)(Math.random() - 0.5) * 30.0f;
        }
    }
}

