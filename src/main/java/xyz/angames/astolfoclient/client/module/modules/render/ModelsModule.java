package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;

@Environment(value=EnvType.CLIENT)
public class ModelsModule
extends Module {
    public final BooleanSetting onlySelf = new BooleanSetting("OnlySelf", false);
    public final BooleanSetting friends = new BooleanSetting("Friends", false);
    public final BooleanSetting friendHighlight = new BooleanSetting("FriendHighlight", false);
    public final BooleanSetting changeZ = new BooleanSetting("Reversed", true);
    public final ModeSetting mode = new ModeSetting("Mode", "Rabbit", "Rabbit", "Cow");

    public ModelsModule() {
        super("Models", Module.Category.RENDER);
        this.addSetting(this.onlySelf);
        this.addSetting(this.friends);
        this.addSetting(this.friendHighlight);
        this.addSetting(this.changeZ);
        this.addSetting(this.mode);
    }
}

