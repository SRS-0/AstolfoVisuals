package xyz.angames.astolfoclient.client.module.setting;

import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class Setting {
    public final String name;
    private boolean hidden = false;
    private Supplier<Boolean> visibility = null;

    public Setting(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isVisible() {
        if (this.visibility != null && !this.visibility.get().booleanValue()) {
            return false;
        }
        return !this.hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public Setting setVisibility(Supplier<Boolean> visibility) {
        this.visibility = visibility;
        return this;
    }
}

