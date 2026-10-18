package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class GhostEspRenderer {
   private final GhostEspManager manager;

   public GhostEspRenderer(GhostEspManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
      if (targetEspModule != null && targetEspModule.isEnabled()) {
         if (!(targetEspModule instanceof TargetEspModule tem && !tem.mode.is("Ghost"))) {
            Map<Entity, GhostEspEffect> allEffects = this.manager.getEffects();
            if (!allEffects.isEmpty()) {
               List<GhostEspEffect> validEffects = new ArrayList<>();
               long currentTime = System.currentTimeMillis();

               for (GhostEspEffect effect : allEffects.values()) {
                  if (effect.target != null && effect.target.isAlive() && !TargetUtils.isInvisible(effect.target)) {
                     long age = currentTime - effect.lastHitTime;
                     if (age <= 450L) {
                        validEffects.add(effect);
                     }
                  }
               }

               if (!validEffects.isEmpty()) {
                  RenderSystem.enableBlend();
                  RenderSystem.disableCull();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
                  RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                  Tesselator tessellator = Tesselator.getInstance();
                  double safeTime = currentTime % 1000000L;
                  float speed = 0.006F;
                  int trailLength = 22;
                  float baseDistanceMultiplier = 1.05F;
                  double spacingDegrees = 120.0;
                  float trailSegmentSpacing = 10.0F;

                  for (GhostEspEffect effect : validEffects) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long age = currentTime - effect.lastHitTime;
                        float lifeProgress = (float)age / 450.0F;
                        float baseAlpha = 1.0F - Math.max(0.0F, (lifeProgress - 0.5F) * 2.0F);
                        if (!(baseAlpha <= 0.05F)) {
                           int rgb = ThemeManager.getThemedColor(0L);
                           float themeR = (rgb >> 16 & 0xFF) / 255.0F;
                           float themeG = (rgb >> 8 & 0xFF) / 255.0F;
                           float themeB = (rgb & 0xFF) / 255.0F;
                           long ageSinceAttack = currentTime - effect.lastAttackTime;
                           float hitColorFactor = Math.max(0.0F, 1.0F - (float)ageSinceAttack / 350.0F);
                           float r = hitColorFactor * 1.0F + (1.0F - hitColorFactor) * themeR;
                           float g = hitColorFactor * 0.0F + (1.0F - hitColorFactor) * themeG;
                           float b = hitColorFactor * 0.0F + (1.0F - hitColorFactor) * themeB;
                           float cr = 1.0F;
                           float cg = 1.0F - hitColorFactor;
                           float cb = 1.0F - hitColorFactor;
                           float baseRadius = target.getBbWidth() * baseDistanceMultiplier;
                           if (age > 400L) {
                              float endProgress = (float)(age - 400L) / 200.0F;
                              baseRadius *= Math.max(0.0F, 1.0F - endProgress);
                           }

                           float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
                           double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                           double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                           double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                           double camX = context.camera().position().x;
                           double camY = context.camera().position().y;
                           double camZ = context.camera().position().z;

                           for (int j = 0; j < 3; j++) {
                              float breathing = (float)Math.sin(safeTime * 0.002 + j * 1.5);
                              float currentRadius = baseRadius + breathing * 0.2F;

                              for (int t = 0; t < trailLength; t++) {
                                 float decay = 1.0F / trailLength;
                                 float scale = (1.0F - t * decay) * 0.2F;
                                 float alpha = baseAlpha * (1.0F - t * decay);
                                 if (!(scale <= 0.01F) && !(alpha <= 0.02F)) {
                                    float timeOffset = t * trailSegmentSpacing;
                                    float histSafeTime = (float)(safeTime - timeOffset);
                                    float angle = histSafeTime * speed + (float)(j * Math.toRadians(spacingDegrees));
                                    float localX = (float)Math.cos(angle) * currentRadius;
                                    float localY = 0.0F;
                                    float localZ = (float)Math.sin(angle) * currentRadius;
                                    float histAnimTime = histSafeTime / 1000.0F;
                                    float scanSpeed = 2.0F;
                                    float phase = (float)Math.sin(histAnimTime * scanSpeed);
                                    float scanY = (phase + 1.0F) / 2.0F * target.getBbHeight();
                                    PoseStack matrices = context.matrixStack();
                                    matrices.pushPose();
                                    matrices.translate(tX + localX - camX, tY + scanY + localY - camY, tZ + localZ - camZ);
                                    matrices.rotate(context.camera().rotation());
                                    matrices.scale(scale, scale, scale);
                                    BufferBuilder buffer = tessellator.begin(PrimitiveTopology.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                                    this.drawGlowingDot(matrices.last().pose(), buffer, r, g, b, cr, cg, cb, alpha);
                                    BufferUploader.drawWithShader(buffer.buildOrThrow());
                                    matrices.popPose();
                                 }
                              }
                           }
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
         }
      }
   }

   private void drawGlowingDot(Matrix4f matrix, BufferBuilder buffer, float r, float g, float b, float cr, float cg, float cb, float alpha) {
      Vtx.vertex(buffer, matrix, 0.0F, 0.0F, 0.0F).setColor(cr, cg, cb, alpha);

      for (int i = 0; i <= 360; i += 20) {
         double rad = Math.toRadians(i);
         float px = (float)Math.cos(rad);
         float py = (float)Math.sin(rad);
         Vtx.vertex(buffer, matrix, px, py, 0.0F).setColor(r, g, b, 0.0F);
      }
   }
}
