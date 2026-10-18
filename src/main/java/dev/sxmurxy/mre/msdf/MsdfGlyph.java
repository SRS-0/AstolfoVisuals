package dev.sxmurxy.mre.msdf;

import org.joml.Matrix4f;

import dev.sxmurxy.mre.msdf.FontData.BoundsData;
import dev.sxmurxy.mre.msdf.FontData.GlyphData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import xyz.angames.astolfoclient.client.compat.Vtx;

public final class MsdfGlyph {

	private final int code;
	private final float minU, maxU, minV, maxV;
	private final float advance, topPosition, width, height;
	
	public MsdfGlyph(GlyphData data, float atlasWidth, float atlasHeight) {
		this.code = data.unicode();
		this.advance = data.advance();
		
		BoundsData atlasBounds = data.atlasBounds();
		if (atlasBounds != null) {
			this.minU = atlasBounds.left() / atlasWidth;
			this.maxU = atlasBounds.right() / atlasWidth;
			this.minV = 1.0F - atlasBounds.top() / atlasHeight;
			this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
		} else {
			this.minU = this.maxU = this.minV = this.maxV = 0.0f;
		}

		BoundsData planeBounds = data.planeBounds();
		if (planeBounds != null) {
			this.width = planeBounds.right() - planeBounds.left();
			this.height = planeBounds.top() - planeBounds.bottom();
			this.topPosition = planeBounds.top();
		} else {
			this.width = this.height = this.topPosition = 0.0f;
		}
	}
	
	public float apply(Matrix4f matrix, VertexConsumer consumer, float size, float x, float y, float z, int color) {
		y -= this.topPosition * size;
		float width = this.width * size;
		float height = this.height * size;
		Vtx.vertex(consumer, matrix, x, y, z).setUv(this.minU, this.minV).setColor(color);
		Vtx.vertex(consumer, matrix, x, y + height, z).setUv(this.minU, this.maxV).setColor(color);
		Vtx.vertex(consumer, matrix, x + width, y + height, z).setUv(this.maxU, this.maxV).setColor(color);
		Vtx.vertex(consumer, matrix, x + width, y, z).setUv(this.maxU, this.minV).setColor(color);
		
		return this.advance * size;
	}
	
	public float getWidth(float size) {
		return this.advance * size;
	}

	public int getCharCode() {
		return code;
	}

}