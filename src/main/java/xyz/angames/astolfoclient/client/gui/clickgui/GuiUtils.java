package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import java.lang.reflect.Field;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class GuiUtils {
    public static final Supplier<MsdfFont> FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    public static final Supplier<MsdfFont> SEMIBOLD = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    public static final Supplier<MsdfFont> MEDIUM = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());

    public static float animate(float current, float target, float speed, float deltaTime) {
        if (Math.abs(target - current) < 5.0E-5f) {
            return target;
        }
        float factor = 1.0f - (float)Math.exp(-speed * deltaTime);
        return current + (target - current) * factor;
    }

    public static void renderTextSafely(Matrix4f matrix, String text, float x, float y, Color color, float size) {
        if (text == null || text.isEmpty() || color.getAlpha() <= 4) {
            return;
        }
        try {
            ((BuiltText)Builder.text().font((MsdfFont)SEMIBOLD.get()).text(text).color(color).size(size).build()).render(matrix, x, y);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static void renderMediumTextSafely(Matrix4f matrix, String text, float x, float y, Color color, float size) {
        if (text == null || text.isEmpty() || color.getAlpha() <= 4) {
            return;
        }
        try {
            ((BuiltText)Builder.text().font((MsdfFont)MEDIUM.get()).text(text).color(color).size(size).build()).render(matrix, x, y);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public static Color withAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(MathHelper.clamp((float)alpha, (float)0.0f, (float)1.0f) * 255.0f));
    }

    public static Color interpolateColor(Color c1, Color c2, float factor) {
        float f = MathHelper.clamp((float)factor, (float)0.0f, (float)1.0f);
        return new Color((int)((float)c1.getRed() + (float)(c2.getRed() - c1.getRed()) * f), (int)((float)c1.getGreen() + (float)(c2.getGreen() - c1.getGreen()) * f), (int)((float)c1.getBlue() + (float)(c2.getBlue() - c1.getBlue()) * f), (int)((float)c1.getAlpha() + (float)(c2.getAlpha() - c1.getAlpha()) * f));
    }

    public static void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float baseRadius, Color themeColor, float alphaMultiplier) {
        GuiUtils.drawIconGlowShadow(matrix, cx, cy, baseRadius, themeColor, alphaMultiplier, 8.0f);
    }

    public static void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float baseRadius, Color themeColor, float alphaMultiplier, float maxSpread) {
        int glowSteps = 6;
        int r = themeColor.getRed();
        int g = themeColor.getGreen();
        int b = themeColor.getBlue();
        float baseAlpha = (float)themeColor.getAlpha() / 255.0f;
        for (int i = glowSteps - 1; i >= 0; --i) {
            float progress = (float)i / (float)glowSteps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            float alpha = baseAlpha * alphaMultiplier * alphaFactor;
            int alphaInt = (int)(255.0f * alpha);
            if (alphaInt <= 0) continue;
            Color glowColor = new Color(r, g, b, alphaInt);
            float radius = baseRadius + spread;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(radius * 2.0f, radius * 2.0f)).radius(new QuadRadiusState(radius)).color(new QuadColorState(glowColor)).build()).render(matrix, cx - radius, cy - radius);
        }
    }

    public static boolean isMouseOver(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static float getScaleModifier(MinecraftClient client) {
        return client == null || client.getWindow() == null ? 1.0f : (float)(2.0 / client.getWindow().getScaleFactor());
    }

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return 1.0f + c3 * (float)Math.pow(x - 1.0f, 3.0) + c1 * (float)Math.pow(x - 1.0f, 2.0);
    }

    public static float easeOutElastic(float x) {
        float c4 = 2.0943952f;
        return x == 0.0f ? 0.0f : (x == 1.0f ? 1.0f : (float)Math.pow(2.0, -10.0f * x) * (float)Math.sin((x * 10.0f - 0.75f) * c4) + 1.0f);
    }

    public static String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    public static boolean hasSettings(Module m) {
        for (Class<?> cls = m.getClass(); cls != null && cls != Module.class; cls = cls.getSuperclass()) {
            for (Field f : cls.getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(m);
                    if (!(v instanceof NumberSetting) && !(v instanceof BooleanSetting)) continue;
                    return true;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        return false;
    }
}

