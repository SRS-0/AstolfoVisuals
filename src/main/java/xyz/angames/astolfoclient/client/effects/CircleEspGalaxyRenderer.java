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
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class CircleEspGalaxyRenderer {
   private static final Identifier BLOOM_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/effects/bloom.png");
   private final CircleEspManager manager;

   public CircleEspGalaxyRenderer(CircleEspManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context, List<CircleEspManager.CircleEspEffect> validEffects) {
      Module module = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
      if (module != null && module.isEnabled()) {
         if (!validEffects.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
            Tesselator tessellator = Tesselator.getInstance();
            long currentTime = System.currentTimeMillis();
            int rgb = ThemeManager.getThemedColor(0L);
            Color c = new Color(rgb);
            float r = c.getRed() / 255.0F;
            float g = c.getGreen() / 255.0F;
            float b = c.getBlue() / 255.0F;

            for (CircleEspManager.CircleEspEffect effect : validEffects) {
               Entity target = effect.target;
               if (target.isAlive() && !TargetUtils.isInvisible(target)) {
                  long timeSinceHit = currentTime - effect.lastHitTime;
                  float fadeProgress = (float)timeSinceHit / 450.0F;
                  float alpha = 1.0F - Math.max(0.0F, (fadeProgress - 0.5F) * 2.0F);
                  if (!(alpha <= 0.05F)) {
                     float animTime = (float)(currentTime - effect.startTime) / 1000.0F;
                     float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
                     double tX = target.xOld + (target.getX() - target.xOld) * tickDelta;
                     double tY = target.yOld + (target.getY() - target.yOld) * tickDelta;
                     double tZ = target.zOld + (target.getZ() - target.zOld) * tickDelta;
                     float height = target.getBbHeight();
                     float radius = target.getBbWidth() * 0.8F;
                     if (timeSinceHit > 400L) {
                        float endProgress = (float)(timeSinceHit - 400L) / 200.0F;
                        radius *= Math.max(0.0F, 1.0F - endProgress);
                     }

                     float scanSpeed = 3.0F;
                     float phase = (float)Math.sin(animTime * scanSpeed);
                     float scanY = (phase + 1.0F) / 2.0F * height;
                     float velocity = (float)Math.cos(animTime * scanSpeed);
                     float maxTailLength = height * 0.4F;
                     float tailOffset = -velocity * maxTailLength;
                     PoseStack matrices = context.matrixStack();
                     matrices.pushPose();
                     matrices.translate(
                        tX - context.camera().position().x,
                        tY - context.camera().position().y,
                        tZ - context.camera().position().z
                     );
                     matrices.rotate(Axis.YP.rotationDegrees(animTime * 45.0F));
                     RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
                     RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
                     BufferBuilder bbBloom = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                     this.drawHorizontalBloom(matrices, bbBloom, scanY, radius * 2.5F, r, g, b, alpha * 0.5F);
                     BufferUploader.drawWithShader(bbBloom.buildOrThrow());
                     RenderSystem.setShader(CoreShaders.POSITION_COLOR);
                     BufferBuilder bbTail = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
                     this.drawGradientCylinder(matrices, bbTail, radius, scanY, scanY + tailOffset, r, g, b, alpha * 0.45F, 0.0F);
                     BufferUploader.drawWithShader(bbTail.buildOrThrow());
                     RenderSystem.lineWidth(2.5F);
                     BufferBuilder bbRing = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
                     this.drawCrispRing(matrices, bbRing, radius, scanY, r, g, b, alpha * 0.9F);
                     BufferUploader.drawWithShader(bbRing.buildOrThrow());
                     RenderSystem.lineWidth(1.0F);
                     matrices.popPose();
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

   private void drawHorizontalBloom(PoseStack stack, BufferBuilder buffer, float y, float size, float r, float g, float b, float a) {
      Matrix4f m = stack.last().pose();
      Vtx.vertex(buffer, m, -size, y, -size).setUv(0.0F, 0.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, -size, y, size).setUv(0.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, size, y, size).setUv(1.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, m, size, y, -size).setUv(1.0F, 0.0F).setColor(r, g, b, a);
   }

   private void drawGradientCylinder(
      PoseStack stack, BufferBuilder buffer, float radius, float yStart, float yEnd, float r, float g, float b, float aStart, float aEnd
   ) {
      Matrix4f m = stack.last().pose();
      int segments = 40;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)(i * Math.PI * 2.0 / segments);
         float angle2 = (float)((i + 1) * Math.PI * 2.0 / segments);
         float x1 = (float)Math.cos(angle1) * radius;
         float z1 = (float)Math.sin(angle1) * radius;
         float x2 = (float)Math.cos(angle2) * radius;
         float z2 = (float)Math.sin(angle2) * radius;
         Vtx.vertex(buffer, m, x1, yStart, z1).setColor(r, g, b, aStart);
         Vtx.vertex(buffer, m, x1, yEnd, z1).setColor(r, g, b, aEnd);
         Vtx.vertex(buffer, m, x2, yEnd, z2).setColor(r, g, b, aEnd);
         Vtx.vertex(buffer, m, x2, yStart, z2).setColor(r, g, b, aStart);
      }
   }

   private void drawCrispRing(PoseStack stack, BufferBuilder buffer, float radius, float y, float r, float g, float b, float a) {
      Matrix4f m = stack.last().pose();
      int segments = 40;

      for (int i = 0; i <= segments; i++) {
         float angle = (float)(i * Math.PI * 2.0 / segments);
         float x = (float)Math.cos(angle) * radius;
         float z = (float)Math.sin(angle) * radius;
         Vtx.vertex(buffer, m, x, y, z).setColor(r, g, b, a);
      }
   }
}
