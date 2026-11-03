package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class DamageIndicatorModule
extends Module {
    public final NumberSetting scale = new NumberSetting("Scale", 1.0, 0.5, 3.0, 0.1);
    public final NumberSetting bounceHeight = new NumberSetting("Bounce Force", 0.3, 0.1, 1.0, 0.05);

    public DamageIndicatorModule() {
        super("DamageIndicators", Module.Category.RENDER);
        this.addSetting(this.scale);
        this.addSetting(this.bounceHeight);
    }

    @Override
    public void onTick() {
        AstolfoclientClient.damageIndicatorManager.tick();
    }
}

