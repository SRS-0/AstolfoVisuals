package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;

@Environment(EnvType.CLIENT)
public class TrailsRenderer {
   private final Minecraft client = Minecraft.getInstance();
   private final List<TrailsRenderer.TrailPoint> points = new ArrayList<>();
   private static final long TRAIL_LIFESPAN = 850L;

   public void render(WorldRenderContext context) {
      Module module = AstolfoclientClient.moduleManager.getModuleByName("Trails");
      if (module != null && module.isEnabled() && !this.client.options.getCameraType().isFirstPerson()) {
         LocalPlayer player = this.client.player;
         if (player != null) {
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            double x = Mth.lerp(tickDelta, player.xOld, player.getX());
            double y = Mth.lerp(tickDelta, player.yOld, player.getY());
            double z = Mth.lerp(tickDelta, player.zOld, player.getZ());
            Vec3 currentPos = new Vec3(x, y, z);
            boolean isBaby = false;
            if (AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer") instanceof BabyPlayerModule bpm) {
               isBaby = bpm.isEnabled() && bpm.self.get();
            }

            float targetHeight = isBaby ? 0.9F : player.getBbHeight();
            if (this.points.isEmpty() || this.points.get(this.points.size() - 1).pos.distanceToSqr(currentPos) > 0.001) {
               this.points.add(new TrailsRenderer.TrailPoint(currentPos, targetHeight, System.currentTimeMillis()));
            }

            long currentTime = System.currentTimeMillis();
            this.points.removeIf(p -> currentTime - p.timeCreated > 850L);
            if (this.points.size() >= 2) {
               RenderSystem.enableBlend();
               RenderSystem.disableCull();
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(false);
               RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
               RenderSystem.setShader(CoreShaders.POSITION_COLOR);
               Tesselator tessellator = Tesselator.getInstance();
               PoseStack matrices = context.matrixStack();
               Matrix4f matrix = matrices.last().pose();
               Vec3 cameraPos = context.camera().position();
               BufferBuilder buffer = tessellator.begin(PrimitiveTopology.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
               int index = 0;

               for (TrailsRenderer.TrailPoint point : this.points) {
                  long age = currentTime - point.timeCreated;
                  float progress = Mth.clamp((float)age / 850.0F, 0.0F, 1.0F);
                  float reverseProgress = 1.0F - progress;
                  float alpha = (float)Math.pow(reverseProgress, 2.5);
                  float bodyAlpha = alpha * 0.45F;
                  float heightScale = reverseProgress * reverseProgress;
                  float midY = point.height / 2.0F;
                  float topY = midY + midY * heightScale;
                  float botY = midY - midY * heightScale;
                  Color c = new Color(ThemeManager.getThemedColor(index * 20));
                  float r = c.getRed() / 255.0F;
                  float g = c.getGreen() / 255.0F;
                  float b = c.getBlue() / 255.0F;
                  double renderX = point.pos.x - cameraPos.x;
                  double renderY = point.pos.y - cameraPos.y;
                  double renderZ = point.pos.z - cameraPos.z;
                  Vtx.vertex(buffer, matrix, (float)renderX, (float)(renderY + topY), (float)renderZ).setColor(r, g, b, bodyAlpha);
                  Vtx.vertex(buffer, matrix, (float)renderX, (float)(renderY + botY), (float)renderZ).setColor(r, g, b, bodyAlpha);
                  index++;
               }

               BufferUploader.drawWithShader(buffer.buildOrThrow());
               RenderSystem.lineWidth(2.5F);
               buffer = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
               index = 0;

               for (TrailsRenderer.TrailPoint point : this.points) {
                  long age = currentTime - point.timeCreated;
                  float progress = Mth.clamp((float)age / 850.0F, 0.0F, 1.0F);
                  float reverseProgress = 1.0F - progress;
                  float alpha = (float)Math.pow(reverseProgress, 2.0);
                  float heightScale = reverseProgress * reverseProgress;
                  float midY = point.height / 2.0F;
                  float topY = midY + midY * heightScale;
                  Color c = new Color(ThemeManager.getThemedColor(index * 20));
                  double renderX = point.pos.x - cameraPos.x;
                  double renderY = point.pos.y - cameraPos.y;
                  double renderZ = point.pos.z - cameraPos.z;
                  Vtx.vertex(buffer, matrix, (float)renderX, (float)(renderY + topY), (float)renderZ)
                     .setColor(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, alpha * 0.8F);
                  index++;
               }

               BufferUploader.drawWithShader(buffer.buildOrThrow());
               buffer = tessellator.begin(PrimitiveTopology.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
               index = 0;

               for (TrailsRenderer.TrailPoint point : this.points) {
                  long age = currentTime - point.timeCreated;
                  float progress = Mth.clamp((float)age / 850.0F, 0.0F, 1.0F);
                  float reverseProgress = 1.0F - progress;
                  float alpha = (float)Math.pow(reverseProgress, 2.0);
                  float heightScale = reverseProgress * reverseProgress;
                  float midY = point.height / 2.0F;
                  float botY = midY - midY * heightScale;
                  Color c = new Color(ThemeManager.getThemedColor(index * 20));
                  double renderX = point.pos.x - cameraPos.x;
                  double renderY = point.pos.y - cameraPos.y;
                  double renderZ = point.pos.z - cameraPos.z;
                  Vtx.vertex(buffer, matrix, (float)renderX, (float)(renderY + botY), (float)renderZ)
                     .setColor(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, alpha * 0.8F);
                  index++;
               }

               BufferUploader.drawWithShader(buffer.buildOrThrow());
               RenderSystem.lineWidth(1.0F);
               RenderSystem.enableDepthTest();
               RenderSystem.depthMask(true);
               RenderSystem.enableCull();
               RenderSystem.defaultBlendFunc();
               RenderSystem.disableBlend();
            }
         }
      } else {
         this.points.clear();
      }
   }

   @Environment(EnvType.CLIENT)
   private static class TrailPoint {
      final Vec3 pos;
      final float height;
      final long timeCreated;

      public TrailPoint(Vec3 pos, float height, long timeCreated) {
         this.pos = pos;
         this.height = height;
         this.timeCreated = timeCreated;
      }
   }
}
