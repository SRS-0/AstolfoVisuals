package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.GlCompat;
import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.entity.Entity;
import com.mojang.blaze3d.pipeline.RenderTarget;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import xyz.angames.astolfoclient.client.compat.GL20;
import org.lwjgl.opengl.GL30;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class TargetEspRenderer {
   private static final Identifier TEXTURE_DEFAULT = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/target_esp.png");
   private static final Identifier TEXTURE_BO = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/target_esp_cube/target_bo.png");
   private static final Identifier TEXTURE_FRAME = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/target_esp_cube/target_frame.png");
   private static final Identifier TEXTURE_JEKA = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/target_esp_cube/target_jeka.png");
   private static final Identifier TEXTURE_VEGAS = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/target_esp_cube/target_vegas.png");
   private final TargetEspManager manager;
   private static final float BASE_ROTATION_SPEED = 0.15F;
   private RenderTarget distortionFbo = null;
   private int distortionProgram = -1;
   private int distortionVao = -1;
   private int distortionVbo = -1;

   public TargetEspRenderer(TargetEspManager manager) {
      this.manager = manager;
   }

   private Identifier getCubeTexture(TargetEspModule module) {
      return switch (module.cubeTexture.get()) {
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

   private void renderDistortionShader(WorldRenderContext context, TargetEspModule tem) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && context.camera() != null) {
         RenderTarget mainFbo = mc.gameRenderer.mainRenderTarget();
         if (mainFbo != null) {
            int width = mainFbo.width;
            int height = mainFbo.height;
            if (width > 0 && height > 0) {
               double camX = context.camera().position().x;
               double camY = context.camera().position().y;
               double camZ = context.camera().position().z;
               float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
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
                              double lerpX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                              double lerpY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                              double lerpZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                              double centerY = lerpY + target.getBbHeight() / 2.0F;
                              float tRadius = Math.max(target.getBbWidth() * 1.3F, target.getBbHeight() * 0.7F);
                              nodes.add(
                                 new TargetEspRenderer.DistortionNode((float)(lerpX - camX), (float)(centerY - camY), (float)(lerpZ - camZ), tRadius, alpha)
                              );
                           }
                        }
                     }
                  }
               } else if ("Diamond".equals(mode) && AstolfoclientClient.diamondEspManager != null) {
                  for (DiamondEspManager.DiamondEffect effect : AstolfoclientClient.diamondEspManager.getEffects().values()) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long timeSinceStart = currentTime - effect.startTime;
                        long timeSinceHit = currentTime - effect.lastHitTime;
                        if (timeSinceHit <= 450L) {
                           float fastTime = (float)timeSinceStart / 1000.0F;
                           double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                           double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                           double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                           float entityCenterY = (float)(tY + target.getBbHeight() * 0.5F);
                           float arriveProgress = Math.min(1.0F, (float)timeSinceStart / 400.0F);
                           float arriveEase = 1.0F - (float)Math.pow(1.0F - arriveProgress, 3.0);
                           float scatterProgress = Math.max(0.0F, ((float)timeSinceHit - 360.0F) / 90.0F);
                           float scatterEase = scatterProgress * scatterProgress;
                           float baseAlpha = arriveProgress < 1.0F ? arriveEase : 1.0F - scatterEase;
                           if (!(baseAlpha <= 0.01F)) {
                              int numDiamonds = 18;
                              float baseRadius = target.getBbWidth() * 1.1F;

                              for (int i = 0; i < numDiamonds; i++) {
                                 float[] pos = this.calculateDiamondPos(i, numDiamonds, fastTime, baseRadius, target.getBbHeight(), arriveEase, scatterEase);
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

                  for (CircleEspManager.CircleEspEffect effect : AstolfoclientClient.circleEspManager.getEffects().values()) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long timeSinceHit = currentTime - effect.lastHitTime;
                        float fadeProgress = (float)timeSinceHit / 450.0F;
                        float alpha = 1.0F - Math.max(0.0F, (fadeProgress - 0.5F) * 2.0F);
                        if (!(alpha <= 0.05F)) {
                           float animTime = (float)(currentTime - effect.startTime) / 1000.0F;
                           double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                           double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                           double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                           float targetH = target.getBbHeight();
                           float radius;
                           float scanY;
                           if (isMimbran) {
                              float speedY = 3.5F;
                              scanY = (float)((Math.sin(animTime * speedY) + 1.0) / 2.0) * targetH;
                              radius = target.getBbWidth() / 2.0F + 0.1F;
                           } else {
                              float scanSpeed = 3.0F;
                              float phase = (float)Math.sin(animTime * scanSpeed);
                              scanY = (phase + 1.0F) / 2.0F * targetH;
                              radius = target.getBbWidth() * 0.8F;
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

                  for (GhostEspEffect effect : AstolfoclientClient.ghostEspManager.getEffects().values()) {
                     Entity target = effect.target;
                     if (target != null && target.isAlive() && !TargetUtils.isInvisible(target)) {
                        long age = currentTime - effect.lastHitTime;
                        float lifeProgress = (float)age / 450.0F;
                        float baseAlpha = 1.0F - Math.max(0.0F, (lifeProgress - 0.5F) * 2.0F);
                        if (!(baseAlpha <= 0.05F)) {
                           double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                           double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                           double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                           float baseRadius = target.getBbWidth() * baseDistanceMultiplier;
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
                              float scanY = (phase + 1.0F) / 2.0F * target.getBbHeight();
                              float gX = (float)(tX + localX - camX);
                              float gY = (float)(tY + scanY - camY);
                              float gZ = (float)(tZ + localZ - camZ);
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
                                    float tScanY = (tPhase + 1.0F) / 2.0F * target.getBbHeight();
                                    float tgX = (float)(tX + tLocalX - camX);
                                    float tgY = (float)(tY + tScanY - camY);
                                    float tgZ = (float)(tZ + tLocalZ - camZ);
                                    nodes.add(new TargetEspRenderer.DistortionNode(tgX, tgY, tgZ, 0.4F, tAlpha * 0.7F));
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               if (!nodes.isEmpty()) {
                  this.initDistortionShader();
                  if (this.distortionProgram != -1) {
                     this.initDistortionQuad();
                     if (this.distortionVao != -1) {
                        this.ensureDistortionFbo(width, height);
                        int prevReadFbo = GL11.glGetInteger(36010);
                        int prevDrawFbo = GL11.glGetInteger(36006);
                        GL30.glBindFramebuffer(36008, GlCompat.fbo(mainFbo));
                        GL30.glBindFramebuffer(36009, GlCompat.fbo(this.distortionFbo));
                        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, 16384, 9728);
                        GL30.glBindFramebuffer(36008, prevReadFbo);
                        GL30.glBindFramebuffer(36009, prevDrawFbo);
                        GL30.glBindFramebuffer(36160, GlCompat.fbo(mainFbo));
                        int prevProgram = GL11.glGetInteger(35725);
                        int prevVao = GL11.glGetInteger(34229);
                        int prevVbo = GL11.glGetInteger(34964);
                        int prevActiveTex = GL11.glGetInteger(34016);
                        GL13.glActiveTexture(33984);
                        int prevTex0 = GL11.glGetInteger(32873);
                        boolean prevBlend = GL11.glIsEnabled(3042);
                        boolean prevDepthTest = GL11.glIsEnabled(2929);
                        boolean prevCull = GL11.glIsEnabled(2884);
                        boolean prevDepthMask = GL11.glGetBoolean(2930);
                        RenderSystem.disableDepthTest();
                        RenderSystem.depthMask(false);
                        RenderSystem.disableCull();
                        RenderSystem.disableBlend();
                        GL20.glUseProgram(this.distortionProgram);
                        Matrix4f viewRotMat = new Matrix4f().rotation(new Quaternionf(context.camera().rotation()).conjugate());
                        Matrix4f projMat = new Matrix4f(context.projectionMatrix());
                        Matrix4f viewProjMat = new Matrix4f(projMat).mul(viewRotMat);
                        float[] viewProjArr = new float[16];
                        viewProjMat.get(viewProjArr);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, GlCompat.colorId(this.distortionFbo));
                        this.setUniform1i("uColorTexture", 0);
                        this.setUniformMatrix4fv("uViewProjMat", viewProjArr);
                        this.setUniform2f("uResolution", width, height);
                        this.setUniform1f("uDistortionStrength", (float)tem.distortionStrength.get());
                        this.setUniform1i("uChromatic", tem.chromatic.get() ? 1 : 0);
                        int maxNodes = 36;
                        int count = 0;

                        for (TargetEspRenderer.DistortionNode n : nodes) {
                           if (count >= maxNodes) {
                              break;
                           }

                           this.setUniform3f("uNodePos[" + count + "]", n.x, n.y, n.z);
                           this.setUniform1f("uNodeRadius[" + count + "]", n.radius);
                           this.setUniform1f("uNodeAlpha[" + count + "]", n.alpha);
                           count++;
                        }

                        this.setUniform1i("uNodeCount", count);
                        GL30.glBindVertexArray(this.distortionVao);
                        GL11.glDrawArrays(4, 0, 6);
                        GL30.glBindVertexArray(prevVao);
                        GL15.glBindBuffer(34962, prevVbo);
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(3553, prevTex0);
                        RenderSystem.setShaderTexture(0, prevTex0);
                        GL13.glActiveTexture(prevActiveTex);
                        GL20.glUseProgram(prevProgram);
                        if (prevDepthTest) {
                           RenderSystem.enableDepthTest();
                        }

                        if (prevCull) {
                           RenderSystem.enableCull();
                        }

                        if (prevBlend) {
                           RenderSystem.enableBlend();
                        }

                        RenderSystem.depthMask(prevDepthMask);
                     }
                  }
               }
            }
         }
      }
   }

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
         RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
         Tesselator tessellator = Tesselator.getInstance();
         float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
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
                  double lerpX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                  double lerpY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                  double lerpZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                  double centerY = lerpY + target.getBbHeight() / 2.0F;
                  PoseStack matrixStack = context.matrixStack();
                  matrixStack.pushPose();
                  matrixStack.translate(
                     lerpX - context.camera().position().x,
                     centerY - context.camera().position().y,
                     lerpZ - context.camera().position().z
                  );
                  matrixStack.rotate(context.camera().rotation());
                  matrixStack.rotate(Axis.ZP.rotationDegrees(effect.currentAngle));
                  float scaleProgress = 1.0F;
                  long timeSinceStart = currentTime - effect.startTime;
                  if (timeSinceStart < 200L) {
                     scaleProgress = (float)timeSinceStart / 200.0F;
                  }

                  if (age > 400L) {
                     float endProgress = (float)(age - 400L) / 200.0F;
                     scaleProgress = Math.max(0.0F, 1.0F - endProgress);
                  }

                  float scale = target.getBbWidth() * 2.5F * scaleProgress;
                  matrixStack.scale(scale, scale, scale);
                  Matrix4f matrix = matrixStack.last().pose();
                  BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                  Vtx.vertex(buffer, matrix, -0.5F, -0.5F, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, alpha);
                  Vtx.vertex(buffer, matrix, -0.5F, 0.5F, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, alpha);
                  Vtx.vertex(buffer, matrix, 0.5F, 0.5F, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, alpha);
                  Vtx.vertex(buffer, matrix, 0.5F, -0.5F, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, alpha);
                  BufferUploader.drawWithShader(buffer.buildOrThrow());
                  matrixStack.popPose();
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

   private void ensureDistortionFbo(int width, int height) {
      if (this.distortionFbo == null || this.distortionFbo.width != width || this.distortionFbo.height != height) {
         if (this.distortionFbo != null) {
            this.distortionFbo.destroyBuffers();
         }

         this.distortionFbo = GlCompat.newTarget(width, height, false);
         GlCompat.setClearColor(this.distortionFbo, 0.0F, 0.0F, 0.0F, 0.0F);
         GL11.glBindTexture(3553, GlCompat.colorId(this.distortionFbo));
         GL11.glTexParameteri(3553, 10241, 9729);
         GL11.glTexParameteri(3553, 10240, 9729);
         GL11.glTexParameteri(3553, 10242, 33071);
         GL11.glTexParameteri(3553, 10243, 33071);
         GL11.glBindTexture(3553, 0);
      }
   }

   private void initDistortionQuad() {
      if (this.distortionVao == -1) {
         float[] vertices = new float[]{
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            -1.0F,
            0.0F,
            0.0F,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
            -1.0F,
            1.0F,
            0.0F,
            1.0F
         };
         int prevVao = GL11.glGetInteger(34229);
         int prevVbo = GL11.glGetInteger(34964);
         this.distortionVao = GL30.glGenVertexArrays();
         this.distortionVbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.distortionVao);
         GL15.glBindBuffer(34962, this.distortionVbo);
         FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
         buffer.put(vertices).flip();
         GL15.glBufferData(34962, buffer, 35044);
         GL20.glVertexAttribPointer(0, 2, 5126, false, 16, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 16, 8L);
         GL20.glEnableVertexAttribArray(1);
         GL15.glBindBuffer(34962, prevVbo);
         GL30.glBindVertexArray(prevVao);
      }
   }

   private void initDistortionShader() {
      if (this.distortionProgram == -1) {
         String vert = "#version 150 core\nin vec2 Position;\nin vec2 TexCoord;\nout vec2 vTexCoord;\nvoid main() {\n    vTexCoord = TexCoord;\n    gl_Position = vec4(Position, 0.0, 1.0);\n}";
         String frag = "#version 150 core\nin vec2 vTexCoord;\nout vec4 fragColor;\n\nuniform sampler2D uColorTexture;\nuniform mat4 uViewProjMat;\nuniform vec2 uResolution;\nuniform float uDistortionStrength;\nuniform int uChromatic;\n\nconst int MAX_NODES = 36;\nuniform int uNodeCount;\nuniform vec3 uNodePos[MAX_NODES];\nuniform float uNodeRadius[MAX_NODES];\nuniform float uNodeAlpha[MAX_NODES];\n\nvoid main() {\n    vec4 baseColor = texture(uColorTexture, vTexCoord);\n    if (uNodeCount <= 0 || uDistortionStrength <= 0.001) {\n        fragColor = baseColor;\n        return;\n    }\n\n    vec2 totalUVShift = vec2(0.0);\n    float totalChromatic = 0.0;\n    float aspect = uResolution.x / uResolution.y;\n\n    for (int i = 0; i < uNodeCount; i++) {\n        if (i >= MAX_NODES) break;\n        vec3 nPos = uNodePos[i];\n        float nRad = uNodeRadius[i];\n        float alpha = uNodeAlpha[i];\n        if (alpha <= 0.001 || nRad <= 0.01) continue;\n\n        // Project 3D node center to Screen Space\n        vec4 clip = uViewProjMat * vec4(nPos, 1.0);\n        if (clip.w <= 0.1) continue; // Behind camera\n\n        vec2 screenCenter = (clip.xy / clip.w) * 0.5 + 0.5;\n        vec2 diff = (vTexCoord - screenCenter) * vec2(aspect, 1.0);\n        float screenDist = length(diff);\n        float screenRadius = clamp(nRad / clip.w, 0.02, 0.40);\n\n        if (screenDist < screenRadius) {\n            float normDist = screenDist / screenRadius;\n            float lens = sin(normDist * 3.14159265);\n            float pull = (1.0 - normDist) * 0.035;\n            vec2 dir = diff / max(0.0001, screenDist);\n\n            vec2 shift = dir * (lens * 0.022 - pull) * alpha * uDistortionStrength;\n            totalUVShift += vec2(shift.x / aspect, shift.y);\n\n            if (uChromatic == 1) {\n                totalChromatic += (lens * 0.008 + pull * 0.004) * alpha * uDistortionStrength;\n            }\n        }\n    }\n\n    if (length(totalUVShift) < 0.00005 && totalChromatic < 0.00005) {\n        fragColor = baseColor;\n        return;\n    }\n\n    // Sample with Chromatic Aberration\n    vec2 uvR = vTexCoord + totalUVShift + vec2(totalChromatic, 0.0);\n    vec2 uvG = vTexCoord + totalUVShift;\n    vec2 uvB = vTexCoord + totalUVShift - vec2(totalChromatic, 0.0);\n\n    float r = texture(uColorTexture, clamp(uvR, 0.001, 0.999)).r;\n    float g = texture(uColorTexture, clamp(uvG, 0.001, 0.999)).g;\n    float b = texture(uColorTexture, clamp(uvB, 0.001, 0.999)).b;\n\n    fragColor = vec4(r, g, b, baseColor.a);\n}";
         this.distortionProgram = this.createProgram(vert, frag);
      }
   }

   private int createProgram(String vert, String frag) {
      int v = GL20.glCreateShader(35633);
      GL20.glShaderSource(v, vert);
      GL20.glCompileShader(v);
      if (GL20.glGetShaderi(v, 35713) == 0) {
         System.err.println("TargetESP Vertex shader compile error:\n" + GL20.glGetShaderInfoLog(v, 1024));
      }

      int f = GL20.glCreateShader(35632);
      GL20.glShaderSource(f, frag);
      GL20.glCompileShader(f);
      if (GL20.glGetShaderi(f, 35713) == 0) {
         System.err.println("TargetESP Fragment shader compile error:\n" + GL20.glGetShaderInfoLog(f, 1024));
      }

      int p = GL20.glCreateProgram();
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glBindAttribLocation(p, 0, "Position");
      GL20.glBindAttribLocation(p, 1, "TexCoord");
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, 35714) == 0) {
         System.err.println("TargetESP Shader program link error:\n" + GL20.glGetProgramInfoLog(p, 1024));
      }

      return p;
   }

   private void setUniform1i(String name, int val) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform1i(loc, val);
      }
   }

   private void setUniform1f(String name, float val) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform1f(loc, val);
      }
   }

   private void setUniform2f(String name, float x, float y) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform2f(loc, x, y);
      }
   }

   private void setUniform3f(String name, float x, float y, float z) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         GL20.glUniform3f(loc, x, y, z);
      }
   }

   private void setUniformMatrix4fv(String name, float[] mat) {
      int loc = GL20.glGetUniformLocation(this.distortionProgram, name);
      if (loc != -1) {
         FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
         buffer.put(mat).flip();
         GL20.glUniformMatrix4fv(loc, false, buffer);
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
}
