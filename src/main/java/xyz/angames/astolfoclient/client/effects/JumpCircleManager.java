package xyz.angames.astolfoclient.client.effects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.effects.JumpCircle;

@Environment(value=EnvType.CLIENT)
public class JumpCircleManager {
    private final List<JumpCircle> circles = new CopyOnWriteArrayList<JumpCircle>();
    public static final long LIFESPAN = 1300L;

    public void addCircle(double x, double y, double z) {
        this.circles.add(new JumpCircle(x, y, z, 0L));
    }

    public void tick() {
        this.circles.removeIf(circle -> System.currentTimeMillis() - circle.creationTime > 1500L);
    }

    public List<JumpCircle> getCircles() {
        return this.circles;
    }
}

