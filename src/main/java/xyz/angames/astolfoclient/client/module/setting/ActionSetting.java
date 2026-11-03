package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class ActionSetting
extends Setting {
    private final Runnable action;

    public ActionSetting(String name, Runnable action) {
        super(name);
        this.action = action;
    }

    public void run() {
        if (this.action != null) {
            this.action.run();
        }
    }
}

