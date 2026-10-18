package xyz.angames.astolfoclient.client.render;

import xyz.angames.astolfoclient.client.compat.Vtx;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.phys.Vec3;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.manager.GpsManager;

@Environment(EnvType.CLIENT)
public class GpsRenderer {
   private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
   private static final Identifier GPS_ICON = Identifier.fromNamespaceAndPath("astolfoclient", "textures/icons/gps.png");
   private String cachedDistanceText = "";
   private int lastDistanceInt = -1;

   public void render(WorldRenderContext context) {
      GpsManager manager = GpsManager.getInstance();
      if (manager.isActive()) {
         Minecraft client = Minecraft.getInstance();
         if (client.player != null && client.level != null) {
            Vec3 cameraPos = context.camera().position();
            double targetX = manager.getTargetX();
            double targetZ = manager.getTargetZ();
            float tickDelta = context.tickCounter().getGameTimeDeltaPartialTick(true);
            double playerX = Mth.lerp(tickDelta, client.player.xo, client.player.getX());
            double playerY = Mth.lerp(tickDelta, client.player.yo, client.player.getY());
            double playerZ = Mth.lerp(tickDelta, client.player.zo, client.player.getZ());
            double targetY = playerY + 2.0;
            double distSq = client.player.distanceToSqr(targetX, client.player.getY(), targetZ);
            double realDistance = Math.sqrt(distSq);
            int currentDistanceInt = (int)realDistance;
            if (currentDistanceInt != this.lastDistanceInt) {
               this.cachedDistanceText = String.format("%.0fm", realDistance);
               this.lastDistanceInt = currentDistanceInt;
            }

            double renderDist = 10.0;
            double dx = targetX - playerX;
            double dy = targetY - playerY;
            double dz = targetZ - playerZ;
            double distance3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double dirX = dx / distance3D;
            double dirY = dy / distance3D;
            double dirZ = dz / distance3D;
            double renderX;
            double renderYFinal;
            double renderZ;
            if (realDistance < renderDist) {
               renderX = targetX;
               renderYFinal = targetY;
               renderZ = targetZ;
            } else {
               renderX = playerX + dirX * renderDist;
               renderYFinal = playerY + dirY * renderDist;
               renderZ = playerZ + dirZ * renderDist;
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            PoseStack matrixStack = context.matrixStack();
            matrixStack.pushPose();
            matrixStack.translate(renderX - cameraPos.x, renderYFinal - cameraPos.y, renderZ - cameraPos.z);
            matrixStack.rotate(context.camera().rotation());
            matrixStack.scale(1.0F, -1.0F, 1.0F);
            float fixedScale = 0.04F;
            matrixStack.scale(fixedScale, fixedScale, fixedScale);
            RenderSystem.setShaderTexture(0, GPS_ICON);
            RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
            Tesselator tessellator = Tesselator.getInstance();
            float size = 40.0F;
            float halfSize = size / 2.0F;
            float shadowOffset = 0.8F;
            matrixStack.pushPose();
            matrixStack.translate(shadowOffset, shadowOffset, 0.05F);
            Matrix4f shadowMatrix = matrixStack.last().pose();
            BufferBuilder shadowBuffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vtx.vertex(shadowBuffer, shadowMatrix, -halfSize, -halfSize, 0.0F).setUv(0.0F, 0.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
            Vtx.vertex(shadowBuffer, shadowMatrix, -halfSize, halfSize, 0.0F).setUv(0.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
            Vtx.vertex(shadowBuffer, shadowMatrix, halfSize, halfSize, 0.0F).setUv(1.0F, 1.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
            Vtx.vertex(shadowBuffer, shadowMatrix, halfSize, -halfSize, 0.0F).setUv(1.0F, 0.0F).setColor(0.0F, 0.0F, 0.0F, 1.0F);
            BufferUploader.draw(shadowBuffer.buildOrThrow());
            matrixStack.popPose();
            matrixStack.pushPose();
            matrixStack.translate(0.0F, 0.0F, -0.1F);
            Matrix4f iconMatrix = matrixStack.last().pose();
            BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Vtx.vertex(buffer, iconMatrix, -halfSize, -halfSize, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Vtx.vertex(buffer, iconMatrix, -halfSize, halfSize, 0.0F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Vtx.vertex(buffer, iconMatrix, halfSize, halfSize, 0.0F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Vtx.vertex(buffer, iconMatrix, halfSize, -halfSize, 0.0F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F);
            BufferUploader.draw(buffer.buildOrThrow());
            matrixStack.popPose();
            float padding = 5.0F;
            matrixStack.translate(0.0F, halfSize + padding, 0.0F);
            float textScale = 0.6F;
            float textWidth = this.cachedDistanceText.length() * 4.5F;
            matrixStack.translate(-(textWidth * textScale) / 2.0, 0.0, 0.0);
            matrixStack.translate(shadowOffset, shadowOffset, 0.05F);
            this.renderText(matrixStack, this.cachedDistanceText, Color.BLACK, textScale);
            matrixStack.translate(-shadowOffset, -shadowOffset, -0.1F);
            this.renderText(matrixStack, this.cachedDistanceText, Color.WHITE, textScale);
            matrixStack.popPose();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
         }
      }
   }

   private void renderText(PoseStack stack, String text, Color color, float scale) {
      Matrix4f mat = stack.last().pose();
      Builder.text().font((MsdfFont)BIKO_FONT.get()).text(text).color(color).size(20.0F * scale).build().render(mat, 0.0F, 0.0F);
   }
}
