package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(value=EnvType.CLIENT)
public class BabyPlayerModule
extends Module {
    public static BabyPlayerModule INSTANCE;
    public BooleanSetting self = new BooleanSetting("Self", true);
    public BooleanSetting friends = new BooleanSetting("Friends", true);

    public BabyPlayerModule() {
        super("BabyPlayer", Module.Category.RENDER);
        INSTANCE = this;
        this.addSettings(this.self, this.friends);
    }
}

