package dev.sxmurxy.mre.msdf;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.sxmurxy.mre.msdf.FontData;
import dev.sxmurxy.mre.msdf.MsdfGlyph;
import dev.sxmurxy.mre.providers.ResourceProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public final class MsdfFont {
    private final String name;
    private final AbstractTexture texture;
    private final FontData.AtlasData atlas;
    private final FontData.MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;

    private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    public AbstractTexture getTexture() {
        return this.texture;
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color) {
        int prevChar = -1;
        for (int i = 0; i < text.length(); ++i) {
            char _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get((int)_char);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault((int)_char, Float.valueOf(0.0f)).floatValue() * size;
            }
            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = _char;
        }
    }

    public float getWidth(String text, float size) {
        int prevChar = -1;
        float width = 0.0f;
        for (int i = 0; i < text.length(); ++i) {
            char _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get((int)_char);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault((int)_char, Float.valueOf(0.0f)).floatValue() * size;
            }
            width += glyph.getWidth(size);
            prevChar = _char;
        }
        return width;
    }

    public String getName() {
        return this.name;
    }

    public FontData.AtlasData getAtlas() {
        return this.atlas;
    }

    public FontData.MetricsData getMetrics() {
        return this.metrics;
    }

    public float getAutoYOffset() {
        MsdfGlyph referenceGlyph = this.glyphs.get(65);
        if (referenceGlyph == null) {
            referenceGlyph = this.glyphs.get(97);
        }
        if (referenceGlyph != null) {
            return referenceGlyph.getTopPosition();
        }
        return this.metrics.ascender();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private String name = "?";
        private Identifier dataIdentifer;
        private Identifier atlasIdentifier;
        private Function<FontData.GlyphData, Integer> glyphMapper;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder data(String dataFileName) {
            this.dataIdentifer = Identifier.fromNamespaceAndPath((String)"mre", (String)("fonts/" + dataFileName + ".json"));
            return this;
        }

        public Builder data(Identifier dataIdentifier) {
            this.dataIdentifer = dataIdentifier;
            return this;
        }

        public Builder atlas(String atlasFileName) {
            this.atlasIdentifier = Identifier.fromNamespaceAndPath((String)"mre", (String)("fonts/" + atlasFileName + ".png"));
            return this;
        }

        public Builder atlas(Identifier atlasIdentifier) {
            this.atlasIdentifier = atlasIdentifier;
            return this;
        }

        public Builder glyphMapper(Function<FontData.GlyphData, Integer> mapper) {
            this.glyphMapper = mapper;
            return this;
        }

        public MsdfFont build() {
            FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
            AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(this.atlasIdentifier);
            if (data == null) {
                throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString() + "; Are you sure this is json file? Try to check the correctness of its syntax.");
            }
            // Filtering is bound through the texture sampler in the render pass.
            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            HashMap<Integer, MsdfGlyph> glyphs = new HashMap<Integer, MsdfGlyph>();
            if (data.glyphs() != null) {
                for (FontData.GlyphData glyphData : data.glyphs()) {
                    int code = this.glyphMapper != null ? this.glyphMapper.apply(glyphData) : (glyphData.unicode() != 0 ? glyphData.unicode() : glyphData.index());
                    if (code <= 0) continue;
                    glyphs.put(code, new MsdfGlyph(glyphData, code, aWidth, aHeight));
                }
            }
            HashMap<Integer, Map<Integer, Float>> kernings = new HashMap<Integer, Map<Integer, Float>>();
            if (data.kernings() != null) {
                data.kernings().forEach(kerning -> {
                    HashMap<Integer, Float> map = (HashMap<Integer, Float>)kernings.get(kerning.leftChar());
                    if (map == null) {
                        map = new HashMap<Integer, Float>();
                        kernings.put(kerning.leftChar(), map);
                    }
                    map.put(kerning.rightChar(), Float.valueOf(kerning.advance()));
                });
            }
            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings);
        }
    }
}

