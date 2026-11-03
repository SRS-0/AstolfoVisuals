package xyz.angames.astolfoclient.client.module.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class BoyKisserModule
extends Module {
    public BoyKisserModule() {
        super("BoyKisser", Module.Category.MISC);
    }
}

