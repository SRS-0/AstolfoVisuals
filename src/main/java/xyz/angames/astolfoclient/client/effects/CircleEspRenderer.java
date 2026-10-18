package xyz.angames.astolfoclient.client.effects;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import xyz.angames.astolfoclient.client.compat.WorldRenderContext;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;

@Environment(EnvType.CLIENT)
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
      if (targetEspModule != null && targetEspModule.isEnabled()) {
         if (targetEspModule instanceof TargetEspModule module) {
            if (module.mode.is("Circle")) {
               if (!this.manager.getEffects().isEmpty()) {
                  List<CircleEspManager.CircleEspEffect> validEffects = new ArrayList<>(this.manager.getEffects().values());
                  if (!validEffects.isEmpty()) {
                     if (module.circleMode.is("Jello")) {
                        this.galaxyRenderer.render(context, validEffects);
                     } else {
                        this.ghostRenderer.render(context, validEffects);
                     }
                  }
               }
            }
         }
      }
   }
}
