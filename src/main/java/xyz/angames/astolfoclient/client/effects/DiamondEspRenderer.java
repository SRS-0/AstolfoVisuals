package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class DiamondEspRenderer {
   private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private final DiamondEspManager manager;

   public DiamondEspRenderer(DiamondEspManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
      if (targetEspModule != null && targetEspModule.isEnabled()) {
         if (!(targetEspModule instanceof TargetEspModule tem && !tem.mode.is("Diamond"))) {
            Map<Entity, DiamondEspManager.DiamondEffect> allEffects = this.manager.getEffects();
            if (!allEffects.isEmpty()) {
               List<DiamondEspManager.DiamondEffect> validEffects = new ArrayList<>(allEffects.values());
               if (!validEffects.isEmpty()) {
                  RenderSystem.enableBlend();
                  RenderSystem.disableCull();
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthFunc(515);
                  RenderSystem.depthMask(false);
                  RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
                  Tesselator tessellator = Tesselator.getInstance();
                  long currentTime = System.currentTimeMillis();
                  Vec3 cameraPos = context.camera().position();

                  for (DiamondEspManager.DiamondEffect effect : validEffects) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long timeSinceStart = currentTime - effect.startTime;
                        long timeSinceHit = currentTime - effect.lastHitTime;
                        if (timeSinceHit <= 450L) {
                           float fastTime = (float)timeSinceStart / 1000.0F;
                           float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
                           double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                           double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                           double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                           float entityCenterY = (float)(tY + target.getBbHeight() * 0.5F);
                           float arriveProgress = Math.min(1.0F, (float)timeSinceStart / 400.0F);
                           float arriveEase = this.easeOutCubic(arriveProgress);
                           float scatterProgress = Math.max(0.0F, ((float)timeSinceHit - 360.0F) / 90.0F);
                           float scatterEase = this.easeInQuad(Math.min(1.0F, scatterProgress));
                           float baseAlpha = arriveProgress < 1.0F ? arriveEase : 1.0F - scatterEase;
                           if (!(baseAlpha <= 0.01F)) {
                              long timeSinceAttack = currentTime - effect.lastAttackTime;
                              float hitColorFactor = Math.max(0.0F, 1.0F - (float)timeSinceAttack / 350.0F);
                              int rgb = ThemeManager.getThemedColor(0L);
                              Color themeColor = new Color(rgb);
                              float themeR = themeColor.getRed() / 255.0F;
                              float themeG = themeColor.getGreen() / 255.0F;
                              float themeB = themeColor.getBlue() / 255.0F;
                              float r = hitColorFactor * 1.0F + (1.0F - hitColorFactor) * themeR;
                              float g = hitColorFactor * 0.0F + (1.0F - hitColorFactor) * themeG;
                              float b = hitColorFactor * 0.0F + (1.0F - hitColorFactor) * themeB;
                              float cr = 1.0F;
                              float cg = 1.0F - hitColorFactor;
                              float cb = 1.0F - hitColorFactor;
                              int numDiamonds = 18;
                              float baseRadius = target.getBbWidth() * 1.1F;
                              PoseStack matrices = context.matrixStack();
                              RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
                              RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
                              BufferBuilder bbBloom = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

                              for (int i = 0; i < numDiamonds; i++) {
                                 float[] pos = this.calculateDiamondPosition(
                                    i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase
                                 );
                                 matrices.pushPose();
                                 matrices.translate(
                                    tX + pos[0] - cameraPos.x, entityCenterY + pos[1] - cameraPos.y, tZ + pos[2] - cameraPos.z
                                 );
                                 matrices.rotate(context.camera().rotation());
                                 this.drawBloom(matrices, bbBloom, 0.35F, r, g, b, baseAlpha * 0.6F * pos[3]);
                                 matrices.popPose();
                              }

                              BufferUploader.drawWithShader(bbBloom.buildOrThrow());
                              RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                              BufferBuilder bbSolid = tessellator.begin(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

                              for (int i = 0; i < numDiamonds; i++) {
                                 float[] pos = this.calculateDiamondPosition(
                                    i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase
                                 );
                                 matrices.pushPose();
                                 matrices.translate(
                                    tX + pos[0] - cameraPos.x, entityCenterY + pos[1] - cameraPos.y, tZ + pos[2] - cameraPos.z
                                 );
                                 matrices.rotate(Axis.YP.rotationDegrees(fastTime * 60.0F + i * 15.0F));
                                 matrices.rotate(Axis.XP.rotationDegrees(fastTime * 40.0F + i * 10.0F));
                                 this.draw3DDiamond(matrices, bbSolid, 0.08F, r, g, b, baseAlpha * 0.8F * pos[3]);
                                 this.draw3DDiamond(matrices, bbSolid, 0.07F, cr, cg, cb, baseAlpha * pos[3]);
                                 matrices.popPose();
                              }

                              BufferUploader.drawWithShader(bbSolid.buildOrThrow());
                              RenderSystem.lineWidth(1.5F);
                              BufferBuilder bbLines = tessellator.begin(PrimitiveTopology.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

                              for (int i = 0; i < numDiamonds; i++) {
                                 float[] pos = this.calculateDiamondPosition(
                                    i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase
                                 );
                                 matrices.pushPose();
                                 matrices.translate(
                                    tX + pos[0] - cameraPos.x, entityCenterY + pos[1] - cameraPos.y, tZ + pos[2] - cameraPos.z
                                 );
                                 matrices.rotate(Axis.YP.rotationDegrees(fastTime * 60.0F + i * 15.0F));
                                 matrices.rotate(Axis.XP.rotationDegrees(fastTime * 40.0F + i * 10.0F));
                                 this.draw3DDiamondLines(matrices, bbLines, 0.08F, r, g, b, baseAlpha * pos[3]);
                                 matrices.popPose();
                              }

                              BufferUploader.drawWithShader(bbLines.buildOrThrow());
                              RenderSystem.lineWidth(1.0F);
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

   private float[] calculateDiamondPosition(int i, int total, float fastTime, float baseRadius, float targetHeight, float arriveEase, float scatterEase) {
      float loops = 1.5F;
      float orbitSpeed = 0.8F;
      boolean isSecondSpiral = i % 2 == 1;
      int totalPerSpiral = total / 2;
      int indexInSpiral = i / 2;
      float progress = totalPerSpiral > 1 ? (float)indexInSpiral / (totalPerSpiral - 1) : 0.5F;
      float height = this.lerp(-targetHeight * 0.5F, targetHeight * 0.7F, progress);
      float helixAngle = progress * loops * 2.0F * (float) Math.PI;
      float spiralOffset = isSecondSpiral ? (float) Math.PI : 0.0F;
      float angle = helixAngle + spiralOffset + fastTime * orbitSpeed;
      float rad = baseRadius;
      float alphaMultiplier = 1.0F;
      if (arriveEase < 1.0F) {
         rad = baseRadius + (1.0F - arriveEase) * 3.0F;
         height += (1.0F - arriveEase) * 2.0F;
         alphaMultiplier = arriveEase;
      } else if (scatterEase > 0.0F) {
         rad = baseRadius + scatterEase * 3.0F;
         height += scatterEase * 2.0F;
         alphaMultiplier = 1.0F - scatterEase;
      }

      float dx = (float)Math.cos(angle) * rad;
      float dz = (float)Math.sin(angle) * rad;
      return new float[]{dx, height, dz, alphaMultiplier};
   }

   private float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private float easeOutCubic(float t) {
      return 1.0F - (float)Math.pow(1.0F - t, 3.0);
   }

   private float easeInQuad(float t) {
      return t * t;
   }

   private void drawBloom(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
      Matrix4f m = stack.last().pose();
      Vtx.vertex(buffer, m, -size, size, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, size, size, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, size, -size, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, -size, -size, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a);
   }

   private void draw3DDiamond(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
      Matrix4f m = stack.last().pose();
      float h = size * 1.8F;
      float w = size * 0.7F;
      this.drawTri(m, buffer, 0.0F, h, 0.0F, -w, 0.0F, -w, w, 0.0F, -w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, h, 0.0F, w, 0.0F, -w, w, 0.0F, w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, h, 0.0F, w, 0.0F, w, -w, 0.0F, w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, h, 0.0F, -w, 0.0F, w, -w, 0.0F, -w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, -h, 0.0F, w, 0.0F, -w, -w, 0.0F, -w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, -h, 0.0F, w, 0.0F, w, w, 0.0F, -w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, -h, 0.0F, -w, 0.0F, w, w, 0.0F, w, r, g, b, a);
      this.drawTri(m, buffer, 0.0F, -h, 0.0F, -w, 0.0F, -w, -w, 0.0F, w, r, g, b, a);
   }

   private void drawTri(
      Matrix4f m, BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float bl, float a
   ) {
      Vtx.vertex(b, m, x1, y1, z1).setColor(r, g, bl, a);
      Vtx.vertex(b, m, x2, y2, z2).setColor(r, g, bl, a);
      Vtx.vertex(b, m, x3, y3, z3).setColor(r, g, bl, a);
   }

   private void draw3DDiamondLines(PoseStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
      Matrix4f m = stack.last().pose();
      float h = size * 1.8F;
      float w = size * 0.7F;
      this.drawLine(m, buffer, -w, 0.0F, -w, w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, w, 0.0F, -w, w, 0.0F, w, r, g, b, a);
      this.drawLine(m, buffer, w, 0.0F, w, -w, 0.0F, w, r, g, b, a);
      this.drawLine(m, buffer, -w, 0.0F, w, -w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, h, 0.0F, -w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, h, 0.0F, w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, h, 0.0F, w, 0.0F, w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, h, 0.0F, -w, 0.0F, w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, -h, 0.0F, -w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, -h, 0.0F, w, 0.0F, -w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, -h, 0.0F, w, 0.0F, w, r, g, b, a);
      this.drawLine(m, buffer, 0.0F, -h, 0.0F, -w, 0.0F, w, r, g, b, a);
   }

   private void drawLine(Matrix4f m, BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float bl, float a) {
      Vtx.vertex(b, m, x1, y1, z1).setColor(r, g, bl, a);
      Vtx.vertex(b, m, x2, y2, z2).setColor(r, g, bl, a);
   }
}
