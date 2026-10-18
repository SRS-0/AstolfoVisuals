package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class MouseMixin {
   @Inject(method = "onButton", at = @At("HEAD"))
   private void onMouseButton(long window, net.minecraft.client.input.MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
      int button = xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(buttonInfo.button());
      Minecraft client = Minecraft.getInstance();
      if (action == 1 && client.gui.screen() == null && AstolfoclientClient.moduleManager != null) {
         int mappedKey = -(button + 100);

         for (Module module : AstolfoclientClient.moduleManager.getModules()) {
            if (module.getKeyCode() == mappedKey) {
               module.toggle();
            }
         }
      }
   }
}
