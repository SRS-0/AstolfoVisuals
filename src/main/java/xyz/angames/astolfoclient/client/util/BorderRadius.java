package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record BorderRadius(float topLeft, float topRight, float bottomLeft, float bottomRight) {
    public static BorderRadius all(float radius) {
        return new BorderRadius(radius, radius, radius, radius);
    }

    public float topLeftRadius() {
        return this.topLeft;
    }

    public float topRightRadius() {
        return this.topRight;
    }

    public float bottomLeftRadius() {
        return this.bottomLeft;
    }

    public float bottomRightRadius() {
        return this.bottomRight;
    }
}

