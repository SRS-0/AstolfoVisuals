package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

@Environment(value=EnvType.CLIENT)
public class ColorUtil {
    public static int red(int c) {
        return c >> 16 & 0xFF;
    }

    public static int green(int c) {
        return c >> 8 & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    public static int alpha(int c) {
        return c >> 24 & 0xFF;
    }

    public static int makeColor(int red, int green, int blue, int alpha) {
        return MathHelper.clamp((int)alpha, (int)0, (int)255) << 24 | MathHelper.clamp((int)red, (int)0, (int)255) << 16 | MathHelper.clamp((int)green, (int)0, (int)255) << 8 | MathHelper.clamp((int)blue, (int)0, (int)255);
    }

    public static int multAlpha(int color, float alphaPercent) {
        return ColorUtil.makeColor(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), Math.round((float)ColorUtil.alpha(color) * alphaPercent));
    }
}

