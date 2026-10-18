package dev.sxmurxy.mre.renderers.impl;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.IRenderer;
import dev.sxmurxy.mre.utils.CRenderPipelines;
import dev.sxmurxy.mre.utils.MreDraw;
import java.util.List;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.compat.Tesselator;
import xyz.angames.astolfoclient.client.compat.Vtx;

public record BuiltTexture(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness,
        float u, float v,
        float texWidth, float texHeight,
        GpuTextureView textureView,
        GpuSampler sampler
    ) implements IRenderer {

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        if (this.textureView == null) {
            return;
        }

        float width = this.size.width(), height = this.size.height();

        BufferBuilder builder = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Vtx.vertex(builder, matrix, x, y, z).setUv(this.u, this.v).setColor(this.color.color1());
        Vtx.vertex(builder, matrix, x, y + height, z).setUv(this.u, this.v + this.texHeight).setColor(this.color.color2());
        Vtx.vertex(builder, matrix, x + width, y + height, z).setUv(this.u + this.texWidth, this.v + this.texHeight).setColor(this.color.color3());
        Vtx.vertex(builder, matrix, x + width, y, z).setUv(this.u + this.texWidth, this.v).setColor(this.color.color4());

        MreDraw.submit(builder.buildOrThrow(), CRenderPipelines.TEXTURE_PIPLINE, 32, b -> b
            .putVec4(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4())
            .putVec2(width, height).putFloat(this.smoothness),
            List.of(new MreDraw.Texture("Sampler0", this.textureView, this.sampler)), false);
    }

}
