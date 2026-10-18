package dev.sxmurxy.mre.msdf;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public final class FontData {

	private AtlasData atlas;
	private MetricsData metrics;
	private List<GlyphData> glyphs;
	@SerializedName("kerning")
	private List<KerningData> kernings;

	public AtlasData atlas() {
		return this.atlas;
	}
	
	public MetricsData metrics() {
		return this.metrics;
	}
	
	public List<GlyphData> glyphs() {
		return this.glyphs;
	}
	
	public List<KerningData> kernings() {
		return this.kernings;
	}
	
	public static final class AtlasData {

		@SerializedName("distanceRange")
		private float range;
		private float width;
		private float height;

		public float range() {
			return this.range;
		}

		public float width() {
			return this.width;
		}

		public float height() {
			return this.height;
		}

	}
	
	public static final class MetricsData {

		private float lineHeight;
		private float ascender;
		private float descender;

		public float lineHeight() {
			return this.lineHeight;
		}

		public float ascender() {
			return this.ascender;
		}

		public float descender() {
			return this.descender;
		}

		public float baselineHeight() {
			return this.lineHeight + this.descender;
		}

	}
	
	public static final class GlyphData {

		private int unicode;
		private int index;
		private float advance;
		private BoundsData planeBounds;
		private BoundsData atlasBounds;

		public int unicode() {
			return this.unicode;
		}

		public int index() {
			return this.index;
		}

		public float advance() {
			return this.advance;
		}

		public BoundsData planeBounds() {
			return this.planeBounds;
		}

		public BoundsData atlasBounds() {
			return this.atlasBounds;
		}

	}
	
	public static final class BoundsData {

		private float left;
		private float top;
		private float right;
		private float bottom;

		public float left() {
			return this.left;
		}

		public float top() {
			return this.top;
		}

		public float right() {
			return this.right;
		}

		public float bottom() {
			return this.bottom;
		}

	}
	
	public static final class KerningData {
		
		@SerializedName("unicode1")
		private int leftChar;
		@SerializedName("unicode2")
		private int rightChar;
		private float advance;

		public int leftChar() {
			return this.leftChar;
		}

		public int rightChar() {
			return this.rightChar;
		}

		public float advance() {
			return this.advance;
		}

	}
	
}