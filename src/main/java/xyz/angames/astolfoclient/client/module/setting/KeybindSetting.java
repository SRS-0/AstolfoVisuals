package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(value=EnvType.CLIENT)
public class KeybindSetting
extends Setting {
    private int key;

    public KeybindSetting(String name, int defaultKey) {
        super(name);
        this.key = defaultKey;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getKeyName() {
        if (this.key == 0 || this.key == -1) {
            return "None";
        }
        return KeyUtils.getKeyName(this.key);
    }
}

