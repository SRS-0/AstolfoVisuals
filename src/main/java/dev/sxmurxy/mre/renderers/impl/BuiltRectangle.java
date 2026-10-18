package dev.sxmurxy.mre.renderers.impl;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
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

public record BuiltRectangle(
        SizeState size,
        QuadRadiusState radius,
        QuadColorState color,
        float smoothness
    ) implements IRenderer {

    @Override
    public void render(Matrix4f matrix, float x, float y, float z) {
        float width = this.size.width(), height = this.size.height();

        BufferBuilder builder = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Vtx.vertex(builder, matrix, x, y, z).setColor(this.color.color1());
        Vtx.vertex(builder, matrix, x, y + height, z).setColor(this.color.color2());
        Vtx.vertex(builder, matrix, x + width, y + height, z).setColor(this.color.color3());
        Vtx.vertex(builder, matrix, x + width, y, z).setColor(this.color.color4());

        MreDraw.submit(builder.buildOrThrow(), CRenderPipelines.RECTANGLE_PIPLINE, 32, b -> b
            .putVec4(this.radius.radius1(), this.radius.radius2(), this.radius.radius3(), this.radius.radius4())
            .putVec2(width, height).putFloat(this.smoothness), List.of(), false);
    }

}
