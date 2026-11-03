package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class HandPositionModule
extends Module {
    public final NumberSetting mainPosX = new NumberSetting("Main Pos X", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting mainPosY = new NumberSetting("Main Pos Y", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting mainPosZ = new NumberSetting("Main Pos Z", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting mainRotX = new NumberSetting("Main Rot X", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting mainRotY = new NumberSetting("Main Rot Y", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting mainRotZ = new NumberSetting("Main Rot Z", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting mainScaleX = new NumberSetting("Main Scale X", 1.0, 0.1, 3.0, 0.05);
    public final NumberSetting mainScaleY = new NumberSetting("Main Scale Y", 1.0, 0.1, 3.0, 0.05);
    public final NumberSetting mainScaleZ = new NumberSetting("Main Scale Z", 1.0, 0.1, 3.0, 0.05);
    public final NumberSetting offPosX = new NumberSetting("Off Pos X", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting offPosY = new NumberSetting("Off Pos Y", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting offPosZ = new NumberSetting("Off Pos Z", 0.0, -3.0, 3.0, 0.05);
    public final NumberSetting offRotX = new NumberSetting("Off Rot X", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting offRotY = new NumberSetting("Off Rot Y", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting offRotZ = new NumberSetting("Off Rot Z", 0.0, -180.0, 180.0, 1.0);
    public final NumberSetting offScaleX = new NumberSetting("Off Scale X", 1.0, 0.1, 3.0, 0.05);
    public final NumberSetting offScaleY = new NumberSetting("Off Scale Y", 1.0, 0.1, 3.0, 0.05);
    public final NumberSetting offScaleZ = new NumberSetting("Off Scale Z", 1.0, 0.1, 3.0, 0.05);

    public HandPositionModule() {
        super("HandPosition", "Changes the position, rotation, and scale of held items.", Module.Category.RENDER);
        this.addSettings(this.mainPosX, this.mainPosY, this.mainPosZ, this.mainRotX, this.mainRotY, this.mainRotZ, this.mainScaleX, this.mainScaleY, this.mainScaleZ, this.offPosX, this.offPosY, this.offPosZ, this.offRotX, this.offRotY, this.offRotZ, this.offScaleX, this.offScaleY, this.offScaleZ);
    }

    private float getFloat(NumberSetting setting) {
        return ((Number)setting.get()).floatValue();
    }

    public float[] getMainHandPos() {
        return new float[]{this.getFloat(this.mainPosX), this.getFloat(this.mainPosY), this.getFloat(this.mainPosZ)};
    }

    public float[] getMainHandRot() {
        return new float[]{this.getFloat(this.mainRotX), this.getFloat(this.mainRotY), this.getFloat(this.mainRotZ)};
    }

    public float[] getMainHandScale() {
        return new float[]{this.getFloat(this.mainScaleX), this.getFloat(this.mainScaleY), this.getFloat(this.mainScaleZ)};
    }

    public float[] getOffHandPos() {
        return new float[]{this.getFloat(this.offPosX), this.getFloat(this.offPosY), this.getFloat(this.offPosZ)};
    }

    public float[] getOffHandRot() {
        return new float[]{this.getFloat(this.offRotX), this.getFloat(this.offRotY), this.getFloat(this.offRotZ)};
    }

    public float[] getOffHandScale() {
        return new float[]{this.getFloat(this.offScaleX), this.getFloat(this.offScaleY), this.getFloat(this.offScaleZ)};
    }
}

