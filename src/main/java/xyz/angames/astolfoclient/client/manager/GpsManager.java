package xyz.angames.astolfoclient.client.manager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GpsManager {
    private static GpsManager instance;
    private boolean active = false;
    private double targetX;
    private double targetZ;

    public GpsManager() {
        instance = this;
    }

    public static GpsManager getInstance() {
        if (instance == null) {
            instance = new GpsManager();
        }
        return instance;
    }

    public void setWaypoint(double x, double z) {
        this.targetX = x;
        this.targetZ = z;
        this.active = true;
    }

    public void clear() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    public double getTargetX() {
        return this.targetX;
    }

    public double getTargetZ() {
        return this.targetZ;
    }
}

