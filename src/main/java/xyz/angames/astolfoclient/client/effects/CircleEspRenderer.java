package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.effects.CircleEspGalaxyRenderer;
import xyz.angames.astolfoclient.client.effects.CircleEspGhostRenderer;
import xyz.angames.astolfoclient.client.effects.CircleEspManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;

@Environment(value=EnvType.CLIENT)
public class CircleEspRenderer {
    private final CircleEspManager manager;
    private final CircleEspGalaxyRenderer galaxyRenderer;
    private final CircleEspGhostRenderer ghostRenderer;

    public CircleEspRenderer(CircleEspManager manager) {
        this.manager = manager;
        this.galaxyRenderer = new CircleEspGalaxyRenderer(manager);
        this.ghostRenderer = new CircleEspGhostRenderer();
    }

    public void render(WorldRenderContext context) {
        Module targetEspModule = AstolfoclientClient.moduleManager.getModuleByName("TargetESP");
        if (targetEspModule == null || !targetEspModule.isEnabled()) {
            return;
        }
        if (!(targetEspModule instanceof TargetEspModule)) {
            return;
        }
        TargetEspModule module = (TargetEspModule)targetEspModule;
        if (!module.mode.is("Circle")) {
            return;
        }
        if (this.manager.getEffects().isEmpty()) {
            return;
        }
        ArrayList<CircleEspManager.CircleEspEffect> validEffects = new ArrayList<CircleEspManager.CircleEspEffect>(this.manager.getEffects().values());
        if (validEffects.isEmpty()) {
            return;
        }
        if (module.circleMode.is("Jello")) {
            this.galaxyRenderer.render(context, validEffects);
        } else {
            this.ghostRenderer.render(context, validEffects);
        }
    }
}

