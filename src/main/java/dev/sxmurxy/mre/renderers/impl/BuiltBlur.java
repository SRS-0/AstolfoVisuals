package dev.sxmurxy.mre.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sxmurxy.mre.renderers.GpuEffect;
import dev.sxmurxy.mre.renderers.GpuSceneCopy;
import net.minecraft.client.texture.AbstractTexture;

@Environment(value=EnvType.CLIENT)
public record BuiltBlur(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float blurRadius) implements IRenderer
{
    private static final GpuEffect BLUR_SHADER_KEY = new GpuEffect("blur", VertexFormats.POSITION_COLOR);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        SimpleFramebuffer fbo = GpuSceneCopy.capture();
        float width = this.size.width();
        float height = this.size.height();
        GpuEffect.Uniforms shader = BLUR_SHADER_KEY.uniforms();
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix, x, y, z).color(this.color.color1());
        builder.vertex(matrix, x, y + height, z).color(this.color.color2());
        builder.vertex(matrix, x + width, y + height, z).color(this.color.color3());
        builder.vertex(matrix, x + width, y, z).color(this.color.color4());
        BLUR_SHADER_KEY.draw(builder.end(), shader, fbo.getColorAttachmentView(), RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.FilterMode.LINEAR));



    }
}

