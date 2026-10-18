package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.lang.reflect.Field;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(EnvType.CLIENT)
public class GuiUtils {
   public static final Supplier<MsdfFont> FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   public static final Supplier<MsdfFont> SEMIBOLD = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   public static final Supplier<MsdfFont> MEDIUM = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());

   public static float animate(float current, float target, float speed, float deltaTime) {
      if (Math.abs(target - current) < 5.0E-5F) {
         return target;
      }

      float factor = 1.0F - (float)Math.exp(-speed * deltaTime);
      return current + (target - current) * factor;
   }

   public static void renderTextSafely(Matrix4f matrix, String text, float x, float y, Color color, float size) {
      if (text != null && !text.isEmpty() && color.getAlpha() > 4) {
         try {
            Builder.text().font((MsdfFont)SEMIBOLD.get()).text(text).color(color).size(size).build().render(matrix, x, y);
         } catch (Exception var7) {
         }
      }
   }

   public static void renderMediumTextSafely(Matrix4f matrix, String text, float x, float y, Color color, float size) {
      if (text != null && !text.isEmpty() && color.getAlpha() > 4) {
         try {
            Builder.text().font((MsdfFont)MEDIUM.get()).text(text).color(color).size(size).build().render(matrix, x, y);
         } catch (Exception var7) {
         }
      }
   }

   public static Color withAlpha(Color c, float alpha) {
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F));
   }

   public static Color interpolateColor(Color c1, Color c2, float factor) {
      float f = Mth.clamp(factor, 0.0F, 1.0F);
      return new Color(
         (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * f),
         (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * f),
         (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * f),
         (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * f)
      );
   }

   public static void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float baseRadius, Color themeColor, float alphaMultiplier) {
      drawIconGlowShadow(matrix, cx, cy, baseRadius, themeColor, alphaMultiplier, 8.0F);
   }

   public static void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float baseRadius, Color themeColor, float alphaMultiplier, float maxSpread) {
      int glowSteps = 6;
      int r = themeColor.getRed();
      int g = themeColor.getGreen();
      int b = themeColor.getBlue();
      float baseAlpha = themeColor.getAlpha() / 255.0F;

      for (int i = glowSteps - 1; i >= 0; i--) {
         float progress = (float)i / glowSteps;
         float spread = progress * maxSpread;
         float alphaFactor = (1.0F - progress) * (1.0F - progress);
         float alpha = baseAlpha * alphaMultiplier * alphaFactor;
         int alphaInt = (int)(255.0F * alpha);
         if (alphaInt > 0) {
            Color glowColor = new Color(r, g, b, alphaInt);
            float radius = baseRadius + spread;
            Builder.rectangle()
               .size(new SizeState(radius * 2.0F, radius * 2.0F))
               .radius(new QuadRadiusState(radius))
               .color(new QuadColorState(glowColor))
               .build()
               .render(matrix, cx - radius, cy - radius);
         }
      }
   }

   public static boolean isMouseOver(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
   }

   public static float getScaleModifier(Minecraft client) {
      return client != null && client.getWindow() != null ? (float)(2.0 / client.getWindow().getGuiScale()) : 1.0F;
   }

   public static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   public static float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(x - 1.0F, 3.0) + c1 * (float)Math.pow(x - 1.0F, 2.0);
   }

   public static float easeOutElastic(float x) {
      float c4 = (float) (Math.PI * 2.0 / 3.0);
      return x == 0.0F ? 0.0F : (x == 1.0F ? 1.0F : (float)Math.pow(2.0, -10.0F * x) * (float)Math.sin((x * 10.0F - 0.75F) * c4) + 1.0F);
   }

   public static String capitalize(String s) {
      return s != null && !s.isEmpty() ? Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase() : s;
   }

   public static boolean hasSettings(Module m) {
      for (Class<?> cls = m.getClass(); cls != null && cls != Module.class; cls = cls.getSuperclass()) {
         for (Field f : cls.getDeclaredFields()) {
            try {
               f.setAccessible(true);
               Object v = f.get(m);
               if (v instanceof NumberSetting || v instanceof BooleanSetting) {
                  return true;
               }
            } catch (Exception var7) {
            }
         }
      }

      return false;
   }
}
