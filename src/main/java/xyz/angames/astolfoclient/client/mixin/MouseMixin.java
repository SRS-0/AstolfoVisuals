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

@Environment(value=EnvType.CLIENT)
@Mixin(value={MouseHandler.class})
public class MouseMixin {
    @Inject(method={"onButton"}, at={@At(value="HEAD")})
    private void onMouseButton(long window, net.minecraft.client.input.MouseButtonInfo input, int action, CallbackInfo ci) {
        int button=xyz.angames.astolfoclient.client.util.LegacyInput.legacyButton(input.button()),mods=input.modifiers();
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle()) {
            xyz.angames.astolfoclient.client.util.HudMouseInput.onButton(client.gui.screen(), client.isWindowActive(), button, action);
        }
        if (action == 1 && client.gui.screen() == null && AstolfoclientClient.moduleManager != null) {
            int mappedKey = -(button + 100);
            for (Module module : AstolfoclientClient.moduleManager.getModules()) {
                if (module.getKeyCode() != mappedKey) continue;
                module.toggle();
            }
        }
    }
}

