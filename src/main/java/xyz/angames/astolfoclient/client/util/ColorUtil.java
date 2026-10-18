package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
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
      return Mth.clamp(alpha, 0, 255) << 24
         | Mth.clamp(red, 0, 255) << 16
         | Mth.clamp(green, 0, 255) << 8
         | Mth.clamp(blue, 0, 255);
   }

   public static int multAlpha(int color, float alphaPercent) {
      return makeColor(red(color), green(color), blue(color), Math.round(alpha(color) * alphaPercent));
   }
}
