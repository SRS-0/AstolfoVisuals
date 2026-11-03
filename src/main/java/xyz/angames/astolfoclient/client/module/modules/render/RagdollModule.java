package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(value=EnvType.CLIENT)
public class RagdollModule
extends Module {
    public final BooleanSetting hit = new BooleanSetting("Hit", true);
    public final BooleanSetting totemPop = new BooleanSetting("Totem Pop", true);
    public final BooleanSetting death = new BooleanSetting("Death", true);

    public RagdollModule() {
        super("Ragdoll", "Spawns a custom glowing ragdoll.", Module.Category.RENDER);
        this.addSettings(this.hit, this.totemPop, this.death);
    }
}

