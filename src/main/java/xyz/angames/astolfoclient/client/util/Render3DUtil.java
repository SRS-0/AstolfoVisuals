package xyz.angames.astolfoclient.client.util;

import com.mojang.blaze3d.opengl.GlStateManager;
import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKeys;
import xyz.angames.astolfoclient.client.render.compat.BufferRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4i;
import xyz.angames.astolfoclient.client.util.ColorUtil;

@Environment(value=EnvType.CLIENT)
public class Render3DUtil {
    private static final List<Texture> GLOW_TEXTURES = new ArrayList<Texture>();
    private static final Tesselator tessellator = Tesselator.getInstance();

    public static void onRenderWorld(PoseStack matrix) {
        PoseStack.Pose entry = matrix.last();
        if (!GLOW_TEXTURES.isEmpty()) {
            Set identifiers = GLOW_TEXTURES.stream().map(texture -> texture.id).collect(Collectors.toCollection(LinkedHashSet::new));
            RenderSystem.enableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask((boolean)false);
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE);
            identifiers.forEach(id -> {
                RenderSystem.setShaderTexture((int)0, (Identifier)id);
                RenderSystem.setShader((ShaderProgramKey)ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = tessellator.begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                GLOW_TEXTURES.stream().filter(texture -> texture.id.equals(id)).forEach(tex -> Render3DUtil.quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color));
                BufferRenderer.drawWithGlobalProgram((MeshData)buffer.buildOrThrow());
            });
            RenderSystem.blendFunc((com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.SRC_ALPHA, (com.mojang.blaze3d.platform.BlendFactor)com.mojang.blaze3d.platform.BlendFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask((boolean)true);
            RenderSystem.disableBlend();
            GLOW_TEXTURES.clear();
        }
    }

    public static void drawGlowTexture(PoseStack.Pose entry, Identifier id, float x, float y, float width, float height, int color) {
        GLOW_TEXTURES.add(new Texture(entry, id, x, y, width, height, new Vector4i(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), ColorUtil.alpha(color))));
    }

    private static void quadTexture(PoseStack.Pose entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color) {
        Matrix4f matrix = entry != null ? entry.pose() : new Matrix4f();
        int r = color.x;
        int g = color.y;
        int b = color.z;
        int a = color.w;
        buffer.addVertex(matrix, x, y + height, 0.0f).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(matrix, x + width, y + height, 0.0f).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buffer.addVertex(matrix, x + width, y, 0.0f).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buffer.addVertex(matrix, x, y, 0.0f).setUv(0.0f, 0.0f).setColor(r, g, b, a);
    }

    public static void drawGlowTexture(PoseStack.Pose peek, Identifier texture, float v, float v1, float v2, float v3, Vector4i vector4i, boolean b) {
    }

    public static void drawBox(@Nullable PoseStack matrixStack, AABB box, Color color) {
    }

    @Environment(value=EnvType.CLIENT)
    public record Texture(PoseStack.Pose entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
    }
}

