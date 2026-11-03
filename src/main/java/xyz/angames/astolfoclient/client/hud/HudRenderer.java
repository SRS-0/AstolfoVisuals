package xyz.angames.astolfoclient.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class HudRenderer {
    private final LogoRenderer logoRenderer = new LogoRenderer();

    public LogoRenderer getLogoRenderer() {
        return this.logoRenderer;
    }

    public void render(DrawContext context, float tickDelta) {
        InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
        if (interfaceMod != null && interfaceMod.isEnabled() && interfaceMod.logo.get()) {
            this.logoRenderer.render(context);
        }
        MinecraftClient client = MinecraftClient.getInstance();
        Module boyKisserModule = AstolfoclientClient.moduleManager.getModuleByName("BoyKisser");
        if (boyKisserModule != null && boyKisserModule.isEnabled() && client.currentScreen == null) {
            AstolfoclientClient.boyKisserManager.render(context, tickDelta);
        }
    }
}

