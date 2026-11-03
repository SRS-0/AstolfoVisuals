package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class ItemPhysicsModule
extends Module {
    public final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.1, 2.0, 0.05);
    public final NumberSetting spinSpeed = new NumberSetting("Spin Speed", 1.0, 0.1, 3.0, 0.1);

    public ItemPhysicsModule() {
        super("ItemPhysics", Module.Category.RENDER);
        this.addSetting(this.scale);
        this.addSetting(this.spinSpeed);
    }
}

