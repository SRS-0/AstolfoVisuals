package xyz.angames.astolfoclient.client.util;

import xyz.angames.astolfoclient.client.compat.Vtx;
import xyz.angames.astolfoclient.client.compat.DestFactor;
import xyz.angames.astolfoclient.client.compat.SourceFactor;
import xyz.angames.astolfoclient.client.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.CoreShaders;
import net.minecraft.world.phys.AABB;
import xyz.angames.astolfoclient.client.compat.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4i;

@Environment(EnvType.CLIENT)
public class Render3DUtil {
   private static final List<Render3DUtil.Texture> GLOW_TEXTURES = new ArrayList<>();
   private static final Tesselator tessellator = Tesselator.getInstance();

   public static void onRenderWorld(PoseStack matrix) {
      Pose entry = matrix.last();
      if (!GLOW_TEXTURES.isEmpty()) {
         Set<Identifier> identifiers = GLOW_TEXTURES.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
         RenderSystem.enableBlend();
         RenderSystem.disableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
         identifiers.forEach(
            id -> {
               RenderSystem.setShaderTexture(0, id);
               RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
               BufferBuilder buffer = tessellator.begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
               GLOW_TEXTURES.stream()
                  .filter(texture -> texture.id.equals(id))
                  .forEach(tex -> quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
               BufferUploader.drawWithShader(buffer.buildOrThrow());
            }
         );
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         GLOW_TEXTURES.clear();
      }
   }

   public static void drawGlowTexture(Pose entry, Identifier id, float x, float y, float width, float height, int color) {
      GLOW_TEXTURES.add(
         new Render3DUtil.Texture(
            entry, id, x, y, width, height, new Vector4i(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), ColorUtil.alpha(color))
         )
      );
   }

   private static void quadTexture(Pose entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
      Matrix4f matrix = entry != null ? entry.pose() : new Matrix4f();
      int r = color.x;
      int g = color.y;
      int b = color.z;
      int a = color.w;
      Vtx.vertex(buffer, matrix, x, y + height, 0.0F).setUv(0.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, matrix, x + width, y + height, 0.0F).setUv(1.0F, 1.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, matrix, x + width, y, 0.0F).setUv(1.0F, 0.0F).setColor(r, g, b, a);
      Vtx.vertex(buffer, matrix, x, y, 0.0F).setUv(0.0F, 0.0F).setColor(r, g, b, a);
   }

   public static void drawGlowTexture(Pose peek, Identifier texture, float v, float v1, float v2, float v3, Vector4i vector4i, boolean b) {
   }

   public static void drawBox(@Nullable PoseStack matrixStack, AABB box, Color color) {
   }

   @Environment(EnvType.CLIENT)
   public record Texture(Pose entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
   }
}
