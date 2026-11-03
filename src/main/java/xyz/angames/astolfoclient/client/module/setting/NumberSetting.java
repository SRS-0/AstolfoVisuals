package xyz.angames.astolfoclient.client.module.setting;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class NumberSetting
extends Setting {
    private double value;
    private final double min;
    private final double max;
    private final double increment;

    public NumberSetting(String name, double value, double min, double max, double increment) {
        super(name);
        this.value = value;
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    public double get() {
        return this.value;
    }

    public float getFloat() {
        return (float)this.value;
    }

    public int getInt() {
        return (int)this.value;
    }

    public void set(double value) {
        double val = Math.max(this.min, Math.min(this.max, value));
        if (this.increment != 0.0) {
            double precision = 1.0 / this.increment;
            val = (double)Math.round(val * precision) / precision;
        }
        this.value = val;
    }

    public double getValue() {
        return this.value;
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double getIncrement() {
        return this.increment;
    }
}

