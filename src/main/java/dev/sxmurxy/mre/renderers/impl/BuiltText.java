package dev.sxmurxy.mre.renderers.impl;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.providers.ColorProvider;
import dev.sxmurxy.mre.renderers.IRenderer;
import dev.sxmurxy.mre.utils.CRenderPipelines;
import dev.sxmurxy.mre.utils.MreDraw;
import java.util.List;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.compat.Tesselator;

public record BuiltText(
        MsdfFont font,
        String text,
    	float size,
        float thickness,
        int color,
		float smoothness,
        float spacing,
		int outlineColor,
		float outlineThickness
    ) implements IRenderer {
	
	@Override
    public void render(Matrix4f matrix, float x, float y, float z) {
		if (this.text == null || this.text.isEmpty()) {
			return;
		}

		BufferBuilder builder = Tesselator.getInstance().begin(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		this.font.applyGlyphs(matrix, builder, this.text, this.size,
			(this.thickness + this.outlineThickness * 0.5f) * 0.5f * this.size, this.spacing,
				x, y + this.font.getMetrics().baselineHeight() * this.size, z, this.color);

		com.mojang.blaze3d.vertex.MeshData mesh = builder.build();
		if (mesh == null) {
			return;
		}

		boolean outlineEnabled = (this.outlineThickness > 0.0f);
		float[] oc = ColorProvider.normalize(this.outlineColor);
		MreDraw.submit(mesh, CRenderPipelines.MSDF_FONT_PIPLINE, 48, b -> b
			.putVec4(oc[0], oc[1], oc[2], oc[3])
			.putFloat(this.font.getAtlas().range()).putFloat(this.thickness).putFloat(this.smoothness)
			.putInt(outlineEnabled ? 1 : 0).putFloat(this.outlineThickness),
			List.of(new MreDraw.Texture("Sampler0", this.font.getTextureView(), this.font.getSampler())), false);
	}

}
