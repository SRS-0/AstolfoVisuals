package xyz.angames.astolfoclient.client.module.modules.misc;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(value=EnvType.CLIENT)
public class CoordsHiderModule
extends Module {
    public static CoordsHiderModule INSTANCE;
    public final BooleanSetting f3 = new BooleanSetting("F3 Debug", true);
    public final BooleanSetting infoHud = new BooleanSetting("Info HUD", true);

    public CoordsHiderModule() {
        super("CoordsHider", "Hides your coordinates in F3 and Info HUD", Module.Category.MISC);
        INSTANCE = this;
        this.addSettings(this.f3, this.infoHud);
    }

    public String getMask() {
        return "***";
    }

    public static String getMaskString() {
        return "***";
    }

    public static boolean isF3Hidden() {
        Module mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("CoordsHider") : null;
        return mod != null && mod.isEnabled() && ((CoordsHiderModule)mod).f3.get();
    }

    public static boolean isInfoHudHidden() {
        Module mod = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("CoordsHider") : null;
        return mod != null && mod.isEnabled() && ((CoordsHiderModule)mod).infoHud.get();
    }

    public static String filterF3Line(String line) {
        if (line == null) {
            return null;
        }
        if (!CoordsHiderModule.isF3Hidden()) {
            return line;
        }
        String mask = CoordsHiderModule.getMaskString();
        if (line.startsWith("XYZ:")) {
            return "XYZ: " + mask + " / " + mask + " / " + mask;
        }
        if (line.startsWith("Block:")) {
            return "Block: " + mask + " " + mask + " " + mask;
        }
        if (line.startsWith("Chunk:")) {
            return "Chunk: " + mask + " " + mask + " " + mask;
        }
        if (line.startsWith("Targeted Block:")) {
            return "Targeted Block: " + mask + ", " + mask + ", " + mask;
        }
        if (line.startsWith("Targeted Fluid:")) {
            return "Targeted Fluid: " + mask + ", " + mask + ", " + mask;
        }
        return line;
    }
}

