package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class AspectRatioModule
extends Module {
    public static AspectRatioModule INSTANCE;
    public final ModeSetting mode = new ModeSetting("Mode", "16:9", "16:9", "4:3", "16:10", "Custom");
    public final NumberSetting customRatio = new NumberSetting("Custom Ratio", 1.78, 0.1, 3.0, 0.01){

        @Override
        public boolean isVisible() {
            return AspectRatioModule.this.mode.is("Custom");
        }
    };

    public AspectRatioModule() {
        super("AspectRatio", "Changes the rendering aspect ratio", Module.Category.RENDER);
        INSTANCE = this;
        this.customRatio.setVisibility(() -> this.mode.is("Custom"));
        this.addSettings(this.mode, this.customRatio);
    }

    public static AspectRatioModule getInstance() {
        Module mod;
        if (INSTANCE != null) {
            return INSTANCE;
        }
        Module module = mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("AspectRatio") : null;
        if (mod instanceof AspectRatioModule) {
            AspectRatioModule am;
            INSTANCE = am = (AspectRatioModule)mod;
            return am;
        }
        return null;
    }

    public float getAspectRatio(float defaultAspect) {
        if (!this.isEnabled()) {
            return defaultAspect;
        }
        if (this.mode.is("4:3")) {
            return 1.3333334f;
        }
        if (this.mode.is("16:9")) {
            return 1.7777778f;
        }
        if (this.mode.is("16:10")) {
            return 1.6f;
        }
        if (this.mode.is("Custom")) {
            return this.customRatio.getFloat();
        }
        return defaultAspect;
    }
}

