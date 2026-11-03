package xyz.angames.astolfoclient.client.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class GuiScaleSettings {
    public static final float[] SCALES = new float[]{0.35f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f};
    public static final String[] SCALE_LABELS = new String[]{"35%", "50%", "75%", "100%", "125%", "150%", "200%"};
    private static float scale = 1.0f;

    public static float getScale() {
        return scale;
    }

    public static void setScale(float newScale) {
        scale = newScale;
    }

    public static String getScaleLabel() {
        return Math.round(scale * 100.0f) + "%";
    }

    public static int getSelectedIndex() {
        for (int i = 0; i < SCALES.length; ++i) {
            if (!(Math.abs(SCALES[i] - scale) < 0.01f)) continue;
            return i;
        }
        return 3;
    }
}

