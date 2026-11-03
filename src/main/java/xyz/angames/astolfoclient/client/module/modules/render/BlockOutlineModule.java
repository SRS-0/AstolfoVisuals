package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class BlockOutlineModule
extends Module {
    public static BlockOutlineModule INSTANCE;
    public final ModeSetting mode = new ModeSetting("Shader Mode", "Pulse Wave", "Pulse Wave", "Cosmos", "Neon Glow", "Rainbow Wave", "Cyber Grid", "Clean");
    public final BooleanSetting shaderFill = new BooleanSetting("Shader Fill", true);
    public final NumberSetting glowIntensity = new NumberSetting("Glow Intensity", 1.5, 0.2, 4.0, 0.1){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get();
        }
    };
    public final NumberSetting pulseSpeed = new NumberSetting("Pulse Speed", 1.5, 0.2, 5.0, 0.1){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get() && !BlockOutlineModule.this.mode.is("Clean");
        }
    };
    public final NumberSetting pulseWidth = new NumberSetting("Pulse Width", 1.2, 0.2, 3.0, 0.1){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get() && BlockOutlineModule.this.mode.is("Pulse Wave");
        }
    };
    public final BooleanSetting distortion = new BooleanSetting("Distortion Wave", true){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get();
        }
    };
    public final BooleanSetting chromatic = new BooleanSetting("Chromatic", true){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get() && BlockOutlineModule.this.distortion.get();
        }
    };
    public final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 0.35, 0.0, 1.0, 0.05){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.shaderFill.get();
        }
    };
    public final BooleanSetting outline = new BooleanSetting("Outline Lines", true);
    public final NumberSetting outlineAlpha = new NumberSetting("Outline Alpha", 1.0, 0.0, 1.0, 0.05){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.outline.get();
        }
    };
    public final NumberSetting lineWidth = new NumberSetting("Line Width", 2.5, 0.5, 6.0, 0.5){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.outline.get();
        }
    };
    public final BooleanSetting fadeEffect = new BooleanSetting("Fade Animation", true);
    public final NumberSetting fadeSpeed = new NumberSetting("Fade Speed", 10.0, 1.0, 25.0, 0.5){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.fadeEffect.get();
        }
    };
    public final BooleanSetting smoothAnim = new BooleanSetting("Smooth Morph", true);
    public final NumberSetting animSpeed = new NumberSetting("Morph Speed", 16.0, 1.0, 30.0, 1.0){

        @Override
        public boolean isVisible() {
            return BlockOutlineModule.this.smoothAnim.get();
        }
    };
    public final BooleanSetting onlyVisible = new BooleanSetting("Only Visible", true);

    public BlockOutlineModule() {
        super("BlockOutline", "Highlights targeted blocks with animated shaders and smooth transitions", Module.Category.RENDER);
        INSTANCE = this;
        this.addSettings(this.mode, this.shaderFill, this.glowIntensity, this.pulseSpeed, this.pulseWidth, this.distortion, this.chromatic, this.fillAlpha, this.outline, this.outlineAlpha, this.lineWidth, this.fadeEffect, this.fadeSpeed, this.smoothAnim, this.animSpeed, this.onlyVisible);
    }
}

