package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class LineGlyphsModule
extends Module {
    public final NumberSetting glyphsCount = new NumberSetting("Count", 70.0, 10.0, 200.0, 1.0);
    public final BooleanSetting slowSpeed = new BooleanSetting("Slow Speed", false);
    public final BooleanSetting linesGlowing = new BooleanSetting("Glowing", true);

    public LineGlyphsModule() {
        super("LineGlyphs", Module.Category.RENDER);
        this.addSetting(this.glyphsCount);
        this.addSetting(this.slowSpeed);
        this.addSetting(this.linesGlowing);
    }
}

