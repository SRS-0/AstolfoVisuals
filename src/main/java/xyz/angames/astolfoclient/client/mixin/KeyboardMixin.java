package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={KeyboardHandler.class})
public class KeyboardMixin {
    @Inject(method={"keyPress"}, at={@At(value="HEAD")}, cancellable=true)
    private void onKey(long window, int action, net.minecraft.client.input.KeyEvent input, CallbackInfo ci) {
        int key=xyz.angames.astolfoclient.client.util.LegacyInput.legacyKey(input.key()),scancode=input.keycode(),modifiers=input.modifiers();
        if (key == -1 || key <= 0) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (action == 1 && client.gui.screen() == null) {
            if (key == AstolfoclientClient.clickGuiKeyCode || key == 260 || key == 344) {
                client.gui.setScreen((Screen)new ClickGuiScreen());
                ci.cancel();
                return;
            }
            if (key == AstolfoclientClient.hudEditorKeyCode) {
                client.gui.setScreen((Screen)new HudEditorScreen());
                ci.cancel();
                return;
            }
            if (AstolfoclientClient.moduleManager != null) {
                for (Module module : AstolfoclientClient.moduleManager.getModules()) {
                    if (module.getKeyCode() != key) continue;
                    module.toggle();
                }
            }
        }
    }
}

