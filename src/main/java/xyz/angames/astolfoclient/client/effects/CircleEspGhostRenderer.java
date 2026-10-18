package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class CircleEspGhostRenderer {
   public void render(WorldRenderContext context, List<CircleEspManager.CircleEspEffect> effects) {
      if (!effects.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         Tesselator tessellator = Tesselator.getInstance();
         long currentTime = System.currentTimeMillis();
         PoseStack matrices = context.matrixStack();
         double camX = context.camera().position().x;
         double camY = context.camera().position().y;
         double camZ = context.camera().position().z;
         Quaternionf cameraRot = context.camera().rotation();

         for (CircleEspManager.CircleEspEffect effect : effects) {
            Entity target = effect.target;
            if (target.isAlive() && !TargetUtils.isInvisible(target)) {
               long timeSinceHit = currentTime - effect.lastHitTime;
               float fadeProgress = Mth.clamp((float)timeSinceHit / 450.0F, 0.0F, 1.0F);
               float baseAlpha = 1.0F - Math.max(0.0F, (fadeProgress - 0.5F) * 2.0F);
               if (!(baseAlpha <= 0.05F)) {
                  float animTime = (float)(currentTime - effect.startTime) / 1000.0F;
                  float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
                  double tX = Mth.lerp(tickDelta, target.xOld, target.getX()) - camX;
                  double tY = Mth.lerp(tickDelta, target.yOld, target.getY()) - camY;
                  double tZ = Mth.lerp(tickDelta, target.zOld, target.getZ()) - camZ;
                  float height = target.getBbHeight();
                  float radius = target.getBbWidth() / 2.0F + 0.1F;
                  if (timeSinceHit > 400L) {
                     float endProgress = (float)(timeSinceHit - 400L) / 200.0F;
                     radius *= Math.max(0.0F, 1.0F - endProgress);
                  }

                  float speedY = 3.5F;
                  RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                  BufferBuilder buffer = tessellator.begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
                  float mainY = (float)((Math.sin(animTime * speedY) + 1.0) / 2.0) * height;
                  Color headColor = new Color(ThemeManager.getThemedColor(0L));
                  float rH = headColor.getRed() / 255.0F;
                  float gH = headColor.getGreen() / 255.0F;
                  float bH = headColor.getBlue() / 255.0F;

                  for (int angle = 0; angle < 360; angle += 10) {
                     double rad = Math.toRadians(angle);
                     float pX = (float)Math.cos(rad) * radius;
                     float pZ = (float)Math.sin(rad) * radius;
                     matrices.pushPose();
                     matrices.translate(tX + pX, tY + mainY, tZ + pZ);
                     matrices.rotate(cameraRot);
                     float scale = 0.12F;
                     matrices.scale(scale, scale, scale);
                     this.drawBatchedGlowingDot(matrices.last().pose(), buffer, rH, gH, bH, baseAlpha);
                     matrices.popPose();
                  }

                  int tailSteps = 22;

                  for (int i = 1; i <= tailSteps; i++) {
                     float tHist = animTime - i * 0.025F;
                     if (!(tHist < 0.0F)) {
                        float histY = (float)((Math.sin(tHist * speedY) + 1.0) / 2.0) * height;
                        float tailProgress = (float)i / tailSteps;
                        float alphaMultiplier = (float)Math.pow(1.0 - tailProgress, 2.0);
                        float tailAlpha = baseAlpha * alphaMultiplier * 0.6F;
                        float tailRadius = radius * (1.0F - tailProgress * 0.15F);
                        Color tColor = new Color(ThemeManager.getThemedColor(i * 15));
                        float rT = tColor.getRed() / 255.0F;
                        float gT = tColor.getGreen() / 255.0F;
                        float bT = tColor.getBlue() / 255.0F;

                        for (int angle = 0; angle < 360; angle += 12) {
                           double rad = Math.toRadians(angle);
                           float pX = (float)Math.cos(rad) * tailRadius;
                           float pZ = (float)Math.sin(rad) * tailRadius;
                           matrices.pushPose();
                           matrices.translate(tX + pX, tY + histY, tZ + pZ);
                           matrices.rotate(cameraRot);
                           float scale = 0.12F * (1.0F - tailProgress * 0.5F);
                           matrices.scale(scale, scale, scale);
                           this.drawBatchedGlowingDot(matrices.last().pose(), buffer, rT, gT, bT, tailAlpha);
                           matrices.popPose();
                        }
                     }
                  }

                  BufferUploader.drawWithShader(buffer.buildOrThrow());
               }
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void drawBatchedGlowingDot(Matrix4f matrix, BufferBuilder buffer, float r, float g, float b, float alpha) {
      float coreR = r * 0.15F + 0.85F;
      float coreG = g * 0.15F + 0.85F;
      float coreB = b * 0.15F + 0.85F;

      for (int i = 0; i < 360; i += 30) {
         double rad1 = Math.toRadians(i);
         double rad2 = Math.toRadians(i + 30);
         float px1 = (float)Math.cos(rad1);
         float py1 = (float)Math.sin(rad1);
         float px2 = (float)Math.cos(rad2);
         float py2 = (float)Math.sin(rad2);
         Vtx.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F).setColor(coreR, coreG, coreB, alpha);
         Vtx.vertex(buffer, matrix, px1, py1, 0.0F).setColor(r, g, b, 0.0F);
         Vtx.vertex(buffer, matrix, px2, py2, 0.0F).setColor(r, g, b, 0.0F);
      }
   }
}
