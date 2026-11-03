package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class BooleanSetting
extends Setting {
    private boolean enabled;

    public BooleanSetting(String name, boolean enabled) {
        super(name);
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean get() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void set(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        this.enabled = !this.enabled;
    }
}

