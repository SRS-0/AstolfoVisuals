package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class ItemChamsModule
extends Module {
    public final BooleanSetting syncTheme = new BooleanSetting("Sync Theme Color", true);
    public final NumberSetting alpha = new NumberSetting("Glass Alpha", 0.4, 0.05, 1.0, 0.05);
    public final BooleanSetting glow = new BooleanSetting("Glow", true);
    public final BooleanSetting animated = new BooleanSetting("Animated", true);
    public final NumberSetting fadeSpeed = new NumberSetting("Fade Speed", 1.0, 0.1, 5.0, 0.1);
    public final BooleanSetting fill = new BooleanSetting("Fill", true);
    private static ItemChamsModule INSTANCE;

    public ItemChamsModule() {
        super("ItemChams", "Shader-based item chams.", Module.Category.RENDER);
        this.addSettings(this.syncTheme, this.alpha, this.glow, this.animated, this.fadeSpeed, this.fill);
        INSTANCE = this;
    }

    public static ItemChamsModule getInstance() {
        return INSTANCE;
    }
}

