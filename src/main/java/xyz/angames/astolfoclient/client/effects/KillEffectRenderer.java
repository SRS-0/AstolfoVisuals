package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
public class KillEffectRenderer {
   private final KillEffectManager manager;
   private static final Identifier BLOOM_TEXTURE = Identifier.of("astolfoclient", "textures/effects/bloom.png");

   public KillEffectRenderer(KillEffectManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      Module module = AstolfoclientClient.moduleManager.getModuleByName("KillEffect");
      if (module != null && module.isEnabled()) {
         if (!this.manager.getEffects().isEmpty()) {
            long currentTime = System.currentTimeMillis();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            Tessellator tessellator = Tessellator.getInstance();
            double camX = context.gameRenderer().getCamera().getCameraPos().x;
            double camY = context.gameRenderer().getCamera().getCameraPos().y;
            double camZ = context.gameRenderer().getCamera().getCameraPos().z;
            int rgb = ThemeManager.getThemedColor(0L);
            Color c = new Color(rgb);
            float r = c.getRed() / 255.0F;
            float g = c.getGreen() / 255.0F;
            float b = c.getBlue() / 255.0F;

            for (KillEffectManager.KillEffect effect : this.manager.getEffects()) {
               long age = currentTime - effect.startTime;
               if (age <= 3000L) {
                  float progress = (float)age / 3000.0F;
                  float globalAlpha = 1.0F - progress;
                  MatrixStack matrices = context.matrices();
                  matrices.push();
                  matrices.translate(effect.pos.x - camX, effect.pos.y - camY, effect.pos.z - camZ);
                  if (effect.mode.equals("Zap")) {
                     RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
                     float zapAlpha = 1.0F - (float)age / 800.0F;
                     if (zapAlpha > 0.0F) {
                        BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

                        for (int i = 0; i < effect.zapPoints.size() - 1; i++) {
                           Vec3d p1 = effect.zapPoints.get(i);
                           Vec3d p2 = effect.zapPoints.get(i + 1);
                           double distance = p1.distanceTo(p2);
                           int bubbles = (int)(distance / 0.25);

                           for (int j = 0; j <= bubbles; j++) {
                              float lerp = (float)j / Math.max(1, bubbles);
                              float bx = (float)(p1.x + (p2.x - p1.x) * lerp);
                              float by = (float)(p1.y + (p2.y - p1.y) * lerp);
                              float bz = (float)(p1.z + (p2.z - p1.z) * lerp);
                              matrices.push();
                              matrices.translate(bx, by, bz);
                              matrices.multiply(context.gameRenderer().getCamera().getRotation());
                              float scale = 0.6F;
                              matrices.scale(scale, scale, scale);
                              this.drawBatchedGlowingDot(matrices.peek().getPositionMatrix(), buffer, r, g, b, zapAlpha);
                              matrices.pop();
                           }
                        }

                        BufferRenderer.drawWithGlobalProgram(buffer.end());
                     }
                  } else if (effect.mode.equals("Thanos")) {
                     RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                     RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
                     Quaternionf cameraRot = context.gameRenderer().getCamera().getRotation();
                     BufferBuilder buffer = null;

                     for (KillEffectManager.ThanosParticle p : effect.thanosParticles) {
                        float currentY = p.startY;
                        float pAlpha = globalAlpha;
                        if ((float)age > p.delay) {
                           float fallTime = (float)age - p.delay;
                           currentY -= fallTime * p.fallSpeed;
                           if (currentY <= 0.0F) {
                              currentY = 0.0F;
                              pAlpha = globalAlpha * 0.6F;
                           }
                        }

                        if (!(pAlpha <= 0.05F)) {
                           if (buffer == null) {
                              buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                           }

                           matrices.push();
                           matrices.translate(p.startX, currentY, p.startZ);
                           matrices.multiply(cameraRot);
                           float pScale = 0.18F;
                           matrices.scale(pScale, pScale, pScale);
                           Matrix4f pMatrix = matrices.peek().getPositionMatrix();
                           buffer.vertex(pMatrix, -0.5F, -0.5F, 0.0F).texture(0.0F, 1.0F).color(1.0F, 1.0F, 1.0F, pAlpha);
                           buffer.vertex(pMatrix, 0.5F, -0.5F, 0.0F).texture(1.0F, 1.0F).color(1.0F, 1.0F, 1.0F, pAlpha);
                           buffer.vertex(pMatrix, 0.5F, 0.5F, 0.0F).texture(1.0F, 0.0F).color(1.0F, 1.0F, 1.0F, pAlpha);
                           buffer.vertex(pMatrix, -0.5F, 0.5F, 0.0F).texture(0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, pAlpha);
                           matrices.pop();
                        }
                     }

                     if (buffer != null) {
                        BufferRenderer.drawWithGlobalProgram(buffer.end());
                     }
                  }

                  matrices.pop();
               }
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
         }
      }
   }

   private void drawBatchedGlowingDot(Matrix4f matrix, BufferBuilder buffer, float r, float g, float b, float alpha) {
      for (int i = 0; i < 360; i += 30) {
         double rad1 = Math.toRadians(i);
         double rad2 = Math.toRadians(i + 30);
         float px1 = (float)Math.cos(rad1);
         float py1 = (float)Math.sin(rad1);
         float px2 = (float)Math.cos(rad2);
         float py2 = (float)Math.sin(rad2);
         buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).color(1.0F, 1.0F, 1.0F, alpha);
         buffer.vertex(matrix, px1, py1, 0.0F).color(r, g, b, 0.0F);
         buffer.vertex(matrix, px2, py2, 0.0F).color(r, g, b, 0.0F);
      }
   }
}
