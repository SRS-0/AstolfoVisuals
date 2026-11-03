package dev.sxmurxy.mre.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.providers.ColorProvider;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.sxmurxy.mre.renderers.GpuEffect;
import dev.sxmurxy.mre.renderers.GpuSceneCopy;

@Environment(value=EnvType.CLIENT)
public record BuiltText(MsdfFont font, String text, float size, float thickness, int color, float smoothness, float spacing, int outlineColor, float outlineThickness) implements IRenderer
{
    private static final GpuEffect MSDF_FONT_SHADER_KEY = new GpuEffect("msdf_font", DefaultVertexFormat.POSITION_TEX_COLOR);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        if (this.text == null || this.text.isEmpty()) {
            return;
        }
        if (this.text.trim().isEmpty()) {
            return;
        }




        boolean outlineEnabled = this.outlineThickness > 0.0f;
        GpuEffect.Uniforms shader = MSDF_FONT_SHADER_KEY.uniforms();
        if (shader != null) {
            if (shader.getUniform("Range") != null) {
                shader.getUniform("Range").set(this.font.getAtlas().range());
            }
            if (shader.getUniform("Thickness") != null) {
                shader.getUniform("Thickness").set(this.thickness);
            }
            if (shader.getUniform("Smoothness") != null) {
                shader.getUniform("Smoothness").set(this.smoothness);
            }
            if (shader.getUniform("Outline") != null) {
                shader.getUniform("Outline").set(outlineEnabled ? 1 : 0);
            }
            if (outlineEnabled) {
                if (shader.getUniform("OutlineThickness") != null) {
                    shader.getUniform("OutlineThickness").set(this.outlineThickness);
                }
                float[] outlineComponents = ColorProvider.normalize(this.outlineColor);
                if (shader.getUniform("OutlineColor") != null) {
                    shader.getUniform("OutlineColor").set(outlineComponents[0], outlineComponents[1], outlineComponents[2], outlineComponents[3]);
                }
            }
        }
        BufferBuilder builder = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float autoCapHeightOffset = this.font.getAutoYOffset() * this.size;
        this.font.applyGlyphs(matrix, (VertexConsumer)builder, this.text, this.size, (this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing, x, y + autoCapHeightOffset, z, this.color);
        try {
            MeshData data = builder.buildOrThrow();
            if (data != null) {
                MSDF_FONT_SHADER_KEY.draw(data, shader, this.font.getTexture());
            }
        }
        catch (IllegalStateException data) {
        }
        catch (Exception e) {
            e.printStackTrace();
        }



    }
}

