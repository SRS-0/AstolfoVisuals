package xyz.angames.astolfoclient.client.compat;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.renderpearl.api.textures.GpuTexture;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * Raw OpenGL access to 26.x {@link RenderTarget}s. The game is forced onto the OpenGL backend, so the
 * {@link GpuTexture}s are {@link GlTexture}s and expose their GL names. Framebuffer objects are created on demand and
 * keyed by the attachment texture names so they follow target resizes.
 */
public final class GlCompat {
   private static final Map<Long, Integer> FBOS = new HashMap<>();
   private static final Map<RenderTarget, float[]> CLEAR_COLORS = new java.util.WeakHashMap<>();
   private static final Map<RenderTarget, Boolean> OWNED = new java.util.WeakHashMap<>();

   private GlCompat() {
   }

   public static int glId(GpuTexture texture) {
      return texture == null ? 0 : ((GlTexture)texture).glId();
   }

   public static int colorId(RenderTarget target) {
      return glId(target.getColorTexture());
   }

   public static int depthId(RenderTarget target) {
      return glId(target.getDepthTexture());
   }

   /** Returns a framebuffer object with the target's color (and depth) textures attached. */
   public static int fbo(RenderTarget target) {
      int color = colorId(target);
      int depth = target.hasDepth() ? depthId(target) : 0;
      long key = ((long)color << 32) | (depth & 0xFFFFFFFFL);
      Integer existing = FBOS.get(key);
      if (existing != null) {
         return existing;
      }

      int prev = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
      int fbo = GL30.glGenFramebuffers();
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
      GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, color, 0);
      if (depth != 0) {
         GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depth, 0);
      }

      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
      FBOS.put(key, fbo);
      return fbo;
   }

   public static TextureTarget newTarget(int width, int height, boolean depth) {
      TextureTarget t = new TextureTarget("astolfo", width, height, GpuFormat.RGBA8_UNORM, depth ? GpuFormat.D32_FLOAT : null);
      OWNED.put(t, Boolean.TRUE);
      return t;
   }

   public static void setClearColor(RenderTarget target, float r, float g, float b, float a) {
      CLEAR_COLORS.put(target, new float[]{r, g, b, a});
   }

   /** Clears the target's color (and depth) using the colour set through {@link #setClearColor}. */
   public static void clear(RenderTarget target) {
      int prev = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo(target));
      float[] c = CLEAR_COLORS.getOrDefault(target, new float[]{0.0F, 0.0F, 0.0F, 0.0F});
      GL11.glClearColor(c[0], c[1], c[2], c[3]);
      int mask = GL11.GL_COLOR_BUFFER_BIT;
      if (target.hasDepth()) {
         GL11.glClearDepth(0.0);
         mask |= GL11.GL_DEPTH_BUFFER_BIT;
      }

      GL11.glClear(mask);
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
   }

   public static void bindWrite(RenderTarget target, boolean setViewport) {
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo(target));
      if (setViewport) {
         GL11.glViewport(0, 0, target.width, target.height);
      }
   }
}
