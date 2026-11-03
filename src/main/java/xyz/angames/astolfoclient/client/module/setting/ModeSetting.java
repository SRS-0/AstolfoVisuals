package xyz.angames.astolfoclient.client.module.setting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.setting.Setting;

@Environment(value=EnvType.CLIENT)
public class ModeSetting
extends Setting {
    private final List<String> modes;
    private int index;

    public ModeSetting(String name, String defaultMode, String ... modes) {
        this(name, defaultMode, Arrays.asList(modes));
    }

    public ModeSetting(String name, String defaultMode, List<String> modes) {
        super(name);
        this.modes = modes != null && !modes.isEmpty() ? new ArrayList<String>(modes) : Collections.singletonList(defaultMode);
        this.index = this.modes.indexOf(defaultMode);
        if (this.index == -1) {
            this.index = 0;
        }
    }

    public String get() {
        return this.modes.get(this.index);
    }

    public boolean is(String mode) {
        return this.modes.get(this.index).equalsIgnoreCase(mode);
    }

    public void cycle() {
        this.index = this.index < this.modes.size() - 1 ? ++this.index : 0;
    }

    public void set(String mode) {
        if (mode == null) {
            return;
        }
        for (int i = 0; i < this.modes.size(); ++i) {
            if (!this.modes.get(i).equalsIgnoreCase(mode)) continue;
            this.index = i;
            return;
        }
    }

    public List<String> getModes() {
        return this.modes;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        if (index >= 0 && index < this.modes.size()) {
            this.index = index;
        }
    }

    public synchronized void addMode(String mode) {
        if (mode != null && !mode.isEmpty()) {
            for (String m : this.modes) {
                if (!m.equalsIgnoreCase(mode)) continue;
                return;
            }
            this.modes.add(mode);
        }
    }

    public synchronized void removeMode(String mode) {
        if (this.modes.size() > 1 && mode != null) {
            int removedIdx = -1;
            for (int i = 0; i < this.modes.size(); ++i) {
                if (!this.modes.get(i).equalsIgnoreCase(mode)) continue;
                removedIdx = i;
                break;
            }
            if (removedIdx != -1) {
                this.modes.remove(removedIdx);
                if (this.index == removedIdx || this.index >= this.modes.size()) {
                    this.index = 0;
                }
            }
        }
    }
}

