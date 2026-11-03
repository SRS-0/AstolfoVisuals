package xyz.angames.astolfoclient.client.effects;

import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class TargetEspRenderer {
   private static final Identifier TEXTURE_DEFAULT = Identifier.of("astolfoclient", "textures/effects/target_esp.png");
   private static final Identifier TEXTURE_BO = Identifier.of("astolfoclient", "textures/effects/target_esp_cube/target_bo.png");
   private static final Identifier TEXTURE_FRAME = Identifier.of("astolfoclient", "textures/effects/target_esp_cube/target_frame.png");
   private static final Identifier TEXTURE_JEKA = Identifier.of("astolfoclient", "textures/effects/target_esp_cube/target_jeka.png");
   private static final Identifier TEXTURE_VEGAS = Identifier.of("astolfoclient", "textures/effects/target_esp_cube/target_vegas.png");
   private final TargetEspManager manager;
   private static final float BASE_ROTATION_SPEED = 0.15F;
   private Framebuffer distortionFbo = null;
   private int distortionProgram = -1;
   private int distortionVao = -1;
   private int distortionVbo = -1;

   public TargetEspRenderer(TargetEspManager manager) {
      this.manager = manager;
   }

   private Identifier getCubeTexture(TargetEspModule module) {
      String var2 = module.cubeTexture.get();

      return switch (var2) {
         case "Rounded", "target_bo.png", "Bo" -> TEXTURE_BO;
         case "Frame", "target_frame.png" -> TEXTURE_FRAME;
         case "Jeka", "target_jeka.png" -> TEXTURE_JEKA;
         case "Vegas", "target_vegas.png" -> TEXTURE_VEGAS;
         default -> TEXTURE_DEFAULT;
      };
   }

   public void render(WorldRenderContext context) {
      Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
      if (targetEspModule != null && targetEspModule.isEnabled()) {
         if (targetEspModule instanceof TargetEspModule tem) {
            if (tem.distortion.get()) {
               this.renderDistortionShader(context, tem);
            }

            if (tem.mode.is("Cube")) {
               this.renderCubeMeshes(context, tem);
            }
         }
      }
   }

   private void renderDistortionShader(WorldRenderContext context, TargetEspModule tem) {var mainFbo=MinecraftClient.getInstance().getFramebuffer();int width=mainFbo.textureWidth,height=mainFbo.textureHeight;var scene=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();var pass=new xyz.angames.astolfoclient.client.render.compat.ScreenPass("target_distortion").texture(0,scene.getColorAttachmentView(),true);Matrix4f viewRotMat = new Matrix4f().rotation(new Quaternionf(context.gameRenderer().getCamera().getRotation()).conjugate());
                        Matrix4f projMat = new Matrix4f(RenderSystem.getProjectionMatrix());
                        Matrix4f viewProjMat = new Matrix4f(projMat).mul(viewRotMat);
                        float[] viewProjArr = new float[16];
                        viewProjMat.get(viewProjArr);
                        pass.uniform("uViewProjMat", viewProjArr);
                        pass.uniform("uResolution", width, height);
                        pass.uniform("uDistortionStrength", (float)tem.distortionStrength.get());
                        pass.uniform("uChromatic", tem.chromatic.get() ? 1 : 0);
                        var nodes=collectDistortionNodes(context,tem);
                        int maxNodes = 36;
                        int count = 0;

                        for (TargetEspRenderer.DistortionNode n : nodes) {
                           if (count >= maxNodes) {
                              break;
                           }

                           pass.uniform("uNodePos[" + count + "]", n.x, n.y, n.z);
                           pass.uniform("uNodeRadius[" + count + "]", n.radius);
                           pass.uniform("uNodeAlpha[" + count + "]", n.alpha);
                           count++;
                        }

                        pass.uniform("uNodeCount", count);
                        
pass.draw(mainFbo);}

   private float[] calculateDiamondPos(int i, int total, float fastTime, float baseRadius, float targetHeight, float arriveEase, float scatterEase) {
      float loops = 1.5F;
      float orbitSpeed = 0.8F;
      boolean isSecondSpiral = i % 2 == 1;
      int totalPerSpiral = total / 2;
      int indexInSpiral = i / 2;
      float progress = totalPerSpiral > 1 ? (float)indexInSpiral / (totalPerSpiral - 1) : 0.5F;
      float height = -targetHeight * 0.5F + targetHeight * 1.2F * progress;
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

   private void renderCubeMeshes(WorldRenderContext context, TargetEspModule tem) {
      Map<Entity, TargetEspEffect> effects = this.manager.getEffects();
      if (!effects.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         Identifier selectedCubeTexture = this.getCubeTexture(tem);
         RenderSystem.setShaderTexture(0, selectedCubeTexture);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         Tessellator tessellator = Tessellator.getInstance();
         float tickDelta = net.minecraft.client.MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
         long currentTime = System.currentTimeMillis();
         int rgb = ThemeManager.getThemedColor(0L);
         float r = (rgb >> 16 & 0xFF) / 255.0F;
         float g = (rgb >> 8 & 0xFF) / 255.0F;
         float b = (rgb & 0xFF) / 255.0F;

         for (TargetEspEffect effect : effects.values()) {
            Entity target = effect.target;
            if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
               long age = currentTime - effect.lastHitTime;
               if (age <= 450L) {
                  float progress = (float)age / 450.0F;
                  float alpha = 1.0F - Math.max(0.0F, (progress - 0.5F) * 2.0F);
                  long deltaTime = effect.lastRenderTime == 0L ? 0L : currentTime - effect.lastRenderTime;
                  effect.lastRenderTime = currentTime;
                  effect.currentAngle += 0.15F * (float)deltaTime;
                  double lerpX = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
                  double lerpY = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
                  double lerpZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
                  double centerY = lerpY + target.getHeight() / 2.0F;
                  MatrixStack matrixStack = context.matrices();
                  matrixStack.push();
                  matrixStack.translate(lerpX - context.gameRenderer().getCamera().getCameraPos().x, centerY - context.gameRenderer().getCamera().getCameraPos().y, lerpZ - context.gameRenderer().getCamera().getCameraPos().z);
                  matrixStack.multiply(context.gameRenderer().getCamera().getRotation());
                  matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(effect.currentAngle));
                  float scaleProgress = 1.0F;
                  long timeSinceStart = currentTime - effect.startTime;
                  if (timeSinceStart < 200L) {
                     scaleProgress = (float)timeSinceStart / 200.0F;
                  }

                  if (age > 400L) {
                     float endProgress = (float)(age - 400L) / 200.0F;
                     scaleProgress = Math.max(0.0F, 1.0F - endProgress);
                  }

                  float scale = target.getWidth() * 2.5F * scaleProgress;
                  matrixStack.scale(scale, scale, scale);
                  Matrix4f matrix = matrixStack.peek().getPositionMatrix();
                  BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  buffer.vertex(matrix, -0.5F, -0.5F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, alpha);
                  buffer.vertex(matrix, -0.5F, 0.5F, 0.0F).texture(0.0F, 1.0F).color(r, g, b, alpha);
                  buffer.vertex(matrix, 0.5F, 0.5F, 0.0F).texture(1.0F, 1.0F).color(r, g, b, alpha);
                  buffer.vertex(matrix, 0.5F, -0.5F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, alpha);
                  BufferRenderer.drawWithGlobalProgram(buffer.end());
                  matrixStack.pop();
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

   

   

   

   

   

   

   

   

   

   @Environment(EnvType.CLIENT)
   private static class DistortionNode {
      final float x;
      final float y;
      final float z;
      final float radius;
      final float alpha;

      DistortionNode(float x, float y, float z, float radius, float alpha) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.radius = radius;
         this.alpha = alpha;
      }
   }
 private List<DistortionNode> collectDistortionNodes(WorldRenderContext context,TargetEspModule tem) {var mc=MinecraftClient.getInstance();if(mc.player==null)return List.of();               double camX = context.gameRenderer().getCamera().getCameraPos().x;
               double camY = context.gameRenderer().getCamera().getCameraPos().y;
               double camZ = context.gameRenderer().getCamera().getCameraPos().z;
               float tickDelta = net.minecraft.client.MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(true);
               long currentTime = System.currentTimeMillis();
               String mode = tem.mode.get();
               List<TargetEspRenderer.DistortionNode> nodes = new ArrayList<>();
               if ("Cube".equals(mode)) {
                  for (TargetEspEffect effect : this.manager.getEffects().values()) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long age = currentTime - effect.lastHitTime;
                        if (age <= 450L) {
                           float progress = (float)age / 450.0F;
                           float alpha = 1.0F - Math.max(0.0F, (progress - 0.5F) * 2.0F);
                           if (!(alpha <= 0.01F)) {
                              double lerpX = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
                              double lerpY = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
                              double lerpZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
                              double centerY = lerpY + target.getHeight() / 2.0F;
                              float tRadius = Math.max(target.getWidth() * 1.3F, target.getHeight() * 0.7F);
                              nodes.add(
                                 new TargetEspRenderer.DistortionNode((float)(lerpX - camX), (float)(centerY - camY), (float)(lerpZ - camZ), tRadius, alpha)
                              );
                           }
                        }
                     }
                  }
               } else if ("Diamond".equals(mode) && AstolfoclientClient.diamondEspManager != null) {
                  for (DiamondEspManager.DiamondEffect effectx : AstolfoclientClient.diamondEspManager.getEffects().values()) {
                     Entity target = effectx.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long timeSinceStart = currentTime - effectx.startTime;
                        long timeSinceHit = currentTime - effectx.lastHitTime;
                        if (timeSinceHit <= 450L) {
                           float fastTime = (float)timeSinceStart / 1000.0F;
                           double tX = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
                           double tY = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
                           double tZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
                           float entityCenterY = (float)(tY + target.getHeight() * 0.5F);
                           float arriveProgress = Math.min(1.0F, (float)timeSinceStart / 400.0F);
                           float arriveEase = 1.0F - (float)Math.pow(1.0F - arriveProgress, 3.0);
                           float scatterProgress = Math.max(0.0F, ((float)timeSinceHit - 360.0F) / 90.0F);
                           float scatterEase = scatterProgress * scatterProgress;
                           float baseAlpha = arriveProgress < 1.0F ? arriveEase : 1.0F - scatterEase;
                           if (!(baseAlpha <= 0.01F)) {
                              int numDiamonds = 18;
                              float baseRadius = target.getWidth() * 1.1F;

                              for (int i = 0; i < numDiamonds; i++) {
                                 float[] pos = this.calculateDiamondPos(i, numDiamonds, fastTime, baseRadius, target.getHeight(), arriveEase, scatterEase);
                                 float dX = (float)(tX + pos[0] - camX);
                                 float dY = (float)(entityCenterY + pos[1] - camY);
                                 float dZ = (float)(tZ + pos[2] - camZ);
                                 float dAlpha = baseAlpha * pos[3];
                                 nodes.add(new TargetEspRenderer.DistortionNode(dX, dY, dZ, 0.65F, dAlpha));
                              }
                           }
                        }
                     }
                  }
               } else if ("Circle".equals(mode) && AstolfoclientClient.circleEspManager != null) {
                  boolean isMimbran = "Mimbran".equals(tem.circleMode.get());

                  for (CircleEspManager.CircleEspEffect effectxx : AstolfoclientClient.circleEspManager.getEffects().values()) {
                     Entity target = effectxx.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long timeSinceHit = currentTime - effectxx.lastHitTime;
                        float fadeProgress = (float)timeSinceHit / 450.0F;
                        float alpha = 1.0F - Math.max(0.0F, (fadeProgress - 0.5F) * 2.0F);
                        if (!(alpha <= 0.05F)) {
                           float animTime = (float)(currentTime - effectxx.startTime) / 1000.0F;
                           double tX = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
                           double tY = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
                           double tZ = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
                           float targetH = target.getHeight();
                           float radius;
                           float scanY;
                           if (isMimbran) {
                              float speedY = 3.5F;
                              scanY = (float)((Math.sin(animTime * speedY) + 1.0) / 2.0) * targetH;
                              radius = target.getWidth() / 2.0F + 0.1F;
                           } else {
                              float scanSpeed = 3.0F;
                              float phase = (float)Math.sin(animTime * scanSpeed);
                              scanY = (phase + 1.0F) / 2.0F * targetH;
                              radius = target.getWidth() * 0.8F;
                           }

                           if (timeSinceHit > 400L) {
                              float endProgress = (float)(timeSinceHit - 400L) / 200.0F;
                              radius *= Math.max(0.0F, 1.0F - endProgress);
                           }

                           for (int k = 0; k < 16; k++) {
                              float ringAngle = (float)(k * (Math.PI / 8));
                              float rX = (float)(tX + Math.cos(ringAngle) * radius - camX);
                              float rY = (float)(tY + scanY - camY);
                              float rZ = (float)(tZ + Math.sin(ringAngle) * radius - camZ);
                              nodes.add(new TargetEspRenderer.DistortionNode(rX, rY, rZ, 0.45F, alpha));
                           }
                        }
                     }
                  }
               } else if ("Ghost".equals(mode) && AstolfoclientClient.ghostEspManager != null) {
                  double safeTime = currentTime % 1000000L;
                  float speed = 0.006F;
                  float baseDistanceMultiplier = 1.05F;
                  double spacingDegrees = 120.0;
                  float trailSegmentSpacing = 10.0F;
                  int trailLength = 22;

                  for (GhostEspEffect effectxxx : AstolfoclientClient.ghostEspManager.getEffects().values()) {
                     Entity target = effectxxx.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long age = currentTime - effectxxx.lastHitTime;
                        float lifeProgress = (float)age / 450.0F;
                        float baseAlpha = 1.0F - Math.max(0.0F, (lifeProgress - 0.5F) * 2.0F);
                        if (!(baseAlpha <= 0.05F)) {
                           double tXx = target.lastRenderX + (target.getX() - target.lastRenderX) * tickDelta;
                           double tYx = target.lastRenderY + (target.getY() - target.lastRenderY) * tickDelta;
                           double tZx = target.lastRenderZ + (target.getZ() - target.lastRenderZ) * tickDelta;
                           float baseRadius = target.getWidth() * baseDistanceMultiplier;
                           if (age > 400L) {
                              float endProgress = (float)(age - 400L) / 200.0F;
                              baseRadius *= Math.max(0.0F, 1.0F - endProgress);
                           }

                           for (int j = 0; j < 3; j++) {
                              float breathing = (float)Math.sin(safeTime * 0.002 + j * 1.5);
                              float currentRadius = baseRadius + breathing * 0.2F;
                              float angle = (float)(safeTime * speed) + (float)(j * Math.toRadians(spacingDegrees));
                              float localX = (float)Math.cos(angle) * currentRadius;
                              float localZ = (float)Math.sin(angle) * currentRadius;
                              float histAnimTime = (float)(safeTime / 1000.0);
                              float scanSpeed = 2.0F;
                              float phase = (float)Math.sin(histAnimTime * scanSpeed);
                              float scanYx = (phase + 1.0F) / 2.0F * target.getHeight();
                              float gX = (float)(tXx + localX - camX);
                              float gY = (float)(tYx + scanYx - camY);
                              float gZ = (float)(tZx + localZ - camZ);
                              nodes.add(new TargetEspRenderer.DistortionNode(gX, gY, gZ, 0.55F, baseAlpha));

                              for (int t : new int[]{6, 12}) {
                                 float decay = 1.0F / trailLength;
                                 float tAlpha = baseAlpha * (1.0F - t * decay);
                                 if (!(tAlpha <= 0.05F)) {
                                    float timeOffset = t * trailSegmentSpacing;
                                    float histSafeTime = (float)(safeTime - timeOffset);
                                    float tAngle = histSafeTime * speed + (float)(j * Math.toRadians(spacingDegrees));
                                    float tLocalX = (float)Math.cos(tAngle) * currentRadius;
                                    float tLocalZ = (float)Math.sin(tAngle) * currentRadius;
                                    float tAnimTime = histSafeTime / 1000.0F;
                                    float tPhase = (float)Math.sin(tAnimTime * scanSpeed);
                                    float tScanY = (tPhase + 1.0F) / 2.0F * target.getHeight();
                                    float tgX = (float)(tXx + tLocalX - camX);
                                    float tgY = (float)(tYx + tScanY - camY);
                                    float tgZ = (float)(tZx + tLocalZ - camZ);
                                    nodes.add(new TargetEspRenderer.DistortionNode(tgX, tgY, tgZ, 0.4F, tAlpha * 0.7F));
                                 }
                              }
                           }
                        }
                     }
                  }
               }


return nodes;
}
}
