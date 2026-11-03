package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class TrajectoriesModule
extends Module {
    public final BooleanSetting showHitbox = new BooleanSetting("Show Landing Box", true);
    public final BooleanSetting drawThroughWalls = new BooleanSetting("Through Walls", true);
    public final BooleanSetting thrownPearls = new BooleanSetting("Track Thrown Pearls", true);
    public final BooleanSetting background = new BooleanSetting("Background", true);
    public final BooleanSetting glow = new BooleanSetting("Theme Glow", true);
    public final NumberSetting bgOpacity = new NumberSetting("BG Opacity", 0.95, 0.1, 1.0, 0.05);
    public final NumberSetting scale = new NumberSetting("Scale", 0.6, 0.1, 3.0, 0.1);

    public TrajectoriesModule() {
        super("Trajectories", Module.Category.RENDER);
        this.addSetting(this.showHitbox);
        this.addSetting(this.drawThroughWalls);
        this.addSetting(this.thrownPearls);
        this.addSetting(this.background);
        this.addSetting(this.glow);
        this.addSetting(this.bgOpacity);
        this.addSetting(this.scale);
    }
}

