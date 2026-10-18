package xyz.angames.astolfoclient.client.util;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public final class DrawUtil {
   public static void drawLiquidRect(
      PoseStack matrices,
      float x,
      float y,
      float width,
      float height,
      BorderRadius borderRadius,
      ColorRGBA color,
      float cornerSmoothness,
      float fresnelPower,
      float fresnelAlpha,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float blurRadius
   ) {
      Matrix4f matrix = matrices.last().pose();
      Color awtColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
      QuadRadiusState radiusState = new QuadRadiusState(
         borderRadius.topLeftRadius(), borderRadius.topRightRadius(), borderRadius.bottomLeftRadius(), borderRadius.bottomRightRadius()
      );
      QuadRadiusState liquidRadiusState = new QuadRadiusState(
         borderRadius.topLeftRadius() * cornerSmoothness / 2.0F,
         borderRadius.topRightRadius() * cornerSmoothness / 2.0F,
         borderRadius.bottomLeftRadius() * cornerSmoothness / 2.0F,
         borderRadius.bottomRightRadius() * cornerSmoothness / 2.0F
      );
      Builder.blur()
         .size(new SizeState(width, height))
         .radius(radiusState)
         .blurRadius(blurRadius)
         .color(new QuadColorState(new Color(255, 255, 255, 45)))
         .build()
         .render(matrix, x, y);
      Builder.liquidGlass()
         .size(new SizeState(width, height))
         .radius(liquidRadiusState)
         .color(new QuadColorState(awtColor))
         .smoothness(1.0F, cornerSmoothness)
         .alpha(1.0F, baseAlpha)
         .fresnel(fresnelPower, Color.WHITE, fresnelAlpha, fresnelMix, fresnelInvert)
         .distortStrength(distortStrength)
         .captureBackground()
         .build()
         .render(matrix, x, y);
   }
}
