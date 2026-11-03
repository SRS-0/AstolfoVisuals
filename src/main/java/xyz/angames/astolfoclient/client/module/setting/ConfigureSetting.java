package xyz.angames.astolfoclient.client.module.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class ConfigureSetting
extends Setting {
    private final String buttonText;
    private final List<Setting> subSettings = new ArrayList<Setting>();

    public ConfigureSetting(String name, String buttonText, Setting ... settings) {
        super(name);
        this.buttonText = buttonText;
        if (settings != null) {
            this.subSettings.addAll(Arrays.asList(settings));
        }
    }

    public ConfigureSetting(String name, String buttonText, List<Setting> settings) {
        super(name);
        this.buttonText = buttonText;
        if (settings != null) {
            this.subSettings.addAll(settings);
        }
    }

    public String getButtonText() {
        return this.buttonText;
    }

    public List<Setting> getSubSettings() {
        return this.subSettings;
    }
}

