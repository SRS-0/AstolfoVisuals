package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Keyboard.class})
public class KeyboardMixin {
    @Inject(method={"onKey"}, at={@At(value="HEAD")}, cancellable=true)
    private void onKey(long window, int action, net.minecraft.client.input.KeyInput input, CallbackInfo ci) {
        int key=input.key(),scancode=input.scancode(),modifiers=input.modifiers();
        if (key == -1 || key <= 0) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (action == 1 && client.currentScreen == null) {
            if (key == AstolfoclientClient.clickGuiKeyCode || key == 260 || key == 344) {
                client.setScreen((Screen)new ClickGuiScreen());
                ci.cancel();
                return;
            }
            if (key == AstolfoclientClient.hudEditorKeyCode) {
                client.setScreen((Screen)new HudEditorScreen());
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

