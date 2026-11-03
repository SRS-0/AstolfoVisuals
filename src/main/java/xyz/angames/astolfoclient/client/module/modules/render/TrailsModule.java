package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class TrailsModule
extends Module {
    public TrailsModule() {
        super("Trails", Module.Category.RENDER);
    }
}

