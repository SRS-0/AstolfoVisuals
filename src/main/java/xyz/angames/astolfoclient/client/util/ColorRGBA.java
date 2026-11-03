package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ColorRGBA(int r, int g, int b, int a) {
    public int getRed() {
        return this.r;
    }

    public int getGreen() {
        return this.g;
    }

    public int getBlue() {
        return this.b;
    }

    public int getAlpha() {
        return this.a;
    }
}

