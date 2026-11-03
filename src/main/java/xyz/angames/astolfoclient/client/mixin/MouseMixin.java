package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Mouse.class})
public class MouseMixin {
    @Inject(method={"onMouseButton"}, at={@At(value="HEAD")})
    private void onMouseButton(long window, net.minecraft.client.input.MouseInput input, int action, CallbackInfo ci) {
        int button=input.button(),mods=input.modifiers();
        MinecraftClient client = MinecraftClient.getInstance();
        if (action == 1 && client.currentScreen == null && AstolfoclientClient.moduleManager != null) {
            int mappedKey = -(button + 100);
            for (Module module : AstolfoclientClient.moduleManager.getModules()) {
                if (module.getKeyCode() != mappedKey) continue;
                module.toggle();
            }
        }
    }
}

