package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
   @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
   private void onKey(long window, int action, net.minecraft.client.input.KeyEvent event, CallbackInfo ci) {
      int key = xyz.angames.astolfoclient.client.compat.KeyMap.sdlToGlfw(event.key());
      if (key != -1 && key > 0) {
         Minecraft client = Minecraft.getInstance();
         if (action == 1 && client.gui.screen() == null) {
            if (key == AstolfoclientClient.clickGuiKeyCode || key == 260 || key == 344) {
               client.gui.setScreen(new ClickGuiScreen());
               ci.cancel();
               return;
            }

            if (key == AstolfoclientClient.hudEditorKeyCode) {
               client.gui.setScreen(new HudEditorScreen());
               ci.cancel();
               return;
            }

            if (AstolfoclientClient.moduleManager != null) {
               for (Module module : AstolfoclientClient.moduleManager.getModules()) {
                  if (module.getKeyCode() == key) {
                     module.toggle();
                  }
               }
            }
         }
      }
   }
}
