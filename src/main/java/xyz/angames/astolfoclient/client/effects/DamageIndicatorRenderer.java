package xyz.angames.astolfoclient.client.effects;

import xyz.angames.astolfoclient.client.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font.DisplayMode;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.msdf.MsdfFont;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;

@Environment(EnvType.CLIENT)
public class DamageIndicatorRenderer {
   private static final Supplier<MsdfFont> FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
   private final DamageIndicatorManager manager;

   public DamageIndicatorRenderer(DamageIndicatorManager manager) {
      this.manager = manager;
   }

   public void render(WorldRenderContext context) {
      DamageIndicatorModule module = (DamageIndicatorModule)AstolfoclientClient.moduleManager.getModuleByName("DamageIndicators");
      if (module != null && module.isEnabled()) {
         if (!this.manager.getParticles().isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            Font textRenderer = client.font;
            Camera camera = context.camera();
            PoseStack matrices = context.matrixStack();
            double camX = camera.position().x;
            double camY = camera.position().y;
            double camZ = camera.position().z;
            float baseScale = (float)module.scale.get() * 0.02F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);

            for (DamageIndicatorManager.DamageParticle p : this.manager.getParticles()) {
               float alpha = 1.0F;
               int fadeTime = 12;
               if (p.maxAge - p.age <= fadeTime) {
                  float fadeProgress = (float)(p.maxAge - p.age) / fadeTime;
                  alpha = fadeProgress * fadeProgress * (3.0F - 2.0F * fadeProgress);
               }

               float popScale = 1.0F;
               float popDuration = 6.0F;
               if (p.age < popDuration) {
                  float t = p.age / popDuration;
                  float c1 = 1.70158F;
                  float c3 = c1 + 1.0F;
                  popScale = 1.0F + c3 * (float)Math.pow(t - 1.0F, 3.0) + c1 * (float)Math.pow(t - 1.0F, 2.0);
                  if (popScale < 0.0F) {
                     popScale = 0.0F;
                  }
               }

               int color = p.isCrit ? -65536 : -22016;
               int alphaHex = (int)(alpha * 255.0F) << 24;
               color = color & 16777215 | alphaHex;
               matrices.pushPose();
               matrices.translate(p.x - camX, p.y - camY, p.z - camZ);
               matrices.rotate(Axis.YP.rotationDegrees(-camera.yRot()));
               matrices.rotate(Axis.XP.rotationDegrees(camera.xRot()));
               float currentScale = baseScale * (p.isCrit ? 1.3F : 1.0F) * popScale;
               matrices.scale(-currentScale, -currentScale, currentScale);
               Matrix4f positionMatrix = matrices.last().pose();
               Component text = Component.literal(p.text);
               MsdfFont font = FONT.get();
               float xOffset = -font.getWidth(p.text, 9.0F) / 2.0F;
               Builder.text().font(font).text(p.text).color(color & 16777215 | (alphaHex >>> 1 & 0xFF000000)).size(9.0F).build().render(positionMatrix, xOffset + 0.8F, 0.8F, 0.0F);
               Builder.text().font(font).text(p.text).color(color).size(9.0F).build().render(positionMatrix, xOffset, 0.0F, 0.0F);
               matrices.popPose();
            }

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
         }
      }
   }
}
