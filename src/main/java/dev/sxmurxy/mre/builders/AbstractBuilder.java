package dev.sxmurxy.mre.builders;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public abstract class AbstractBuilder<T> {
    public AbstractBuilder() {
        this.reset();
    }

    public final T build() {
        T instance = this._build();
        this.reset();
        return instance;
    }

    protected abstract void reset();

    protected abstract T _build();
}

