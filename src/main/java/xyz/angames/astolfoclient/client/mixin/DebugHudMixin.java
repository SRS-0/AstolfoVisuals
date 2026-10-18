package xyz.angames.astolfoclient.client.mixin;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.module.modules.misc.CoordsHiderModule;

@Environment(EnvType.CLIENT)
@Mixin(DebugScreenOverlay.class)
public class DebugHudMixin {
   @ModifyVariable(method = "extractLines", at = @At("HEAD"), argsOnly = true)
   private List<String> onExtractLines(List<String> list) {
      if (list != null && !list.isEmpty() && CoordsHiderModule.isF3Hidden()) {
         List<String> modified = new ArrayList<>(list.size());

         for (String line : list) {
            modified.add(CoordsHiderModule.filterF3Line(line));
         }

         return modified;
      }

      return list;
   }
}
