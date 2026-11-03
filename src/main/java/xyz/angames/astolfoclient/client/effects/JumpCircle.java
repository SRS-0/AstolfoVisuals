package xyz.angames.astolfoclient.client.effects;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class JumpCircle {
    public final long creationTime = System.currentTimeMillis();
    public final double x;
    public final double y;
    public final double z;
    public final long delay;

    public JumpCircle(double x, double y, double z, long delay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delay = delay;
    }
}

