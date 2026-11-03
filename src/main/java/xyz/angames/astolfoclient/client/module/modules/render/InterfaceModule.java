package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(value=EnvType.CLIENT)
public class InterfaceModule
extends Module {
    public final BooleanSetting targetHud = new BooleanSetting("Target HUD", false);
    public final BooleanSetting effectHud = new BooleanSetting("Effect HUD", false);
    public final BooleanSetting inventoryHud = new BooleanSetting("Inventory HUD", false);
    public final BooleanSetting arrayList = new BooleanSetting("Array List", false);
    public final BooleanSetting armorHud = new BooleanSetting("Armor HUD", false);
    public final BooleanSetting musicHud = new BooleanSetting("Music HUD", false);
    public final BooleanSetting infoHud = new BooleanSetting("Info HUD", false);
    public final BooleanSetting logo = new BooleanSetting("WaterMark", false);
    public final BooleanSetting activeBinds = new BooleanSetting("Active Binds", false);

    public InterfaceModule() {
        super("Interface", "Manages the visibility and settings of all HUD interface elements.", Module.Category.RENDER);
        this.addSetting(this.targetHud);
        this.addSetting(this.effectHud);
        this.addSetting(this.inventoryHud);
        this.addSetting(this.arrayList);
        this.addSetting(this.armorHud);
        this.addSetting(this.musicHud);
        this.addSetting(this.infoHud);
        this.addSetting(this.logo);
        this.addSetting(this.activeBinds);
    }
}

