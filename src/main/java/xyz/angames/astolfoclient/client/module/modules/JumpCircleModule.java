package xyz.angames.astolfoclient.client.module.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class JumpCircleModule
extends Module {
    public final NumberSetting radius = new NumberSetting("Radius", 3.2, 1.0, 6.0, 0.1);
    public final BooleanSetting distortion = new BooleanSetting("Distortion", true);
    public final NumberSetting distortionStrength = new NumberSetting("Strength", 1.0, 0.2, 2.5, 0.1){

        @Override
        public boolean isVisible() {
            return JumpCircleModule.this.distortion.get();
        }
    };
    public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true){

        @Override
        public boolean isVisible() {
            return JumpCircleModule.this.distortion.get();
        }
    };
    public final BooleanSetting texture = new BooleanSetting("Texture", true);
    public final ModeSetting style = new ModeSetting("Style", "Client", new String[]{"Client", "Large", "Slim"}){

        @Override
        public boolean isVisible() {
            return JumpCircleModule.this.texture.get();
        }
    };

    public JumpCircleModule() {
        super("JumpCircle", Module.Category.RENDER);
        this.addSetting(this.radius);
        this.addSetting(this.distortion);
        this.addSetting(this.distortionStrength);
        this.addSetting(this.chromatic);
        this.addSetting(this.texture);
        this.addSetting(this.style);
    }
}

