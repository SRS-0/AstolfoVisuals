package xyz.angames.astolfoclient.client.compat;

import com.mojang.renderpearl.api.pipeline.BlendFactor;

/** Legacy GlStateManager.SourceFactor, mapped onto 26.x blend factors. */
public enum SourceFactor {
   CONSTANT_ALPHA(BlendFactor.CONSTANT_ALPHA, 32771),
   CONSTANT_COLOR(BlendFactor.CONSTANT_COLOR, 32769),
   DST_ALPHA(BlendFactor.DST_ALPHA, 772),
   DST_COLOR(BlendFactor.DST_COLOR, 774),
   ONE(BlendFactor.ONE, 1),
   ONE_MINUS_CONSTANT_ALPHA(BlendFactor.ONE_MINUS_CONSTANT_ALPHA, 32772),
   ONE_MINUS_CONSTANT_COLOR(BlendFactor.ONE_MINUS_CONSTANT_COLOR, 32770),
   ONE_MINUS_DST_ALPHA(BlendFactor.ONE_MINUS_DST_ALPHA, 773),
   ONE_MINUS_DST_COLOR(BlendFactor.ONE_MINUS_DST_COLOR, 775),
   ONE_MINUS_SRC_ALPHA(BlendFactor.ONE_MINUS_SRC_ALPHA, 771),
   ONE_MINUS_SRC_COLOR(BlendFactor.ONE_MINUS_SRC_COLOR, 769),
   SRC_ALPHA(BlendFactor.SRC_ALPHA, 770),
   SRC_ALPHA_SATURATE(BlendFactor.SRC_ALPHA_SATURATE, 776),
   SRC_COLOR(BlendFactor.SRC_COLOR, 768),
   ZERO(BlendFactor.ZERO, 0);

   public final BlendFactor factor;
   public final int value;

   SourceFactor(BlendFactor factor, int value) {
      this.factor = factor;
      this.value = value;
   }

   public static BlendFactor fromGl(int gl) {
      for (SourceFactor f : values()) {
         if (f.value == gl) {
            return f.factor;
         }
      }

      return BlendFactor.ONE;
   }
}
