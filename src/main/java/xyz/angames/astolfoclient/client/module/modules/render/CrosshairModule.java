package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class CrosshairModule
extends Module {
    public final NumberSetting thickness = new NumberSetting("Thickness", 1.0, 0.5, 5.0, 0.1);
    public final NumberSetting length = new NumberSetting("Length", 3.0, 0.5, 10.0, 0.1);
    public final NumberSetting gap = new NumberSetting("Gap", 2.0, 0.0, 10.0, 0.1);
    public final BooleanSetting dynamicGap = new BooleanSetting("Dynamic Gap", false);
    public final BooleanSetting useEntityColor = new BooleanSetting("Use Entity Color", true);

    public CrosshairModule() {
        super("Crosshair", Module.Category.RENDER);
        this.addSetting(this.thickness);
        this.addSetting(this.length);
        this.addSetting(this.gap);
        this.addSetting(this.dynamicGap);
        this.addSetting(this.useEntityColor);
    }

    public float getThickness() {
        return (float)this.thickness.get();
    }

    public float getLength() {
        return (float)this.length.get();
    }

    public float getGap() {
        return (float)this.gap.get();
    }

    public boolean hasDynamicGap() {
        return this.dynamicGap.get();
    }

    public boolean usesEntityColor() {
        return this.useEntityColor.get();
    }

    public void reset() {
        this.thickness.set(1.0);
        this.length.set(3.0);
        this.gap.set(2.0);
        this.dynamicGap.set(false);
        this.useEntityColor.set(true);
    }
}

