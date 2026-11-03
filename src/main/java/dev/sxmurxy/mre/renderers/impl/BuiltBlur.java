package dev.sxmurxy.mre.renderers.impl;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.sxmurxy.mre.renderers.GpuEffect;
import dev.sxmurxy.mre.renderers.GpuSceneCopy;

@Environment(value=EnvType.CLIENT)
public record BuiltBlur(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness, float blurRadius) implements IRenderer
{
    private static final GpuEffect BLUR_SHADER_KEY = new GpuEffect("blur", DefaultVertexFormat.POSITION_COLOR);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        TextureTarget fbo = GpuSceneCopy.gui();
        float width = this.size.width();
        float height = this.size.height();
        GpuEffect.Uniforms shader = BLUR_SHADER_KEY.uniforms();
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        shader.getUniform("BlurRadius").set(this.blurRadius);
        BufferBuilder builder = Tesselator.getInstance().begin(com.mojang.blaze3d.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, x, y, z).setColor(this.color.color1());
        builder.addVertex(matrix, x, y + height, z).setColor(this.color.color2());
        builder.addVertex(matrix, x + width, y + height, z).setColor(this.color.color3());
        builder.addVertex(matrix, x + width, y, z).setColor(this.color.color4());
        BLUR_SHADER_KEY.draw(builder.buildOrThrow(), shader, fbo.getColorTextureView(), RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));



    }
}

