package dev.sxmurxy.mre.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ColorProvider;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sxmurxy.mre.renderers.GpuEffect;
import dev.sxmurxy.mre.renderers.GpuSceneCopy;
import org.joml.Vector4f;
import org.joml.Vector4fc;

@Environment(value=EnvType.CLIENT)
public record BuiltLiquidGlass(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float cornerSmoothness, float globalAlpha, float fresnelPower, int fresnelColor, float fresnelAlpha, float baseAlpha, boolean fresnelInvert, float fresnelMix, float distortStrength, boolean useBackground, float u, float v, float texWidth, float texHeight, AbstractTexture textureId) implements IRenderer
{
    private static final GpuEffect LIQUID_GLASS_SHADER_KEY = new GpuEffect("liquidglass", DefaultVertexFormat.POSITION_TEX_COLOR);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        float v3;
        float u3;
        float v2;
        float u2;
        float v1;
        float u1;
        float v0;
        float u0;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget screenFBO = mc.gameRenderer.mainRenderTarget();



        float width = this.size.width();
        float height = this.size.height();
        TextureTarget scene = this.useBackground ? GpuSceneCopy.gui() : null;
        if (this.useBackground) {
            float scaledW = mc.getWindow().getGuiScaledWidth();
            float scaledH = mc.getWindow().getGuiScaledHeight();
            Vector4f pos0 = matrix.transform((Vector4fc)new Vector4f(x, y, z, 1.0f), new Vector4f());
            u0 = pos0.x / scaledW;
            v0 = (scaledH - pos0.y) / scaledH;
            Vector4f pos1 = matrix.transform((Vector4fc)new Vector4f(x, y + height, z, 1.0f), new Vector4f());
            u1 = pos1.x / scaledW;
            v1 = (scaledH - pos1.y) / scaledH;
            Vector4f pos2 = matrix.transform((Vector4fc)new Vector4f(x + width, y + height, z, 1.0f), new Vector4f());
            u2 = pos2.x / scaledW;
            v2 = (scaledH - pos2.y) / scaledH;
            Vector4f pos3 = matrix.transform((Vector4fc)new Vector4f(x + width, y, z, 1.0f), new Vector4f());
            u3 = pos3.x / scaledW;
            v3 = (scaledH - pos3.y) / scaledH;
        } else {

            u0 = this.u;
            v0 = this.v + this.texHeight;
            u1 = this.u;
            v1 = this.v;
            u2 = this.u + this.texWidth;
            v2 = this.v;
            u3 = this.u + this.texWidth;
            v3 = this.v + this.texHeight;
        }
        GpuEffect.Uniforms shader = LIQUID_GLASS_SHADER_KEY.uniforms();
        if (shader != null) {
            if (shader.getUniform("Size") != null) {
                shader.getUniform("Size").set(width, height);
            }
            if (shader.getUniform("Radius") != null) {
                shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
            }
            if (shader.getUniform("Smoothness") != null) {
                shader.getUniform("Smoothness").set(this.smoothness);
            }
            if (shader.getUniform("CornerSmoothness") != null) {
                shader.getUniform("CornerSmoothness").set(this.cornerSmoothness);
            }
            if (shader.getUniform("GlobalAlpha") != null) {
                shader.getUniform("GlobalAlpha").set(this.globalAlpha);
            }
            if (shader.getUniform("FresnelPower") != null) {
                shader.getUniform("FresnelPower").set(this.fresnelPower);
            }
            float[] fColor = ColorProvider.normalize(this.fresnelColor);
            if (shader.getUniform("FresnelColor") != null) {
                shader.getUniform("FresnelColor").set(fColor[0], fColor[1], fColor[2]);
            }
            if (shader.getUniform("FresnelAlpha") != null) {
                shader.getUniform("FresnelAlpha").set(this.fresnelAlpha);
            }
            if (shader.getUniform("BaseAlpha") != null) {
                shader.getUniform("BaseAlpha").set(this.baseAlpha);
            }
            if (shader.getUniform("FresnelInvert") != null) {
                shader.getUniform("FresnelInvert").set(this.fresnelInvert ? 1 : 0);
            }
            if (shader.getUniform("FresnelMix") != null) {
                shader.getUniform("FresnelMix").set(this.fresnelMix);
            }
            if (shader.getUniform("DistortStrength") != null) {
                shader.getUniform("DistortStrength").set(this.distortStrength);
            }
        }
        BufferBuilder builder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.addVertex(matrix, x, y, z).setUv(u0, v0).setColor(this.color.color1());
        builder.addVertex(matrix, x, y + height, z).setUv(u1, v1).setColor(this.color.color2());
        builder.addVertex(matrix, x + width, y + height, z).setUv(u2, v2).setColor(this.color.color3());
        builder.addVertex(matrix, x + width, y, z).setUv(u3, v3).setColor(this.color.color4());
        try {
            MeshData data = builder.buildOrThrow();
            if (data != null) {
                LIQUID_GLASS_SHADER_KEY.draw(data, shader, this.useBackground ? scene.getColorTextureView() : this.textureId.getTextureView(), this.useBackground ? RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR) : this.textureId.getSampler());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }



    }
}

