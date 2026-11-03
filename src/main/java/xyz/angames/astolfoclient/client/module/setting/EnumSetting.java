package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class EnumSetting<T extends Enum<T>>
extends Setting {
    private T value;
    private final T[] values;

    public EnumSetting(String name, T value) {
        super(name);
        this.value = value;
        this.values = value.getDeclaringClass().getEnumConstants();
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setByName(String name) {
        for (T e : this.values) {
            if (!((Enum)e).name().equalsIgnoreCase(name)) continue;
            this.value = e;
            return;
        }
    }

    public T[] getValues() {
        return this.values;
    }

    public void cycle() {
        int index = ((Enum)this.value).ordinal();
        int nextIndex = (index + 1) % this.values.length;
        this.value = this.values[nextIndex];
    }

    public String get() {
        return "";
    }
}

