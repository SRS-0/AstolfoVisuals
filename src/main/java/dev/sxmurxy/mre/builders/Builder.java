package dev.sxmurxy.mre.builders;

import dev.sxmurxy.mre.builders.impl.BlurBuilder;
import dev.sxmurxy.mre.builders.impl.BorderBuilder;
import dev.sxmurxy.mre.builders.impl.RectangleBuilder;
import dev.sxmurxy.mre.builders.impl.TextureBuilder;
import dev.sxmurxy.mre.builders.impl.LiquidGlassBuilder;
import dev.sxmurxy.mre.builders.impl.TextBuilder;

public final class Builder {

    private static final RectangleBuilder RECTANGLE_BUILDER = new RectangleBuilder();
    private static final BorderBuilder BORDER_BUILDER = new BorderBuilder();
    private static final TextureBuilder TEXTURE_BUILDER = new TextureBuilder();
    private static final TextBuilder TEXT_BUILDER = new TextBuilder();
    private static final BlurBuilder BLUR_BUILDER = new BlurBuilder();
    private static final LiquidGlassBuilder LIQUID_GLASS_BUILDER = new LiquidGlassBuilder();

    public static RectangleBuilder rectangle() {
        return RECTANGLE_BUILDER;
    }

    public static BorderBuilder border() {
        return BORDER_BUILDER;
    }

    public static TextureBuilder texture() {
        return TEXTURE_BUILDER;
    }

    public static TextBuilder text() {
        return TEXT_BUILDER;
    }

    public static BlurBuilder blur() {
        return BLUR_BUILDER;
    }

    public static LiquidGlassBuilder liquidGlass() {
        return LIQUID_GLASS_BUILDER;
    }

}
