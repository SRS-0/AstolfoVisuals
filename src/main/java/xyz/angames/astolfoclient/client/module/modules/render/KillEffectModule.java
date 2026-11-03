package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;

@Environment(value=EnvType.CLIENT)
public class KillEffectModule
extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Zap", "Zap", "Thanos");

    public KillEffectModule() {
        super("KillEffect", Module.Category.RENDER);
        this.addSetting(this.mode);
    }

    @Override
    public void onTick() {
        AstolfoclientClient.killEffectManager.tick();
    }
}

