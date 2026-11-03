package dev.sxmurxy.mre.renderers.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.providers.ResourceProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.joml.Matrix4f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import xyz.angames.astolfoclient.client.render.compat.Tesselator;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.sxmurxy.mre.renderers.GpuEffect;
import dev.sxmurxy.mre.renderers.GpuSceneCopy;

@Environment(value=EnvType.CLIENT)
public record BuiltRectangle(SizeState size, QuadRadiusState radius, QuadColorState color, float smoothness) implements IRenderer
{
    private static final GpuEffect RECTANGLE_SHADER_KEY = new GpuEffect("rectangle", DefaultVertexFormat.POSITION_COLOR);

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {



        float width = this.size.width();
        float height = this.size.height();
        GpuEffect.Uniforms shader = RECTANGLE_SHADER_KEY.uniforms();
        shader.getUniform("Size").set(width, height);
        shader.getUniform("Radius").set(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4());
        shader.getUniform("Smoothness").set(this.smoothness);
        BufferBuilder builder = Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
        builder.addVertex(matrix, x, y, z).setColor(this.color.color1());
        builder.addVertex(matrix, x, y + height, z).setColor(this.color.color2());
        builder.addVertex(matrix, x + width, y + height, z).setColor(this.color.color3());
        builder.addVertex(matrix, x + width, y, z).setColor(this.color.color4());
        RECTANGLE_SHADER_KEY.draw(builder.buildOrThrow(), shader, (AbstractTexture)null);


    }
}

