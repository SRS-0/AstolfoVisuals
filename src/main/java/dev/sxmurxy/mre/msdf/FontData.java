package dev.sxmurxy.mre.msdf;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class FontData {
    private AtlasData atlas;
    private MetricsData metrics;
    private List<GlyphData> glyphs;
    @SerializedName(value="kerning")
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

    @Environment(value=EnvType.CLIENT)
    public static final class AtlasData {
        @SerializedName(value="distanceRange")
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

    @Environment(value=EnvType.CLIENT)
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

    @Environment(value=EnvType.CLIENT)
    public static final class KerningData {
        @SerializedName(value="unicode1")
        private int leftChar;
        @SerializedName(value="unicode2")
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

    @Environment(value=EnvType.CLIENT)
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

    @Environment(value=EnvType.CLIENT)
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
}

